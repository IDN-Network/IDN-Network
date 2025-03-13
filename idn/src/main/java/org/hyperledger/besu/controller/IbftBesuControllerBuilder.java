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

import org.idnecology.idn.config.BftConfigOptions;
import org.idnecology.idn.config.BftFork;
import org.idnecology.idn.consensus.common.BftValidatorOverrides;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftContext;
import org.idnecology.idn.consensus.common.bft.BftEventQueue;
import org.idnecology.idn.consensus.common.bft.BftExecutors;
import org.idnecology.idn.consensus.common.bft.BftProcessor;
import org.idnecology.idn.consensus.common.bft.BftProtocolSchedule;
import org.idnecology.idn.consensus.common.bft.BlockTimer;
import org.idnecology.idn.consensus.common.bft.EthSynchronizerUpdater;
import org.idnecology.idn.consensus.common.bft.EventMultiplexer;
import org.idnecology.idn.consensus.common.bft.MessageTracker;
import org.idnecology.idn.consensus.common.bft.RoundTimer;
import org.idnecology.idn.consensus.common.bft.UniqueMessageMulticaster;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftMiningCoordinator;
import org.idnecology.idn.consensus.common.bft.blockcreation.ProposerSelector;
import org.idnecology.idn.consensus.common.bft.network.ValidatorPeers;
import org.idnecology.idn.consensus.common.bft.protocol.BftProtocolManager;
import org.idnecology.idn.consensus.common.bft.statemachine.BftEventHandler;
import org.idnecology.idn.consensus.common.bft.statemachine.BftFinalState;
import org.idnecology.idn.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.consensus.ibft.IbftForksSchedulesFactory;
import org.idnecology.idn.consensus.ibft.IbftGossip;
import org.idnecology.idn.consensus.ibft.IbftProtocolScheduleBuilder;
import org.idnecology.idn.consensus.ibft.jsonrpc.IbftJsonRpcMethods;
import org.idnecology.idn.consensus.ibft.payload.MessageFactory;
import org.idnecology.idn.consensus.ibft.protocol.IbftSubProtocol;
import org.idnecology.idn.consensus.ibft.statemachine.IbftBlockHeightManagerFactory;
import org.idnecology.idn.consensus.ibft.statemachine.IbftController;
import org.idnecology.idn.consensus.ibft.statemachine.IbftRoundFactory;
import org.idnecology.idn.consensus.ibft.validation.MessageValidatorFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.JsonRpcMethods;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.MinedBlockObserver;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.eth.EthProtocol;
import org.idnecology.idn.ethereum.eth.SnapProtocol;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.snap.SnapProtocolManager;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.config.SubProtocolConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.plugin.services.IdnEvents;
import org.idnecology.idn.util.Subscribers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Ibft idn controller builder. */
public class IbftIdnControllerBuilder extends IdnControllerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(IbftIdnControllerBuilder.class);
  private BftEventQueue bftEventQueue;
  private BftConfigOptions bftConfig;
  private ForksSchedule<BftConfigOptions> forksSchedule;
  private ValidatorPeers peers;
  private IbftExtraDataCodec bftExtraDataCodec;
  private BftBlockInterface bftBlockInterface;

  /** Default Constructor */
  public IbftIdnControllerBuilder() {}

  @Override
  protected void prepForBuild() {
    bftConfig = genesisConfigOptions.getBftConfigOptions();
    bftEventQueue = new BftEventQueue(bftConfig.getMessageQueueLimit());
    forksSchedule = IbftForksSchedulesFactory.create(genesisConfigOptions);
    bftExtraDataCodec = new IbftExtraDataCodec();
    bftBlockInterface = new BftBlockInterface(bftExtraDataCodec);
  }

  @Override
  protected JsonRpcMethods createAdditionalJsonRpcMethodFactory(
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final MiningConfiguration miningConfiguration) {
    return new IbftJsonRpcMethods(protocolContext, protocolSchedule, miningConfiguration);
  }

  @Override
  protected SubProtocolConfiguration createSubProtocolConfiguration(
      final EthProtocolManager ethProtocolManager,
      final Optional<SnapProtocolManager> maybeSnapProtocolManager) {
    final SubProtocolConfiguration subProtocolConfiguration =
        new SubProtocolConfiguration()
            .withSubProtocol(EthProtocol.get(), ethProtocolManager)
            .withSubProtocol(
                IbftSubProtocol.get(),
                new BftProtocolManager(
                    bftEventQueue, peers, IbftSubProtocol.IBFV1, IbftSubProtocol.get().getName()));
    maybeSnapProtocolManager.ifPresent(
        snapProtocolManager -> {
          subProtocolConfiguration.withSubProtocol(SnapProtocol.get(), snapProtocolManager);
        });
    return subProtocolConfiguration;
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    final MutableBlockchain blockchain = protocolContext.getBlockchain();
    final BftExecutors bftExecutors =
        BftExecutors.create(metricsSystem, BftExecutors.ConsensusType.IBFT);

    final Address localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    final BftProtocolSchedule bftProtocolSchedule = (BftProtocolSchedule) protocolSchedule;
    final BftBlockCreatorFactory<?> blockCreatorFactory =
        new BftBlockCreatorFactory<>(
            transactionPool,
            protocolContext,
            bftProtocolSchedule,
            forksSchedule,
            miningConfiguration,
            localAddress,
            bftExtraDataCodec,
            ethProtocolManager.ethContext().getScheduler());

    final ValidatorProvider validatorProvider =
        protocolContext.getConsensusContext(BftContext.class).getValidatorProvider();

    final ProposerSelector proposerSelector =
        new ProposerSelector(blockchain, bftBlockInterface, true, validatorProvider);

    // NOTE: peers should not be used for accessing the network as it does not enforce the
    // "only send once" filter applied by the UniqueMessageMulticaster.
    peers = new ValidatorPeers(validatorProvider, IbftSubProtocol.NAME);

    final UniqueMessageMulticaster uniqueMessageMulticaster =
        new UniqueMessageMulticaster(peers, bftConfig.getGossipedHistoryLimit());

    final IbftGossip gossiper = new IbftGossip(uniqueMessageMulticaster);

    final BftFinalState finalState =
        new BftFinalState(
            validatorProvider,
            nodeKey,
            Util.publicKeyToAddress(nodeKey.getPublicKey()),
            proposerSelector,
            uniqueMessageMulticaster,
            new RoundTimer(
                bftEventQueue,
                Duration.ofSeconds(bftConfig.getRequestTimeoutSeconds()),
                bftExecutors),
            new BlockTimer(bftEventQueue, forksSchedule, bftExecutors, clock),
            blockCreatorFactory,
            clock);

    final MessageValidatorFactory messageValidatorFactory =
        new MessageValidatorFactory(
            proposerSelector, bftProtocolSchedule, protocolContext, bftExtraDataCodec);

    final Subscribers<MinedBlockObserver> minedBlockObservers = Subscribers.create();
    minedBlockObservers.subscribe(ethProtocolManager);
    minedBlockObservers.subscribe(blockLogger(transactionPool, localAddress));

    final FutureMessageBuffer futureMessageBuffer =
        new FutureMessageBuffer(
            bftConfig.getFutureMessagesMaxDistance(),
            bftConfig.getFutureMessagesLimit(),
            blockchain.getChainHeadBlockNumber());
    final MessageTracker duplicateMessageTracker =
        new MessageTracker(bftConfig.getDuplicateMessageLimit());

    final MessageFactory messageFactory = new MessageFactory(nodeKey);

    final BftEventHandler ibftController =
        new IbftController(
            blockchain,
            finalState,
            new IbftBlockHeightManagerFactory(
                finalState,
                new IbftRoundFactory(
                    finalState,
                    protocolContext,
                    bftProtocolSchedule,
                    minedBlockObservers,
                    messageValidatorFactory,
                    messageFactory,
                    bftExtraDataCodec),
                messageValidatorFactory,
                messageFactory),
            gossiper,
            duplicateMessageTracker,
            futureMessageBuffer,
            new EthSynchronizerUpdater(ethProtocolManager.ethContext().getEthPeers()));

    final EventMultiplexer eventMultiplexer = new EventMultiplexer(ibftController);
    final BftProcessor bftProcessor = new BftProcessor(bftEventQueue, eventMultiplexer);

    final MiningCoordinator ibftMiningCoordinator =
        new BftMiningCoordinator(
            bftExecutors,
            ibftController,
            bftProcessor,
            blockCreatorFactory,
            blockchain,
            bftEventQueue);

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

    syncState.subscribeSyncStatus(
        syncStatus -> {
          if (syncState.syncTarget().isPresent()) {
            // We're syncing so stop doing other stuff
            LOG.info("Stopping IBFT mining coordinator while we are syncing");
            ibftMiningCoordinator.stop();
          } else {
            LOG.info("Starting IBFT mining coordinator following sync");
            ibftMiningCoordinator.enable();
            ibftMiningCoordinator.start();
          }
        });

    syncState.subscribeCompletionReached(
        new IdnEvents.InitialSyncCompletionListener() {
          @Override
          public void onInitialSyncCompleted() {
            LOG.info("Starting IBFT mining coordinator following initial sync");
            ibftMiningCoordinator.enable();
            ibftMiningCoordinator.start();
          }

          @Override
          public void onInitialSyncRestart() {
            // Nothing to do. The mining coordinator won't be started until
            // sync has completed.
          }
        });

    return ibftMiningCoordinator;
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    final ValidatorProvider validatorProvider =
        protocolContext.getConsensusContext(BftContext.class).getValidatorProvider();
    return new IbftQueryPluginServiceFactory(
        blockchain, bftBlockInterface, validatorProvider, nodeKey);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return IbftProtocolScheduleBuilder.create(
        genesisConfigOptions,
        forksSchedule,
        privacyParameters,
        isRevertReasonEnabled,
        bftExtraDataCodec,
        evmConfiguration,
        miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
        metricsSystem);
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (bftBlockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }

  @Override
  protected BftContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {
    final BftConfigOptions ibftConfig = genesisConfigOptions.getBftConfigOptions();
    final EpochManager epochManager = new EpochManager(ibftConfig.getEpochLength());

    final BftValidatorOverrides validatorOverrides =
        convertIbftForks(genesisConfigOptions.getTransitions().getIbftForks());

    return new BftContext(
        BlockValidatorProvider.forkingValidatorProvider(
            blockchain, epochManager, bftBlockInterface, validatorOverrides),
        epochManager,
        bftBlockInterface);
  }

  private BftValidatorOverrides convertIbftForks(final List<BftFork> bftForks) {
    final Map<Long, List<Address>> result = new HashMap<>();

    for (final BftFork fork : bftForks) {
      fork.getValidators()
          .ifPresent(
              validators ->
                  result.put(
                      fork.getForkBlock(),
                      validators.stream()
                          .map(Address::fromHexString)
                          .collect(Collectors.toList())));
    }

    return new BftValidatorOverrides(result);
  }

  private static MinedBlockObserver blockLogger(
      final TransactionPool transactionPool, final Address localAddress) {
    return block ->
        LOG.info(
            String.format(
                "%s #%,d / %d tx / %d pending / %,d (%01.1f%%) gas / (%s)",
                block.getHeader().getCoinbase().equals(localAddress) ? "Produced" : "Imported",
                block.getHeader().getNumber(),
                block.getBody().getTransactions().size(),
                transactionPool.count(),
                block.getHeader().getGasUsed(),
                (block.getHeader().getGasUsed() * 100.0) / block.getHeader().getGasLimit(),
                block.getHash().toHexString()));
  }
}
