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
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockchainSetupUtil;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ProtocolScheduleFixture;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.RespondingEthPeer;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.state.SyncTarget;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
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

public class FullSyncTargetManagerTest {

  private EthProtocolManager ethProtocolManager;

  private MutableBlockchain localBlockchain;
  private final WorldStateArchive localWorldState = mock(WorldStateArchive.class);
  private RespondingEthPeer.Responder responder;
  private FullSyncTargetManager syncTargetManager;

  static class FullSyncTargetManagerTestArguments implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      return Stream.of(
          Arguments.of(DataStorageFormat.BONSAI), Arguments.of(DataStorageFormat.FOREST));
    }
  }

  public void setup(final DataStorageFormat storageFormat) {
    final BlockchainSetupUtil otherBlockchainSetup = BlockchainSetupUtil.forTesting(storageFormat);
    final Blockchain otherBlockchain = otherBlockchainSetup.getBlockchain();
    responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final BlockchainSetupUtil localBlockchainSetup = BlockchainSetupUtil.forTesting(storageFormat);
    localBlockchain = localBlockchainSetup.getBlockchain();

    final ProtocolSchedule protocolSchedule = ProtocolScheduleFixture.MAINNET;
    final ProtocolContext protocolContext =
        new ProtocolContext(
            localBlockchain, localWorldState, mock(ConsensusContext.class), new BadBlockManager());
    ethProtocolManager =
        EthProtocolManagerTestBuilder.builder()
            .setProtocolSchedule(protocolSchedule)
            .setBlockchain(localBlockchain)
            .setEthScheduler(new EthScheduler(1, 1, 1, 1, new NoOpMetricsSystem()))
            .setWorldStateArchive(localBlockchainSetup.getWorldArchive())
            .setTransactionPool(localBlockchainSetup.getTransactionPool())
            .setEthereumWireProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .build();
    final EthContext ethContext = ethProtocolManager.ethContext();
    localBlockchainSetup.importFirstBlocks(5);
    otherBlockchainSetup.importFirstBlocks(20);
    syncTargetManager =
        new FullSyncTargetManager(
            SynchronizerConfiguration.builder().build(),
            protocolSchedule,
            protocolContext,
            ethContext,
            new NoOpMetricsSystem(),
            SyncTerminationCondition.never());
  }

  @AfterEach
  public void tearDown() {
    if (ethProtocolManager != null) {
      ethProtocolManager.stop();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(FullSyncTargetManagerTest.FullSyncTargetManagerTestArguments.class)
  public void findSyncTarget_withHeightEstimates(final DataStorageFormat storageFormat) {
    setup(storageFormat);
    final BlockHeader chainHeadHeader = localBlockchain.getChainHeadHeader();
    when(localWorldState.isWorldStateAvailable(
            chainHeadHeader.getStateRoot(), chainHeadHeader.getHash()))
        .thenReturn(true);
    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, Difficulty.MAX_VALUE, 4);

    final CompletableFuture<SyncTarget> result = syncTargetManager.findSyncTarget();
    bestPeer.respond(responder);

    assertThat(result)
        .isCompletedWithValue(
            new SyncTarget(bestPeer.getEthPeer(), localBlockchain.getBlockHeader(4L).get()));
  }

  @ParameterizedTest
  @ArgumentsSource(FullSyncTargetManagerTest.FullSyncTargetManagerTestArguments.class)
  public void findSyncTarget_noHeightEstimates(final DataStorageFormat storageFormat) {
    setup(storageFormat);
    final BlockHeader chainHeadHeader = localBlockchain.getChainHeadHeader();
    when(localWorldState.isWorldStateAvailable(
            chainHeadHeader.getStateRoot(), chainHeadHeader.getHash()))
        .thenReturn(true);
    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, Difficulty.MAX_VALUE, 0);

    final CompletableFuture<SyncTarget> result = syncTargetManager.findSyncTarget();
    bestPeer.respond(responder);

    assertThat(result).isNotCompleted();
  }

  @ParameterizedTest
  @ArgumentsSource(FullSyncTargetManagerTest.FullSyncTargetManagerTestArguments.class)
  public void shouldDisconnectPeerIfWorldStateIsUnavailableForCommonAncestor(
      final DataStorageFormat storageFormat) {
    setup(storageFormat);
    final BlockHeader chainHeadHeader = localBlockchain.getChainHeadHeader();
    when(localWorldState.isWorldStateAvailable(
            chainHeadHeader.getStateRoot(), chainHeadHeader.getHash()))
        .thenReturn(false);
    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 20);

    final CompletableFuture<SyncTarget> result = syncTargetManager.findSyncTarget();

    bestPeer.respond(responder);

    assertThat(result).isNotCompleted();
    assertThat(bestPeer.getPeerConnection().isDisconnected()).isTrue();
  }

  @ParameterizedTest
  @ArgumentsSource(FullSyncTargetManagerTest.FullSyncTargetManagerTestArguments.class)
  public void shouldAllowSyncTargetWhenIfWorldStateIsAvailableForCommonAncestor(
      final DataStorageFormat storageFormat) {
    setup(storageFormat);
    final BlockHeader chainHeadHeader = localBlockchain.getChainHeadHeader();
    when(localWorldState.isWorldStateAvailable(
            chainHeadHeader.getStateRoot(), chainHeadHeader.getHash()))
        .thenReturn(true);
    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 20);

    final CompletableFuture<SyncTarget> result = syncTargetManager.findSyncTarget();

    bestPeer.respond(responder);

    assertThat(result)
        .isCompletedWithValue(
            new SyncTarget(bestPeer.getEthPeer(), localBlockchain.getChainHeadHeader()));
    assertThat(bestPeer.getPeerConnection().isDisconnected()).isFalse();
  }

  @Test
  void dryRunDetector() {
    assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
  }
}
