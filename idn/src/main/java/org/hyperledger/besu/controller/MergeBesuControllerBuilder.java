/*
 * Copyright contributors to Hyperledger Idn.
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

import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.consensus.merge.MergeProtocolSchedule;
import org.idnecology.idn.consensus.merge.PostMergeContext;
import org.idnecology.idn.consensus.merge.TransitionBestPeerComparator;
import org.idnecology.idn.consensus.merge.blockcreation.MergeCoordinator;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthMessages;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.MergePeerFilter;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.peervalidation.PeerValidator;
import org.idnecology.idn.ethereum.eth.peervalidation.RequiredBlocksPeerValidator;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.backwardsync.BackwardChain;
import org.idnecology.idn.ethereum.eth.sync.backwardsync.BackwardSyncContext;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.forkid.ForkIdManager;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Merge idn controller builder. */
public class MergeIdnControllerBuilder extends IdnControllerBuilder {
  private final AtomicReference<SyncState> syncState = new AtomicReference<>();
  private static final Logger LOG = LoggerFactory.getLogger(MergeIdnControllerBuilder.class);
  private final PostMergeContext postMergeContext = new PostMergeContext();

  /** Default constructor. */
  public MergeIdnControllerBuilder() {}

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    return createTransitionMiningCoordinator(
        protocolSchedule,
        protocolContext,
        transactionPool,
        miningConfiguration,
        syncState,
        new BackwardSyncContext(
            protocolContext,
            protocolSchedule,
            syncConfig,
            metricsSystem,
            ethProtocolManager.ethContext(),
            syncState,
            BackwardChain.from(
                storageProvider, ScheduleBasedBlockHeaderFunctions.create(protocolSchedule))),
        ethProtocolManager.ethContext().getScheduler());
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

    var mergeContext = protocolContext.getConsensusContext(MergeContext.class);

    var mergeBestPeerComparator =
        new TransitionBestPeerComparator(
            genesisConfigOptions.getTerminalTotalDifficulty().map(Difficulty::of).orElseThrow());
    ethPeers.setBestPeerComparator(mergeBestPeerComparator);
    mergeContext.observeNewIsPostMergeState(mergeBestPeerComparator);

    Optional<MergePeerFilter> filterToUse = Optional.of(new MergePeerFilter());

    if (mergePeerFilter.isPresent()) {
      filterToUse = mergePeerFilter;
    }
    mergeContext.observeNewIsPostMergeState(filterToUse.get());
    mergeContext.addNewUnverifiedForkchoiceListener(filterToUse.get());

    EthProtocolManager ethProtocolManager =
        super.createEthProtocolManager(
            protocolContext,
            synchronizerConfiguration,
            transactionPool,
            ethereumWireProtocolConfiguration,
            ethPeers,
            ethContext,
            ethMessages,
            scheduler,
            peerValidators,
            filterToUse,
            forkIdManager);

    return ethProtocolManager;
  }

  /**
   * Create transition mining coordinator.
   *
   * @param protocolSchedule the protocol schedule
   * @param protocolContext the protocol context
   * @param transactionPool the transaction pool
   * @param miningConfiguration the mining parameters
   * @param syncState the sync state
   * @param backwardSyncContext the backward sync context
   * @param ethScheduler the scheduler
   * @return the mining coordinator
   */
  protected MiningCoordinator createTransitionMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final SyncState syncState,
      final BackwardSyncContext backwardSyncContext,
      final EthScheduler ethScheduler) {

    this.syncState.set(syncState);

    final Optional<Address> depositContractAddress =
        genesisConfigOptions.getDepositContractAddress();

    return new MergeCoordinator(
        protocolContext,
        protocolSchedule,
        ethScheduler,
        transactionPool,
        miningConfiguration,
        backwardSyncContext,
        depositContractAddress);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return MergeProtocolSchedule.create(
        genesisConfigOptions,
        privacyParameters,
        isRevertReasonEnabled,
        miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
        metricsSystem);
  }

  @Override
  protected MergeContext createConsensusContext(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule) {

    final OptionalLong terminalBlockNumber = genesisConfigOptions.getTerminalBlockNumber();
    final Optional<Hash> terminalBlockHash = genesisConfigOptions.getTerminalBlockHash();
    final boolean isPostMergeAtGenesis =
        genesisConfigOptions.getTerminalTotalDifficulty().isPresent()
            && genesisConfigOptions.getTerminalTotalDifficulty().get().isZero()
            && blockchain.getGenesisBlockHeader().getDifficulty().isZero();

    final MergeContext mergeContext =
        postMergeContext
            .setSyncState(syncState.get())
            .setTerminalTotalDifficulty(
                genesisConfigOptions
                    .getTerminalTotalDifficulty()
                    .map(Difficulty::of)
                    .orElse(Difficulty.ZERO))
            .setPostMergeAtGenesis(isPostMergeAtGenesis);

    blockchain
        .getFinalized()
        .flatMap(blockchain::getBlockHeader)
        .ifPresent(mergeContext::setFinalized);

    blockchain
        .getSafeBlock()
        .flatMap(blockchain::getBlockHeader)
        .ifPresent(mergeContext::setSafeBlock);

    if (terminalBlockNumber.isPresent() && terminalBlockHash.isPresent()) {
      Optional<BlockHeader> termBlock = blockchain.getBlockHeader(terminalBlockNumber.getAsLong());
      mergeContext.setTerminalPoWBlock(termBlock);
    }
    blockchain.observeBlockAdded(
        blockAddedEvent ->
            blockchain
                .getTotalDifficultyByHash(blockAddedEvent.getBlock().getHeader().getHash())
                .ifPresent(mergeContext::setIsPostMerge));

    return mergeContext;
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(
      final Blockchain blockchain, final ProtocolContext protocolContext) {
    return new NoopPluginServiceFactory();
  }

  @Override
  protected List<PeerValidator> createPeerValidators(
      final ProtocolSchedule protocolSchedule, final PeerTaskExecutor peerTaskExecutor) {
    List<PeerValidator> retval = super.createPeerValidators(protocolSchedule, peerTaskExecutor);
    final OptionalLong powTerminalBlockNumber = genesisConfigOptions.getTerminalBlockNumber();
    final Optional<Hash> powTerminalBlockHash = genesisConfigOptions.getTerminalBlockHash();
    if (powTerminalBlockHash.isPresent() && powTerminalBlockNumber.isPresent()) {
      retval.add(
          new RequiredBlocksPeerValidator(
              protocolSchedule,
              peerTaskExecutor,
              syncConfig,
              metricsSystem,
              powTerminalBlockNumber.getAsLong(),
              powTerminalBlockHash.get(),
              0));
    } else {
      LOG.debug("unable to validate peers with terminal difficulty blocks");
    }
    return retval;
  }

  @Override
  public IdnController build() {
    final IdnController controller = super.build();
    postMergeContext.setSyncState(syncState.get());
    return controller;
  }

  /**
   * Gets post merge context.
   *
   * @return the post merge context
   */
  public PostMergeContext getPostMergeContext() {
    return postMergeContext;
  }
}
