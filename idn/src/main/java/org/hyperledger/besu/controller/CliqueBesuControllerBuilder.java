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
package org.idnecology.idn.controller;

import org.idnecology.idn.config.CliqueConfigOptions;
import org.idnecology.idn.consensus.clique.CliqueBlockInterface;
import org.idnecology.idn.consensus.clique.CliqueContext;
import org.idnecology.idn.consensus.clique.CliqueForksSchedulesFactory;
import org.idnecology.idn.consensus.clique.CliqueHelpers;
import org.idnecology.idn.consensus.clique.CliqueMiningTracker;
import org.idnecology.idn.consensus.clique.CliqueProtocolSchedule;
import org.idnecology.idn.consensus.clique.blockcreation.CliqueBlockScheduler;
import org.idnecology.idn.consensus.clique.blockcreation.CliqueMinerExecutor;
import org.idnecology.idn.consensus.clique.blockcreation.CliqueMiningCoordinator;
import org.idnecology.idn.consensus.clique.jsonrpc.CliqueJsonRpcMethods;
import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.JsonRpcMethods;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Clique consensus controller builder. */
public class CliqueIdnControllerBuilder extends IdnControllerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(CliqueIdnControllerBuilder.class);

  private Address localAddress;
  private EpochManager epochManager;
  private final BlockInterface blockInterface = new CliqueBlockInterface();
  private ForksSchedule<CliqueConfigOptions> forksSchedule;

  /** Default constructor. */
  public CliqueIdnControllerBuilder() {}

  @Override
  protected void prepForBuild() {
    localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    final CliqueConfigOptions cliqueConfig = genesisConfigOptions.getCliqueConfigOptions();
    final long blocksPerEpoch = cliqueConfig.getEpochLength();

    epochManager = new EpochManager(blocksPerEpoch);
    forksSchedule = CliqueForksSchedulesFactory.create(genesisConfigOptions);
  }

  @Override
  protected JsonRpcMethods createAdditionalJsonRpcMethodFactory(
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final MiningConfiguration miningConfiguration) {
    return new CliqueJsonRpcMethods(protocolContext, protocolSchedule, miningConfiguration);
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    final CliqueMinerExecutor miningExecutor =
        new CliqueMinerExecutor(
            protocolContext,
            protocolSchedule,
            transactionPool,
            nodeKey,
            miningConfiguration,
            new CliqueBlockScheduler(
                clock,
                protocolContext.getConsensusContext(CliqueContext.class).getValidatorProvider(),
                localAddress,
                forksSchedule),
            epochManager,
            forksSchedule,
            ethProtocolManager.ethContext().getScheduler());
    final CliqueMiningCoordinator miningCoordinator =
        new CliqueMiningCoordinator(
            protocolContext.getBlockchain(),
            miningExecutor,
            syncState,
            new CliqueMiningTracker(localAddress, protocolContext));

    // Update the next block period in seconds according to the transition schedule
    protocolContext
        .getBlockchain()
        .observeBlockAdded(
            o ->
                miningConfiguration.setBlockPeriodSeconds(
                    forksSchedule
                        .getFork(o.getBlock().getHeader().getNumber() + 1)
                        .getValue()
                        .getBlockPeriodSeconds()));

    miningCoordinator.addMinedBlockObserver(ethProtocolManager);

    // Clique mining is implicitly enabled.
    miningCoordinator.enable();
    return miningCoordinator;
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return CliqueProtocolSchedule.create(
        genesisConfigOptions,
        forksSchedule,
        nodeKey,
        privacyParameters,
        isRevertReasonEnabled,
        evmConfiguration,
        miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
        metricsSystem);
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (blockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    return new CliqueQueryPluginServiceFactory(blockchain, nodeKey);
  }

  @Override
  protected CliqueContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    final CliqueContext cliqueContext =
        new CliqueContext(
            BlockValidatorProvider.nonForkingValidatorProvider(
                blockchain, epochManager, blockInterface),
            epochManager,
            blockInterface);
    CliqueHelpers.setCliqueContext(cliqueContext);
    CliqueHelpers.installCliqueBlockChoiceRule(blockchain, cliqueContext);
    return cliqueContext;
  }

  @Override
  public MiningConfiguration getMiningParameterOverrides(final MiningConfiguration fromCli) {
    // Clique mines by default, reflect that with in the mining parameters:
    return fromCli.setMiningEnabled(true);
  }
}
