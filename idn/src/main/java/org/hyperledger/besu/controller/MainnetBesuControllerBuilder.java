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

import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.DefaultBlockScheduler;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.blockcreation.PoWMinerExecutor;
import org.idnecology.idn.ethereum.blockcreation.PoWMiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.EpochCalculator;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import java.util.Optional;

/** The Mainnet idn controller builder. */
public class MainnetIdnControllerBuilder extends IdnControllerBuilder {

  private EpochCalculator epochCalculator = new EpochCalculator.DefaultEpochCalculator();

  /** Default constructor. */
  public MainnetIdnControllerBuilder() {}

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {

    final PoWMinerExecutor executor =
        new PoWMinerExecutor(
            protocolContext,
            protocolSchedule,
            transactionPool,
            miningConfiguration,
            new DefaultBlockScheduler(
                MainnetBlockHeaderValidator.MINIMUM_SECONDS_SINCE_PARENT,
                MainnetBlockHeaderValidator.TIMESTAMP_TOLERANCE_S,
                clock),
            epochCalculator,
            ethProtocolManager.ethContext().getScheduler());

    final PoWMiningCoordinator miningCoordinator =
        new PoWMiningCoordinator(
            protocolContext.getBlockchain(),
            executor,
            syncState,
            miningConfiguration.getUnstable().getRemoteSealersLimit(),
            miningConfiguration.getUnstable().getRemoteSealersTimeToLive());
    miningCoordinator.addMinedBlockObserver(ethProtocolManager);
    miningCoordinator.setStratumMiningEnabled(miningConfiguration.isStratumMiningEnabled());
    if (miningConfiguration.isMiningEnabled()) {
      miningCoordinator.enable();
    }

    return miningCoordinator;
  }

  @Override
  protected ConsensusContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    return null;
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    return new NoopPluginServiceFactory();
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return MainnetProtocolSchedule.fromConfig(
        genesisConfigOptions,
        Optional.of(privacyParameters),
        Optional.of(isRevertReasonEnabled),
        Optional.of(evmConfiguration),
        super.miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
        metricsSystem);
  }

  @Override
  protected void prepForBuild() {
    genesisConfigOptions
        .getThanosBlockNumber()
        .ifPresent(
            activationBlock -> epochCalculator = new EpochCalculator.Ecip1099EpochCalculator());
  }
}
