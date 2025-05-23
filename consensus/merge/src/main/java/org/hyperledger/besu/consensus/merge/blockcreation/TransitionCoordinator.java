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
package org.idnecology.idn.consensus.merge.blockcreation;

import org.idnecology.idn.consensus.merge.PostMergeContext;
import org.idnecology.idn.consensus.merge.TransitionUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.blockcreation.PoWMiningCoordinator;
import org.idnecology.idn.ethereum.chain.PoWObserver;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.tuweni.bytes.Bytes32;

/** The Transition coordinator. */
public class TransitionCoordinator extends TransitionUtils<MiningCoordinator>
    implements MergeMiningCoordinator {

  private final MiningCoordinator miningCoordinator;
  private final MergeMiningCoordinator mergeCoordinator;

  /**
   * Instantiates a new Transition coordinator.
   *
   * @param miningCoordinator the mining coordinator
   * @param mergeCoordinator the merge coordinator
   * @param postMergeContext the post merge context
   */
  public TransitionCoordinator(
      final MiningCoordinator miningCoordinator,
      final MiningCoordinator mergeCoordinator,
      final PostMergeContext postMergeContext) {
    super(miningCoordinator, mergeCoordinator, postMergeContext);
    this.miningCoordinator = miningCoordinator;
    this.mergeCoordinator = (MergeMiningCoordinator) mergeCoordinator;
  }

  /**
   * Gets merge coordinator.
   *
   * @return the merge coordinator
   */
  public MergeMiningCoordinator getMergeCoordinator() {
    return mergeCoordinator;
  }

  @Override
  public void start() {
    if (isMiningBeforeMerge()) {
      miningCoordinator.start();
    }
  }

  @Override
  public void stop() {
    miningCoordinator.stop();
  }

  @Override
  public void awaitStop() throws InterruptedException {
    miningCoordinator.awaitStop();
  }

  @Override
  public boolean enable() {
    return dispatchFunctionAccordingToMergeState(MiningCoordinator::enable);
  }

  @Override
  public boolean disable() {
    return dispatchFunctionAccordingToMergeState(MiningCoordinator::disable);
  }

  @Override
  public boolean isMining() {
    return dispatchFunctionAccordingToMergeState(MiningCoordinator::isMining);
  }

  @Override
  public Wei getMinTransactionGasPrice() {
    return dispatchFunctionAccordingToMergeState(MiningCoordinator::getMinTransactionGasPrice);
  }

  @Override
  public Wei getMinPriorityFeePerGas() {
    return dispatchFunctionAccordingToMergeState(MiningCoordinator::getMinPriorityFeePerGas);
  }

  @Override
  public Optional<Address> getCoinbase() {
    return dispatchFunctionAccordingToMergeState(MiningCoordinator::getCoinbase);
  }

  @Override
  public Optional<Block> createBlock(
      final BlockHeader parentHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers) {
    return dispatchFunctionAccordingToMergeState(
        (MiningCoordinator coordinator) ->
            miningCoordinator.createBlock(parentHeader, transactions, ommers));
  }

  @Override
  public Optional<Block> createBlock(final BlockHeader parentHeader, final long timestamp) {
    return dispatchFunctionAccordingToMergeState(
        (MiningCoordinator coordinator) -> coordinator.createBlock(parentHeader, timestamp));
  }

  @Override
  public void addEthHashObserver(final PoWObserver observer) {
    if (this.miningCoordinator instanceof PoWMiningCoordinator) {
      miningCoordinator.addEthHashObserver(observer);
    }
  }

  @Override
  public void changeTargetGasLimit(final Long targetGasLimit) {
    miningCoordinator.changeTargetGasLimit(targetGasLimit);
    mergeCoordinator.changeTargetGasLimit(targetGasLimit);
  }

  @Override
  public PayloadIdentifier preparePayload(
      final BlockHeader parentHeader,
      final Long timestamp,
      final Bytes32 prevRandao,
      final Address feeRecipient,
      final Optional<List<Withdrawal>> withdrawals,
      final Optional<Bytes32> parentBeaconBlockRoot) {
    return mergeCoordinator.preparePayload(
        parentHeader, timestamp, prevRandao, feeRecipient, withdrawals, parentBeaconBlockRoot);
  }

  @Override
  public BlockProcessingResult rememberBlock(final Block block) {
    return mergeCoordinator.rememberBlock(block);
  }

  @Override
  public BlockProcessingResult validateBlock(final Block block) {
    return mergeCoordinator.validateBlock(block);
  }

  @Override
  public ForkchoiceResult updateForkChoice(
      final BlockHeader newHead, final Hash finalizedBlockHash, final Hash safeBlockHash) {
    return mergeCoordinator.updateForkChoice(newHead, finalizedBlockHash, safeBlockHash);
  }

  @Override
  public Optional<Hash> getLatestValidAncestor(final Hash blockHash) {
    return mergeCoordinator.getLatestValidAncestor(blockHash);
  }

  @Override
  public Optional<Hash> getLatestValidAncestor(final BlockHeader blockHeader) {
    return mergeCoordinator.getLatestValidAncestor(blockHeader);
  }

  @Override
  public boolean isBackwardSyncing() {
    return mergeCoordinator.isBackwardSyncing();
  }

  @Override
  public CompletableFuture<Void> appendNewPayloadToSync(final Block newPayload) {
    return mergeCoordinator.appendNewPayloadToSync(newPayload);
  }

  @Override
  public Optional<BlockHeader> getOrSyncHeadByHash(final Hash headHash, final Hash finalizedHash) {
    return mergeCoordinator.getOrSyncHeadByHash(headHash, finalizedHash);
  }

  @Override
  public boolean isMiningBeforeMerge() {
    return mergeCoordinator.isMiningBeforeMerge();
  }

  @Override
  public boolean isDescendantOf(final BlockHeader ancestorBlock, final BlockHeader newBlock) {
    return mergeCoordinator.isDescendantOf(ancestorBlock, newBlock);
  }

  @Override
  public boolean isBadBlock(final Hash blockHash) {
    return mergeCoordinator.isBadBlock(blockHash);
  }

  @Override
  public Optional<Hash> getLatestValidHashOfBadBlock(final Hash blockHash) {
    return mergeCoordinator.getLatestValidHashOfBadBlock(blockHash);
  }

  @Override
  public void finalizeProposalById(final PayloadIdentifier payloadId) {
    mergeCoordinator.finalizeProposalById(payloadId);
  }

  /**
   * returns the instance of ethScheduler
   *
   * @return get the Eth scheduler
   */
  @Override
  public EthScheduler getEthScheduler() {
    return mergeCoordinator.getEthScheduler();
  }
}
