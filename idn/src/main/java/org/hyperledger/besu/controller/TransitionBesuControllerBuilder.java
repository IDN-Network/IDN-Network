/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.controller;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.consensus.merge.PostMergeContext;
import org.idnecology.idn.consensus.merge.TransitionBackwardSyncContext;
import org.idnecology.idn.consensus.merge.TransitionContext;
import org.idnecology.idn.consensus.merge.TransitionProtocolSchedule;
import org.idnecology.idn.consensus.merge.blockcreation.TransitionCoordinator;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthMessages;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.MergePeerFilter;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.peervalidation.PeerValidator;
import org.idnecology.idn.ethereum.eth.sync.DefaultSynchronizer;
import org.idnecology.idn.ethereum.eth.sync.PivotBlockSelector;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.backwardsync.BackwardSyncContext;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.forkid.ForkIdManager;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.plugin.services.permissioning.NodeMessagePermissioningProvider;

import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Transition idn controller builder. */
public class TransitionIdnControllerBuilder extends IdnControllerBuilder {
  private final IdnControllerBuilder preMergeIdnControllerBuilder;
  private final MergeIdnControllerBuilder mergeIdnControllerBuilder;

  private static final Logger LOG = LoggerFactory.getLogger(TransitionIdnControllerBuilder.class);
  private TransitionProtocolSchedule transitionProtocolSchedule;

  /**
   * Instantiates a new Transition idn controller builder.
   *
   * @param preMergeIdnControllerBuilder the pre merge idn controller builder
   * @param mergeIdnControllerBuilder the merge idn controller builder
   */
  public TransitionIdnControllerBuilder(
      final IdnControllerBuilder preMergeIdnControllerBuilder,
      final MergeIdnControllerBuilder mergeIdnControllerBuilder) {
    this.preMergeIdnControllerBuilder = preMergeIdnControllerBuilder;
    this.mergeIdnControllerBuilder = mergeIdnControllerBuilder;
  }

  @Override
  protected void prepForBuild() {
    preMergeIdnControllerBuilder.prepForBuild();
    mergeIdnControllerBuilder.prepForBuild();
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {

    // cast to transition schedule for explicit access to pre and post objects:
    final TransitionProtocolSchedule transitionProtocolSchedule =
        (TransitionProtocolSchedule) protocolSchedule;

    // PoA consensus mines by default, get consensus-specific mining parameters for
    // TransitionCoordinator:
    MiningConfiguration transitionMiningConfiguration =
        preMergeIdnControllerBuilder.getMiningParameterOverrides(miningConfiguration);

    // construct a transition backward sync context
    BackwardSyncContext transitionBackwardsSyncContext =
        new TransitionBackwardSyncContext(
            protocolContext,
            transitionProtocolSchedule,
            syncConfig,
            metricsSystem,
            ethProtocolManager.ethContext(),
            syncState,
            storageProvider);

    final TransitionCoordinator composedCoordinator =
        new TransitionCoordinator(
            preMergeIdnControllerBuilder.createMiningCoordinator(
                transitionProtocolSchedule.getPreMergeSchedule(),
                protocolContext,
                transactionPool,
                ImmutableMiningConfiguration.builder()
                    .from(miningConfiguration)
                    .mutableInitValues(
                        ImmutableMiningConfiguration.MutableInitValues.builder()
                            .isMiningEnabled(false)
                            .build())
                    .build(),
                syncState,
                ethProtocolManager),
            mergeIdnControllerBuilder.createTransitionMiningCoordinator(
                transitionProtocolSchedule,
                protocolContext,
                transactionPool,
                transitionMiningConfiguration,
                syncState,
                transitionBackwardsSyncContext,
                ethProtocolManager.ethContext().getScheduler()),
            mergeIdnControllerBuilder.getPostMergeContext());
    initTransitionWatcher(protocolContext, composedCoordinator);
    return composedCoordinator;
  }

  @Override
  protected EthProtocolManager createEthProtocolManager(
      final ProtocolContext protocolContext,
      final SynchronizerConfiguration synchronizerConfiguration,
      final TransactionPool transactionPool,
      final EthProtocolConfiguration ethereumWireProtocolConfiguration,
      final EthPeers ethPeers,
      final EthContext ethContext,
      final EthMessages ethMessages,
      final EthScheduler scheduler,
      final List<PeerValidator> peerValidators,
      final Optional<MergePeerFilter> mergePeerFilter,
      final ForkIdManager forkIdManager) {
    return mergeIdnControllerBuilder.createEthProtocolManager(
        protocolContext,
        synchronizerConfiguration,
        transactionPool,
        ethereumWireProtocolConfiguration,
        ethPeers,
        ethContext,
        ethMessages,
        scheduler,
        peerValidators,
        mergePeerFilter,
        forkIdManager);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    transitionProtocolSchedule =
        new TransitionProtocolSchedule(
            preMergeIdnControllerBuilder.createProtocolSchedule(),
            mergeIdnControllerBuilder.createProtocolSchedule(),
            mergeIdnControllerBuilder.getPostMergeContext());
    return transitionProtocolSchedule;
  }

  @Override
  protected ProtocolContext createProtocolContext(
      final MutableBlockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ConsensusContext consensusContext) {
    final ProtocolContext protocolContext =
        super.createProtocolContext(blockchain, worldStateArchive, consensusContext);
    transitionProtocolSchedule.setProtocolContext(protocolContext);
    return protocolContext;
  }

  @Override
  protected ConsensusContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    return new TransitionContext(
        preMergeIdnControllerBuilder.createConsensusContext(
            blockchain, worldStateArchive, protocolSchedule),
        mergeIdnControllerBuilder.createConsensusContext(
            blockchain, worldStateArchive, protocolSchedule));
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    return new NoopPluginServiceFactory();
  }

