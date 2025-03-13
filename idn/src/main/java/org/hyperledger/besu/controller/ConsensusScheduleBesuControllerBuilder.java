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

import static org.idnecology.idn.ethereum.core.BlockHeader.GENESIS_BLOCK_NUMBER;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.consensus.common.CombinedProtocolScheduleFactory;
import org.idnecology.idn.consensus.common.ForkSpec;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.MigratingConsensusContext;
import org.idnecology.idn.consensus.common.MigratingMiningCoordinator;
import org.idnecology.idn.consensus.common.MigratingProtocolContext;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.JsonRpcMethods;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthMessages;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.MergePeerFilter;
import org.idnecology.idn.ethereum.eth.manager.snap.SnapProtocolManager;
import org.idnecology.idn.ethereum.eth.peervalidation.PeerValidator;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.forkid.ForkIdManager;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.config.SubProtocolConfiguration;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.plugin.services.permissioning.NodeMessagePermissioningProvider;

import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * This is a placeholder class for the QBFT migration logic. For now, all it does is to delegate any
 * IdnControllerBuilder to the first controller in the list.
 */
public class ConsensusScheduleIdnControllerBuilder extends IdnControllerBuilder {

  private final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule = new HashMap<>();
  private final BiFunction<
          NavigableSet<ForkSpec<ProtocolSchedule>>, Optional<BigInteger>, ProtocolSchedule>
      combinedProtocolScheduleFactory;

  /**
   * Instantiates a new Consensus schedule Idn controller builder.
   *
   * @param idnControllerBuilderSchedule the idn controller builder schedule
   */
  public ConsensusScheduleIdnControllerBuilder(
      final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule) {
    this(
        idnControllerBuilderSchedule,
        (protocolScheduleSpecs, chainId) ->
            new CombinedProtocolScheduleFactory().create(protocolScheduleSpecs, chainId));
  }

  /**
   * Instantiates a new Consensus schedule idn controller builder. Visible for testing.
   *
   * @param idnControllerBuilderSchedule the idn controller builder schedule
   * @param combinedProtocolScheduleFactory the combined protocol schedule factory
   */
  @VisibleForTesting
  protected ConsensusScheduleIdnControllerBuilder(
      final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule,
      final BiFunction<
              NavigableSet<ForkSpec<ProtocolSchedule>>, Optional<BigInteger>, ProtocolSchedule>
          combinedProtocolScheduleFactory) {
    Preconditions.checkNotNull(
        idnControllerBuilderSchedule, "IdnControllerBuilder schedule can't be null");
    Preconditions.checkArgument(
        !idnControllerBuilderSchedule.isEmpty(), "IdnControllerBuilder schedule can't be empty");
    this.idnControllerBuilderSchedule.putAll(idnControllerBuilderSchedule);
    this.combinedProtocolScheduleFactory = combinedProtocolScheduleFactory;
  }

