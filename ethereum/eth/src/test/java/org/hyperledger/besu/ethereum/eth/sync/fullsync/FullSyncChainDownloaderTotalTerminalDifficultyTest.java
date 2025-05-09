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
package org.idnecology.idn.ethereum.eth.sync.fullsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlockchainSetupUtil;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.RespondingEthPeer;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.sync.ChainDownloader;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.SyncDurationMetrics;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class FullSyncChainDownloaderTotalTerminalDifficultyTest {

  protected ProtocolSchedule protocolSchedule;
  protected EthProtocolManager ethProtocolManager;
  protected EthContext ethContext;
  protected ProtocolContext protocolContext;
  private SyncState syncState;

  private BlockchainSetupUtil localBlockchainSetup;
  protected MutableBlockchain localBlockchain;
  private BlockchainSetupUtil otherBlockchainSetup;
  protected Blockchain otherBlockchain;
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();
  private static final Difficulty TARGET_TERMINAL_DIFFICULTY = Difficulty.of(1_000_000L);

  static class FullSyncChainDownloaderTotalTerminalDifficultyTestArguments
      implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      return Stream.of(
          Arguments.of(DataStorageFormat.BONSAI), Arguments.of(DataStorageFormat.FOREST));
    }
  }

  public void setupTest(final DataStorageFormat storageFormat) {
    localBlockchainSetup = BlockchainSetupUtil.forTesting(storageFormat);
    localBlockchain = localBlockchainSetup.getBlockchain();
    otherBlockchainSetup = BlockchainSetupUtil.forTesting(storageFormat);
    otherBlockchain = otherBlockchainSetup.getBlockchain();

    protocolSchedule = localBlockchainSetup.getProtocolSchedule();
    protocolContext = localBlockchainSetup.getProtocolContext();
    ethProtocolManager =
        EthProtocolManagerTestBuilder.builder()
            .setProtocolSchedule(protocolSchedule)
            .setBlockchain(localBlockchain)
            .setEthScheduler(new EthScheduler(1, 1, 1, 1, new NoOpMetricsSystem()))
            .setWorldStateArchive(localBlockchainSetup.getWorldArchive())
            .setTransactionPool(localBlockchainSetup.getTransactionPool())
            .setEthereumWireProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .build();
    ethContext = ethProtocolManager.ethContext();
    syncState = new SyncState(protocolContext.getBlockchain(), ethContext.getEthPeers());
  }

  @AfterEach
  public void tearDown() {
    if (ethProtocolManager != null) {
      ethProtocolManager.stop();
    }
  }

  private ChainDownloader downloader(
      final SynchronizerConfiguration syncConfig,
      final SyncTerminationCondition terminalCondition) {
    return FullSyncChainDownloader.create(
        syncConfig,
        protocolSchedule,
        protocolContext,
        ethContext,
        syncState,
        metricsSystem,
        terminalCondition,
        SyncDurationMetrics.NO_OP_SYNC_DURATION_METRICS,
        mock(PeerTaskExecutor.class));
  }

  private SynchronizerConfiguration.Builder syncConfigBuilder() {
    return SynchronizerConfiguration.builder();
  }

  @ParameterizedTest
  @ArgumentsSource(FullSyncChainDownloaderTotalTerminalDifficultyTestArguments.class)
  public void syncsFullyAndStopsWhenTTDReached(final DataStorageFormat storageFormat) {
    setupTest(storageFormat);
    otherBlockchainSetup.importFirstBlocks(30);
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(1).downloaderParallelism(1).build();
    final ChainDownloader downloader =
        downloader(
            syncConfig,
            SyncTerminationCondition.difficulty(TARGET_TERMINAL_DIFFICULTY, localBlockchain));
    final CompletableFuture<Void> future = downloader.start();

    assertThat(future.isDone()).isFalse();

    peer.respondWhileOtherThreadsWork(responder, () -> syncState.syncTarget().isEmpty());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peer.getEthPeer());

    peer.respondWhileOtherThreadsWork(responder, () -> !future.isDone());

    assertThat(localBlockchain.getChainHead().getTotalDifficulty())
        .isGreaterThan(TARGET_TERMINAL_DIFFICULTY);

    assertThat(future.isDone()).isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(FullSyncChainDownloaderTotalTerminalDifficultyTestArguments.class)
  public void syncsFullyAndContinuesWhenTTDNotSpecified(final DataStorageFormat storageFormat) {
    setupTest(storageFormat);
    otherBlockchainSetup.importFirstBlocks(30);
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(1).downloaderParallelism(1).build();
    final ChainDownloader downloader = downloader(syncConfig, SyncTerminationCondition.never());
    final CompletableFuture<Void> future = downloader.start();

    assertThat(future.isDone()).isFalse();

    peer.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peer.getEthPeer());

    peer.respondWhileOtherThreadsWork(
        responder, () -> localBlockchain.getChainHeadBlockNumber() < targetBlock);

    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(targetBlock);

    assertThat(future.isDone()).isFalse();
  }

  @Test
  void dryRunDetector() {
    assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
  }
}
