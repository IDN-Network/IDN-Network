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

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.AbstractBlockCreator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.SealableBlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;

/** The Merge block creator. */
class MergeBlockCreator extends AbstractBlockCreator {

  /**
   * Instantiates a new Merge block creator.
   *
   * @param miningConfiguration the mining parameters
   * @param extraDataCalculator the extra data calculator
   * @param transactionPool the pending transactions
   * @param protocolContext the protocol context
   * @param protocolSchedule the protocol schedule
   * @param parentHeader the parent header
   */
  public MergeBlockCreator(
      final MiningConfiguration miningConfiguration,
      final ExtraDataCalculator extraDataCalculator,
      final TransactionPool transactionPool,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final BlockHeader parentHeader,
      final EthScheduler ethScheduler) {
    super(
        miningConfiguration,
        __ -> miningConfiguration.getCoinbase().orElseThrow(),
        extraDataCalculator,
        transactionPool,
        protocolContext,
        protocolSchedule,
        ethScheduler);
  }

  /**
   * Create block and return block creation result.
   *
   * @param maybeTransactions the maybe transactions
   * @param random the random
   * @param timestamp the timestamp
   * @param withdrawals optional list of withdrawals
   * @param parentBeaconBlockRoot optional root hash of the parent beacon block
   * @return the block creation result
   */
  public BlockCreationResult createBlock(
      final Optional<List<Transaction>> maybeTransactions,
      final Bytes32 random,
      final long timestamp,
      final Optional<List<Withdrawal>> withdrawals,
      final Optional<Bytes32> parentBeaconBlockRoot,
      final BlockHeader parentHeader) {

    return createBlock(
        maybeTransactions,
        Optional.of(Collections.emptyList()),
        withdrawals,
        Optional.of(random),
        parentBeaconBlockRoot,
        timestamp,
        false,
        parentHeader);
  }

  @Override
  public BlockCreationResult createBlock(
      final Optional<List<Transaction>> maybeTransactions,
      final Optional<List<BlockHeader>> maybeOmmers,
      final long timestamp,
      final BlockHeader parentHeader) {
    throw new UnsupportedOperationException("random is required");
  }

  @Override
  protected BlockHeader createFinalBlockHeader(final SealableBlockHeader sealableBlockHeader) {
    return BlockHeaderBuilder.create()
        .difficulty(Difficulty.ZERO)
        .populateFrom(sealableBlockHeader)
        .nonce(0L)
        .blockHeaderFunctions(blockHeaderFunctions)
        .buildBlockHeader();
  }
}