  @Override
  protected DefaultSynchronizer createSynchronizer(
      final ProtocolSchedule protocolSchedule,
      final WorldStateStorageCoordinator worldStateStorageCoordinator,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final PeerTaskExecutor peerTaskExecutor,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager,
      final PivotBlockSelector pivotBlockSelector) {

    DefaultSynchronizer sync =
        super.createSynchronizer(
            protocolSchedule,
            worldStateStorageCoordinator,
            protocolContext,
            ethContext,
            peerTaskExecutor,
            syncState,
            ethProtocolManager,
            pivotBlockSelector);

    if (genesisConfigOptions.getTerminalTotalDifficulty().isPresent()) {
      LOG.info(
          "TTD present, creating DefaultSynchronizer that stops propagating after finalization");
      protocolContext
          .getConsensusContext(MergeContext.class)
          .addNewUnverifiedForkchoiceListener(sync);
    }

    return sync;
  }

  @SuppressWarnings("UnusedVariable")
  private void initTransitionWatcher(
      final ProtocolContext protocolContext, final TransitionCoordinator composedCoordinator) {

    PostMergeContext postMergeContext = mergeIdnControllerBuilder.getPostMergeContext();
    postMergeContext.observeNewIsPostMergeState(
        (isPoS, priorState, difficultyStoppedAt) -> {
          if (isPoS) {
            // if we transitioned to post-merge, stop and disable any mining
            composedCoordinator.getPreMergeObject().disable();
            composedCoordinator.getPreMergeObject().stop();
            // set the blockchoiceRule to never reorg, rely on forkchoiceUpdated instead
            protocolContext
                .getBlockchain()
                .setBlockChoiceRule((newBlockHeader, currentBlockHeader) -> -1);

          } else if (composedCoordinator.isMiningBeforeMerge()) {
            // if our merge state is set to mine pre-merge and we are mining, start mining
            composedCoordinator.getPreMergeObject().enable();
            composedCoordinator.getPreMergeObject().start();
          }
        });

    // initialize our merge context merge status before we would start either
    Blockchain blockchain = protocolContext.getBlockchain();
    blockchain
        .getTotalDifficultyByHash(blockchain.getChainHeadHash())
        .ifPresent(postMergeContext::setIsPostMerge);
  }

  @Override
  public IdnControllerBuilder storageProvider(final StorageProvider storageProvider) {
    super.storageProvider(storageProvider);
    return propagateConfig(z -> z.storageProvider(storageProvider));
  }

  @Override
  public IdnController build() {
    final IdnController controller = super.build();
    mergeIdnControllerBuilder.getPostMergeContext().setSyncState(controller.getSyncState());
    return controller;
  }

  @Override
  public IdnControllerBuilder evmConfiguration(final EvmConfiguration evmConfiguration) {
    super.evmConfiguration(evmConfiguration);
    return propagateConfig(z -> z.evmConfiguration(evmConfiguration));
  }

  @Override
  public IdnControllerBuilder genesisConfig(final GenesisConfig genesisConfig) {
    super.genesisConfig(genesisConfig);
    return propagateConfig(z -> z.genesisConfig(genesisConfig));
  }

