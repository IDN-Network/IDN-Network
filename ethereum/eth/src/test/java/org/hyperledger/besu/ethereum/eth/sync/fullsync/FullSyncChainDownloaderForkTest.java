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
import org.idnecology.idn.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason;
import org.idnecology.idn.metrics.SyncDurationMetrics;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FullSyncChainDownloaderForkTest {

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

  @BeforeEach
  public void setupTest() throws IOException {
    localBlockchainSetup = BlockchainSetupUtil.forUpgradedFork();
    localBlockchain = localBlockchainSetup.getBlockchain();
    otherBlockchainSetup = BlockchainSetupUtil.forOutdatedFork();
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
    ethProtocolManager.stop();
  }

  private ChainDownloader downloader(final SynchronizerConfiguration syncConfig) {
    return FullSyncChainDownloader.create(
        syncConfig,
        protocolSchedule,
        protocolContext,
        ethContext,
        syncState,
        metricsSystem,
        SyncTerminationCondition.never(),
        SyncDurationMetrics.NO_OP_SYNC_DURATION_METRICS,
        mock(PeerTaskExecutor.class));
  }

  private ChainDownloader downloader() {
    final SynchronizerConfiguration syncConfig = syncConfigBuilder().build();
    return downloader(syncConfig);
  }

  private SynchronizerConfiguration.Builder syncConfigBuilder() {
    return SynchronizerConfiguration.builder();
  }

  @Test
  public void disconnectsFromPeerOnBadFork() {
    otherBlockchainSetup.importAllBlocks();
    final Difficulty localTd = localBlockchain.getChainHead().getTotalDifficulty();

    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(otherBlockchain);
    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.add(100), 100);

    final ChainDownloader downloader = downloader();
    downloader.start();

    // Process until the sync target is selected
    peer.respondWhileOtherThreadsWork(responder, () -> syncState.syncTarget().isEmpty());

    // Check that we picked our peer
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peer.getEthPeer());

    // Process until the sync target is cleared
    peer.respondWhileOtherThreadsWork(responder, () -> syncState.syncTarget().isPresent());

    // We should have disconnected from our peer on the invalid chain
    assertThat(peer.getEthPeer().isDisconnected()).isTrue();
    assertThat(peer.getPeerConnection().getDisconnectReason())
        .contains(DisconnectReason.BREACH_OF_PROTOCOL_INVALID_BLOCK);
    assertThat(syncState.syncTarget()).isEmpty();
  }
}
