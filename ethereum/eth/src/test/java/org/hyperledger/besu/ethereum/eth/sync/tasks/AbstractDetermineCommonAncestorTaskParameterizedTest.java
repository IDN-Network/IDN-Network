/*
 * Copyright contributors to Idn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.idnecology.idn.ethereum.eth.sync.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ProtocolScheduleFixture;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.idnecology.idn.ethereum.eth.manager.RespondingEthPeer;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResponseCode;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResult;
import org.idnecology.idn.ethereum.eth.manager.peertask.task.GetHeadersFromPeerTask;
import org.idnecology.idn.ethereum.eth.manager.task.EthTask;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public abstract class AbstractDetermineCommonAncestorTaskParameterizedTest {
  private final ProtocolSchedule protocolSchedule = ProtocolScheduleFixture.MAINNET;
  private static final BlockDataGenerator blockDataGenerator = new BlockDataGenerator();
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  private static Block genesisBlock;
  private static MutableBlockchain localBlockchain;
  protected static final int chainHeight = 50;

  private MutableBlockchain remoteBlockchain;
  private PeerTaskExecutor peerTaskExecutor;

  @BeforeAll
  public static void setupClass() {
    genesisBlock = blockDataGenerator.genesisBlock();
    localBlockchain = createInMemoryBlockchain(genesisBlock);

    // Setup local chain
    for (int i = 1; i <= chainHeight; i++) {
      final BlockDataGenerator.BlockOptions options =
          new BlockDataGenerator.BlockOptions()
              .setBlockNumber(i)
              .setParentHash(localBlockchain.getBlockHashByNumber(i - 1).get());
      final Block block = blockDataGenerator.block(options);
      final List<TransactionReceipt> receipts = blockDataGenerator.receipts(block);
      localBlockchain.appendBlock(block, receipts);
    }
  }

  @BeforeEach
  public void setup() {
    remoteBlockchain = createInMemoryBlockchain(genesisBlock);
    peerTaskExecutor = mock(PeerTaskExecutor.class);
  }

  @ParameterizedTest(name = "requestSize={0}, commonAncestor={1}, isPeerTaskSystemEnabled={2}")
  @MethodSource("parameters")
  public void searchesAgainstNetwork(
      final int headerRequestSize,
      final int commonAncestorHeight,
      final boolean isPeerTaskSystemEnabled) {
    BlockHeader commonHeader = genesisBlock.getHeader();
    for (long i = 1; i <= commonAncestorHeight; i++) {
      commonHeader = localBlockchain.getBlockHeader(i).get();
      final List<TransactionReceipt> receipts =
          localBlockchain.getTxReceipts(commonHeader.getHash()).get();
      final BlockBody commonBody = localBlockchain.getBlockBody(commonHeader.getHash()).get();
      remoteBlockchain.appendBlock(new Block(commonHeader, commonBody), receipts);
    }

    // Remaining blocks are disparate...
    for (long i = commonAncestorHeight + 1L; i <= chainHeight; i++) {
      final BlockDataGenerator.BlockOptions localOptions =
          new BlockDataGenerator.BlockOptions()
              .setBlockNumber(i)
              .setParentHash(localBlockchain.getBlockHashByNumber(i - 1).get());
      final Block localBlock = blockDataGenerator.block(localOptions);
      final List<TransactionReceipt> localReceipts = blockDataGenerator.receipts(localBlock);
      localBlockchain.appendBlock(localBlock, localReceipts);

      final BlockDataGenerator.BlockOptions remoteOptions =
          new BlockDataGenerator.BlockOptions()
              .setDifficulty(Difficulty.ONE) // differentiator
              .setBlockNumber(i)
              .setParentHash(remoteBlockchain.getBlockHashByNumber(i - 1).get());
      final Block remoteBlock = blockDataGenerator.block(remoteOptions);
      final List<TransactionReceipt> remoteReceipts = blockDataGenerator.receipts(remoteBlock);
      remoteBlockchain.appendBlock(remoteBlock, remoteReceipts);
    }

    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();
    final EthProtocolManager ethProtocolManager =
        EthProtocolManagerTestBuilder.builder()
            .setProtocolSchedule(protocolSchedule)
            .setBlockchain(localBlockchain)
            .setWorldStateArchive(worldStateArchive)
            .setTransactionPool(mock(TransactionPool.class))
            .setEthereumWireProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .setPeerTaskExecutor(peerTaskExecutor)
            .build();
    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(remoteBlockchain);
    final RespondingEthPeer respondingEthPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager);

    // Execute task and wait for response
    final AtomicReference<BlockHeader> actualResult = new AtomicReference<>();
    final AtomicBoolean done = new AtomicBoolean(false);

    final EthContext ethContext = ethProtocolManager.ethContext();
    final ProtocolContext protocolContext =
        new ProtocolContext(
            localBlockchain,
            worldStateArchive,
            mock(ConsensusContext.class),
            new BadBlockManager());

    final EthTask<BlockHeader> task =
        DetermineCommonAncestorTask.create(
            protocolSchedule,
            protocolContext,
            ethContext,
            respondingEthPeer.getEthPeer(),
            headerRequestSize,
            SynchronizerConfiguration.builder()
                .isPeerTaskSystemEnabled(isPeerTaskSystemEnabled)
                .build(),
            metricsSystem);

    when(peerTaskExecutor.executeAgainstPeer(
            Mockito.any(GetHeadersFromPeerTask.class), Mockito.eq(respondingEthPeer.getEthPeer())))
        .thenAnswer(
            (invocationOnMock) -> {
              GetHeadersFromPeerTask getHeadersTask =
                  invocationOnMock.getArgument(0, GetHeadersFromPeerTask.class);
              long blockNumber = getHeadersTask.getBlockNumber();
              int maxHeaders = getHeadersTask.getMaxHeaders();
              int skip = getHeadersTask.getSkip();

              List<BlockHeader> headers = new ArrayList<>();
              long lowerBound = Math.max(0, blockNumber - (maxHeaders - 1) * (skip + 1));
              for (long i = blockNumber; i > lowerBound; i -= skip + 1) {
                headers.add(remoteBlockchain.getBlockHeader(i).get());
              }

              return new PeerTaskExecutorResult<>(
                  Optional.of(headers),
                  PeerTaskExecutorResponseCode.SUCCESS,
                  Optional.of(respondingEthPeer.getEthPeer()));
            });

    final CompletableFuture<BlockHeader> future = task.run();
    respondingEthPeer.respondWhile(responder, () -> !future.isDone());

    future.whenComplete(
        (response, error) -> {
          actualResult.set(response);
          done.compareAndSet(false, true);
        });

    assertThat(actualResult.get()).isNotNull();
    assertThat(actualResult.get().getHash())
        .isEqualTo(MainnetBlockHeaderFunctions.createHash(commonHeader));
  }

  @Test
  void dryRunDetector() {
    assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
  }
}
