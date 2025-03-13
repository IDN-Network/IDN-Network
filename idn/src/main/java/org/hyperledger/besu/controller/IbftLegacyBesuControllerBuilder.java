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

import org.idnecology.idn.config.IbftLegacyConfigOptions;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftContext;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.consensus.ibftlegacy.IbftExtraDataCodec;
import org.idnecology.idn.consensus.ibftlegacy.IbftLegacyBlockInterface;
import org.idnecology.idn.consensus.ibftlegacy.IbftProtocolSchedule;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.blockcreation.NoopMiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Ibft legacy idn controller builder. */
public class IbftLegacyIdnControllerBuilder extends IdnControllerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(IbftLegacyIdnControllerBuilder.class);
  private final BftBlockInterface blockInterface;

  /** Default constructor */
  public IbftLegacyIdnControllerBuilder() {
    LOG.warn(
        "IBFT1 is deprecated. This consensus configuration should be used only while migrating to another consensus mechanism.");
    this.blockInterface = new IbftLegacyBlockInterface(new IbftExtraDataCodec());
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    return new NoopMiningCoordinator(miningConfiguration);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return IbftProtocolSchedule.create(
        genesisConfigOptions, privacyParameters, isRevertReasonEnabled, evmConfiguration);
  }

  @Override
  protected BftContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    final IbftLegacyConfigOptions ibftConfig = genesisConfigOptions.getIbftLegacyConfigOptions();
    final EpochManager epochManager = new EpochManager(ibftConfig.getEpochLength());
    final ValidatorProvider validatorProvider =
        BlockValidatorProvider.nonForkingValidatorProvider(
            blockchain, epochManager, blockInterface);

    return new BftContext(validatorProvider, epochManager, blockInterface);
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    return new NoopPluginServiceFactory();
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (blockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }
}