  @Override
  public IdnControllerBuilder synchronizerConfiguration(
      final SynchronizerConfiguration synchronizerConfig) {
    super.synchronizerConfiguration(synchronizerConfig);
    return propagateConfig(z -> z.synchronizerConfiguration(synchronizerConfig));
  }

  @Override
  public IdnControllerBuilder ethProtocolConfiguration(
      final EthProtocolConfiguration ethProtocolConfiguration) {
    super.ethProtocolConfiguration(ethProtocolConfiguration);
    return propagateConfig(z -> z.ethProtocolConfiguration(ethProtocolConfiguration));
  }

  @Override
  public IdnControllerBuilder networkId(final BigInteger networkId) {
    super.networkId(networkId);
    return propagateConfig(z -> z.networkId(networkId));
  }

  @Override
  public IdnControllerBuilder miningParameters(final MiningConfiguration miningConfiguration) {
    super.miningParameters(miningConfiguration);
    return propagateConfig(z -> z.miningParameters(miningConfiguration));
  }

  @Override
  public IdnControllerBuilder messagePermissioningProviders(
      final List<NodeMessagePermissioningProvider> messagePermissioningProviders) {
    super.messagePermissioningProviders(messagePermissioningProviders);
    return propagateConfig(z -> z.messagePermissioningProviders(messagePermissioningProviders));
  }

  @Override
  public IdnControllerBuilder nodeKey(final NodeKey nodeKey) {
    super.nodeKey(nodeKey);
    return propagateConfig(z -> z.nodeKey(nodeKey));
  }

  @Override
  public IdnControllerBuilder metricsSystem(final ObservableMetricsSystem metricsSystem) {
    super.metricsSystem(metricsSystem);
    return propagateConfig(z -> z.metricsSystem(metricsSystem));
  }

  @Override
  public IdnControllerBuilder privacyParameters(final PrivacyParameters privacyParameters) {
    super.privacyParameters(privacyParameters);
    return propagateConfig(z -> z.privacyParameters(privacyParameters));
  }

  @Override
  public IdnControllerBuilder dataDirectory(final Path dataDirectory) {
    super.dataDirectory(dataDirectory);
    return propagateConfig(z -> z.dataDirectory(dataDirectory));
  }

  @Override
  public IdnControllerBuilder clock(final Clock clock) {
    super.clock(clock);
    return propagateConfig(z -> z.clock(clock));
  }

  @Override
  public IdnControllerBuilder transactionPoolConfiguration(
      final TransactionPoolConfiguration transactionPoolConfiguration) {
    super.transactionPoolConfiguration(transactionPoolConfiguration);
    return propagateConfig(z -> z.transactionPoolConfiguration(transactionPoolConfiguration));
  }

  @Override
  public IdnControllerBuilder isRevertReasonEnabled(final boolean isRevertReasonEnabled) {
    super.isRevertReasonEnabled(isRevertReasonEnabled);
    return propagateConfig(z -> z.isRevertReasonEnabled(isRevertReasonEnabled));
  }

  @Override
  public IdnControllerBuilder isParallelTxProcessingEnabled(
      final boolean isParallelTxProcessingEnabled) {
    super.isParallelTxProcessingEnabled(isParallelTxProcessingEnabled);
    return propagateConfig(z -> z.isParallelTxProcessingEnabled(isParallelTxProcessingEnabled));
  }

  @Override
  public IdnControllerBuilder requiredBlocks(final Map<Long, Hash> requiredBlocks) {
    super.requiredBlocks(requiredBlocks);
    return propagateConfig(z -> z.requiredBlocks(requiredBlocks));
  }

  @Override
  public IdnControllerBuilder reorgLoggingThreshold(final long reorgLoggingThreshold) {
    super.reorgLoggingThreshold(reorgLoggingThreshold);
    return propagateConfig(z -> z.reorgLoggingThreshold(reorgLoggingThreshold));
  }

  @Override
  public IdnControllerBuilder dataStorageConfiguration(
      final DataStorageConfiguration dataStorageConfiguration) {
    super.dataStorageConfiguration(dataStorageConfiguration);
    return propagateConfig(z -> z.dataStorageConfiguration(dataStorageConfiguration));
  }

  private IdnControllerBuilder propagateConfig(final Consumer<IdnControllerBuilder> toPropagate) {
    toPropagate.accept(preMergeIdnControllerBuilder);
    toPropagate.accept(mergeIdnControllerBuilder);
    return this;
  }
}
