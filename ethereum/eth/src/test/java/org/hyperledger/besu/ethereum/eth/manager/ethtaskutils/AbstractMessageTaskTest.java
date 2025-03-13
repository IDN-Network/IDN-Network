/*
 * Copyright ConsenSys AG.
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
package org.idnecology.idn.ethereum.eth.manager.ethtaskutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SECPPrivateKey;
import org.idnecology.idn.crypto.SECPPublicKey;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockchainSetupUtil;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthMessages;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.RespondingEthPeer;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.manager.task.EthTask;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolFactory;
import org.idnecology.idn.ethereum.forkid.ForkIdManager;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.testutil.DeterministicEthScheduler;
import org.idnecology.idn.testutil.TestClock;

import java.time.ZoneId;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @param <T> The type of data being requested from the network
 * @param <R> The type of data returned from the network
 */
public abstract class AbstractMessageTaskTest<T, R> {
  protected static final int MAX_PEERS = 5;
  protected static final KeyPair genesisAccountKeyPair =
      new KeyPair(
          SECPPrivateKey.create(
              Bytes32.fromHexString(
                  "0x45a915e4d060149eb4365960e6a7a45f334393093061116b197e3240065ff2d8"),
              "ECDSA"),
          SECPPublicKey.create(
              Bytes.fromHexString(
                  "0x3a514176466fa815ed481ffad09110a2d344f6c9b78c1d14afc351c3a51be33d8072e77939dc03ba44790779b7a1025baf3003f6732430e20cd9b76d953391b3"),
              "ECDSA"));
  protected static final Address genesisAccountSender =
      Address.extract(Hash.hash(genesisAccountKeyPair.getPublicKey().getEncodedBytes()));
  protected static final long genesisAccountNonce = 32;
  protected static Blockchain blockchain;
  protected static ProtocolSchedule protocolSchedule;
  protected static ProtocolContext protocolContext;
  protected static MetricsSystem metricsSystem = new NoOpMetricsSystem();
  protected EthProtocolManager ethProtocolManager;
  protected EthContext ethContext;
  protected EthPeers ethPeers;
  protected TransactionPool transactionPool;
  protected PeerTaskExecutor peerTaskExecutor;
  protected AtomicBoolean peersDoTimeout;
  protected AtomicInteger peerCountToTimeout;

  @BeforeAll
  public static void setup() {
    final BlockchainSetupUtil blockchainSetupUtil =
        BlockchainSetupUtil.forTesting(DataStorageFormat.FOREST);
    blockchainSetupUtil.importAllBlocks();
    blockchain = blockchainSetupUtil.getBlockchain();
    protocolSchedule = blockchainSetupUtil.getProtocolSchedule();
    protocolContext = blockchainSetupUtil.getProtocolContext();
    assertThat(blockchainSetupUtil.getMaxBlockNumber()).isGreaterThanOrEqualTo(20L);
  }

  @BeforeEach
  public void setupTest() {
    protocolContext.getBadBlockManager().reset();
    peersDoTimeout = new AtomicBoolean(false);
    peerCountToTimeout = new AtomicInteger(0);
    ethPeers =
        spy(
            new EthPeers(
                () -> protocolSchedule.getByBlockHeader(blockchain.getChainHeadHeader()),
                TestClock.fixed(),
                metricsSystem,
                EthProtocolConfiguration.DEFAULT_MAX_MESSAGE_SIZE,
                Collections.emptyList(),
                Bytes.random(64),
                MAX_PEERS,
                MAX_PEERS,
                false,
                SyncMode.FAST,
                new ForkIdManager(
                    blockchain, Collections.emptyList(), Collections.emptyList(), false)));

    final EthMessages ethMessages = new EthMessages();
    final EthScheduler ethScheduler =
        new DeterministicEthScheduler(
            () -> peerCountToTimeout.getAndDecrement() > 0 || peersDoTimeout.get());
    peerTaskExecutor = Mockito.mock(PeerTaskExecutor.class);
    ethContext = new EthContext(ethPeers, ethMessages, ethScheduler, peerTaskExecutor);
    final SyncState syncState = new SyncState(blockchain, ethContext.getEthPeers());
    transactionPool =
        TransactionPoolFactory.createTransactionPool(
            protocolSchedule,
            protocolContext,
            ethContext,
            TestClock.system(ZoneId.systemDefault()),
            metricsSystem,
            syncState,
            TransactionPoolConfiguration.DEFAULT,
            new BlobCache(),
            MiningConfiguration.newDefault(),
            false);
    transactionPool.setEnabled();

    ethProtocolManager =
        EthProtocolManagerTestBuilder.builder()
            .setProtocolSchedule(protocolSchedule)
            .setBlockchain(blockchain)
            .setEthScheduler(ethScheduler)
            .setTransactionPool(transactionPool)
            .setEthereumWireProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .setEthPeers(ethPeers)
            .setEthMessages(ethMessages)
            .setEthContext(ethContext)
            .build();
  }

  protected abstract T generateDataToBeRequested();

  protected abstract EthTask<R> createTask(T requestedData);

  protected abstract void assertResultMatchesExpectation(
      T requestedData, R response, EthPeer respondingPeer);

  @Test
  public void completesWhenPeersAreResponsive() {
    // Setup a responsive peer
    final RespondingEthPeer.Responder responder = getFullResponder();
    final RespondingEthPeer respondingPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 32);

    // Setup data to be requested and expected response
    final T requestedData = generateDataToBeRequested();

    // Execute task and wait for response
    final AtomicReference<R> actualResult = new AtomicReference<>();
    final AtomicBoolean done = new AtomicBoolean(false);
    final EthTask<R> task = createTask(requestedData);
    final CompletableFuture<R> future = task.run();
    respondingPeer.respondWhile(responder, () -> !future.isDone());
    future.whenComplete(
        (result, error) -> {
          actualResult.set(result);
          done.compareAndSet(false, true);
        });

    assertThat(done).isTrue();
    assertResultMatchesExpectation(requestedData, actualResult.get(), respondingPeer.getEthPeer());
    assertNoBadBlocks();
  }

  @Test
  public void doesNotCompleteWhenPeersDoNotRespond() {
    // Setup a unresponsive peer
    EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 32);

    // Setup data to be requested
    final T requestedData = generateDataToBeRequested();

    // Execute task and wait for response
    final AtomicBoolean done = new AtomicBoolean(false);
    final EthTask<R> task = createTask(requestedData);
    final CompletableFuture<R> future = task.run();
    future.whenComplete(
        (response, error) -> {
          done.compareAndSet(false, true);
        });
    assertThat(done).isFalse();
  }

  @Test
  public void cancel() {
    // Setup a unresponsive peer
    EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 32);

    // Setup data to be requested
    final T requestedData = generateDataToBeRequested();

    // Execute task
    final EthTask<R> task = createTask(requestedData);
    final CompletableFuture<R> future = task.run();

    assertThat(future.isDone()).isFalse();
    task.cancel();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(task.run().isCancelled()).isTrue();
  }

  protected RespondingEthPeer.Responder getFullResponder() {
    return RespondingEthPeer.blockchainResponder(
        blockchain, protocolContext.getWorldStateArchive(), transactionPool);
  }

  protected void assertNoBadBlocks() {
    BadBlockManager badBlockManager = protocolContext.getBadBlockManager();
    assertThat(badBlockManager.getBadBlocks().size()).isEqualTo(0);
    assertThat(badBlockManager.getBadHeaders().size()).isEqualTo(0);
  }
}