  @Override
  protected void prepForBuild() {
    idnControllerBuilderSchedule.values().forEach(IdnControllerBuilder::prepForBuild);
    super.prepForBuild();
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {

    final List<ForkSpec<MiningCoordinator>> miningCoordinatorForkSpecs =
        idnControllerBuilderSchedule.entrySet().stream()
            .map(
                e ->
                    new ForkSpec<>(
                        e.getKey(),
                        e.getValue()
                            .createMiningCoordinator(
                                protocolSchedule,
                                protocolContext,
                                transactionPool,
                                miningConfiguration,
                                syncState,
                                ethProtocolManager)))
            .collect(Collectors.toList());
    final ForksSchedule<MiningCoordinator> miningCoordinatorSchedule =
        new ForksSchedule<>(miningCoordinatorForkSpecs);

    return new MigratingMiningCoordinator(
        miningCoordinatorSchedule, protocolContext.getBlockchain());
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    final NavigableSet<ForkSpec<ProtocolSchedule>> protocolScheduleSpecs =
        idnControllerBuilderSchedule.entrySet().stream()
            .map(e -> new ForkSpec<>(e.getKey(), e.getValue().createProtocolSchedule()))
            .collect(Collectors.toCollection(() -> new TreeSet<>(ForkSpec.COMPARATOR)));
    final Optional<BigInteger> chainId = genesisConfigOptions.getChainId();
    return combinedProtocolScheduleFactory.apply(protocolScheduleSpecs, chainId);
  }

  @Override
  protected ProtocolContext createProtocolContext(
      final MutableBlockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ConsensusContext consensusContext) {
    return new MigratingProtocolContext(
        blockchain,
        worldStateArchive,
        consensusContext.as(MigratingConsensusContext.class),
        badBlockManager);
  }

  @Override
  protected ConsensusContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    final List<ForkSpec<ConsensusContext>> consensusContextSpecs =
        idnControllerBuilderSchedule.entrySet().stream()
            .map(
                e ->
                    new ForkSpec<>(
                        e.getKey(),
                        e.getValue()
                            .createConsensusContext(
                                blockchain, worldStateArchive, protocolSchedule)))
            .toList();
    final ForksSchedule<ConsensusContext> consensusContextsSchedule =
        new ForksSchedule<>(consensusContextSpecs);
    return new MigratingConsensusContext(consensusContextsSchedule);
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.createAdditionalPluginServices(blockchain, protocolContext));
    return new NoopPluginServiceFactory();
  }

  @Override
  protected JsonRpcMethods createAdditionalJsonRpcMethodFactory(
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final MiningConfiguration miningConfiguration) {
    idnControllerBuilderSchedule
        .values()
        .forEach(
            b ->
                b.createAdditionalJsonRpcMethodFactory(
                    protocolContext, protocolSchedule, miningConfiguration));
    return super.createAdditionalJsonRpcMethodFactory(
        protocolContext, protocolSchedule, miningConfiguration);
  }

  @Override
  protected SubProtocolConfiguration createSubProtocolConfiguration(
      final EthProtocolManager ethProtocolManager,
      final Optional<SnapProtocolManager> maybeSnapProtocolManager) {
    return idnControllerBuilderSchedule
        .get(idnControllerBuilderSchedule.keySet().stream().skip(1).findFirst().orElseThrow())
        .createSubProtocolConfiguration(ethProtocolManager, maybeSnapProtocolManager);
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    idnControllerBuilderSchedule.get(GENESIS_BLOCK_NUMBER).validateContext(context);
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
    idnControllerBuilderSchedule
        .values()
        .forEach(
            b ->
                b.createEthProtocolManager(
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
                    forkIdManager));
    return super.createEthProtocolManager(
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
  public IdnControllerBuilder storageProvider(final StorageProvider storageProvider) {
    idnControllerBuilderSchedule.values().forEach(b -> b.storageProvider(storageProvider));
    return super.storageProvider(storageProvider);
  }

  @Override
  public IdnControllerBuilder genesisConfig(final GenesisConfig genesisConfig) {
    idnControllerBuilderSchedule.values().forEach(b -> b.genesisConfig(genesisConfig));
    return super.genesisConfig(genesisConfig);
  }

  @Override
  public IdnControllerBuilder synchronizerConfiguration(
      final SynchronizerConfiguration synchronizerConfig) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.synchronizerConfiguration(synchronizerConfig));
    return super.synchronizerConfiguration(synchronizerConfig);
  }

  @Override
  public IdnControllerBuilder ethProtocolConfiguration(
      final EthProtocolConfiguration ethProtocolConfiguration) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.ethProtocolConfiguration(ethProtocolConfiguration));
    return super.ethProtocolConfiguration(ethProtocolConfiguration);
  }

  @Override
  public IdnControllerBuilder networkId(final BigInteger networkId) {
    idnControllerBuilderSchedule.values().forEach(b -> b.networkId(networkId));
    return super.networkId(networkId);
  }

  @Override
  public IdnControllerBuilder miningParameters(final MiningConfiguration miningConfiguration) {
    idnControllerBuilderSchedule.values().forEach(b -> b.miningParameters(miningConfiguration));
    return super.miningParameters(miningConfiguration);
  }

  @Override
  public IdnControllerBuilder messagePermissioningProviders(
      final List<NodeMessagePermissioningProvider> messagePermissioningProviders) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.messagePermissioningProviders(messagePermissioningProviders));
    return super.messagePermissioningProviders(messagePermissioningProviders);
  }

  @Override
  public IdnControllerBuilder nodeKey(final NodeKey nodeKey) {
    idnControllerBuilderSchedule.values().forEach(b -> b.nodeKey(nodeKey));
    return super.nodeKey(nodeKey);
  }

  @Override
  public IdnControllerBuilder metricsSystem(final ObservableMetricsSystem metricsSystem) {
    idnControllerBuilderSchedule.values().forEach(b -> b.metricsSystem(metricsSystem));
    return super.metricsSystem(metricsSystem);
  }

  @Override
  public IdnControllerBuilder privacyParameters(final PrivacyParameters privacyParameters) {
    idnControllerBuilderSchedule.values().forEach(b -> b.privacyParameters(privacyParameters));
    return super.privacyParameters(privacyParameters);
  }

  @Override
  public IdnControllerBuilder dataDirectory(final Path dataDirectory) {
    idnControllerBuilderSchedule.values().forEach(b -> b.dataDirectory(dataDirectory));
    return super.dataDirectory(dataDirectory);
  }

  @Override
  public IdnControllerBuilder clock(final Clock clock) {
    idnControllerBuilderSchedule.values().forEach(b -> b.clock(clock));
    return super.clock(clock);
  }

  @Override
  public IdnControllerBuilder transactionPoolConfiguration(
      final TransactionPoolConfiguration transactionPoolConfiguration) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.transactionPoolConfiguration(transactionPoolConfiguration));
    return super.transactionPoolConfiguration(transactionPoolConfiguration);
  }

  @Override
  public IdnControllerBuilder isRevertReasonEnabled(final boolean isRevertReasonEnabled) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.isRevertReasonEnabled(isRevertReasonEnabled));
    return super.isRevertReasonEnabled(isRevertReasonEnabled);
  }

  @Override
  public IdnControllerBuilder isParallelTxProcessingEnabled(
      final boolean isParallelTxProcessingEnabled) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.isParallelTxProcessingEnabled(isParallelTxProcessingEnabled));
    return super.isParallelTxProcessingEnabled(isParallelTxProcessingEnabled);
  }

  @Override
  public IdnControllerBuilder requiredBlocks(final Map<Long, Hash> requiredBlocks) {
    idnControllerBuilderSchedule.values().forEach(b -> b.requiredBlocks(requiredBlocks));
    return super.requiredBlocks(requiredBlocks);
  }

  @Override
  public IdnControllerBuilder reorgLoggingThreshold(final long reorgLoggingThreshold) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.reorgLoggingThreshold(reorgLoggingThreshold));
    return super.reorgLoggingThreshold(reorgLoggingThreshold);
  }

  @Override
  public IdnControllerBuilder dataStorageConfiguration(
      final DataStorageConfiguration dataStorageConfiguration) {
    idnControllerBuilderSchedule
        .values()
        .forEach(b -> b.dataStorageConfiguration(dataStorageConfiguration));
    return super.dataStorageConfiguration(dataStorageConfiguration);
  }

  @Override
  public IdnControllerBuilder evmConfiguration(final EvmConfiguration evmConfiguration) {
    idnControllerBuilderSchedule.values().forEach(b -> b.evmConfiguration(evmConfiguration));
    return super.evmConfiguration(evmConfiguration);
  }

  /**
   * Gets idn controller builder schedule. Visible for testing.
   *
   * @return the Idn controller builder schedule
   */
  @VisibleForTesting
  Map<Long, IdnControllerBuilder> getIdnControllerBuilderSchedule() {
    return idnControllerBuilderSchedule;
  }
}
