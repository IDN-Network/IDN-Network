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
package org.idnecology.idn.ethereum.privacy.storage.migration;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.BlockProcessingOutputs;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.mainnet.AbstractBlockProcessor;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.MiningBeneficiaryCalculator;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.evm.account.MutableAccount;
import org.idnecology.idn.evm.blockhash.BlockHashLookup;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivateMigrationBlockProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(PrivateMigrationBlockProcessor.class);

  static final int MAX_GENERATION = 6;

  private final MainnetTransactionProcessor transactionProcessor;
  private final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory;
  final Wei blockReward;
  private final boolean skipZeroBlockRewards;
  private final MiningBeneficiaryCalculator miningBeneficiaryCalculator;

  public PrivateMigrationBlockProcessor(
      final MainnetTransactionProcessor transactionProcessor,
      final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory,
      final Wei blockReward,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final boolean skipZeroBlockRewards) {
    this.transactionProcessor = transactionProcessor;
    this.transactionReceiptFactory = transactionReceiptFactory;
    this.blockReward = blockReward;
    this.miningBeneficiaryCalculator = miningBeneficiaryCalculator;
    this.skipZeroBlockRewards = skipZeroBlockRewards;
  }

  public PrivateMigrationBlockProcessor(final ProtocolSpec protocolSpec) {
    this(
        protocolSpec.getTransactionProcessor(),
        protocolSpec.getTransactionReceiptFactory(),
        protocolSpec.getBlockReward(),
        protocolSpec.getMiningBeneficiaryCalculator(),
        protocolSpec.isSkipZeroBlockRewards());
  }

  public BlockProcessingResult processBlock(
      final Blockchain blockchain,
      final MutableWorldState worldState,
      final BlockHashLookup blockHashLookup,
      final BlockHeader blockHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers) {
    long gasUsed = 0;
    final List<TransactionReceipt> receipts = new ArrayList<>();

    for (final Transaction transaction : transactions) {
      final long remainingGasBudget = blockHeader.getGasLimit() - gasUsed;
      if (Long.compareUnsigned(transaction.getGasLimit(), remainingGasBudget) > 0) {
        LOG.warn(
            "Transaction processing error: transaction gas limit {} exceeds available block budget"
                + " remaining {}",
            transaction.getGasLimit(),
            remainingGasBudget);
        return BlockProcessingResult.FAILED;
      }

      final WorldUpdater worldStateUpdater = worldState.updater();
      final Address miningBeneficiary =
          miningBeneficiaryCalculator.calculateBeneficiary(blockHeader);

      final TransactionProcessingResult result =
          transactionProcessor.processTransaction(
              worldStateUpdater,
              blockHeader,
              transaction,
              miningBeneficiary,
              blockHashLookup,
              true,
              TransactionValidationParams.processingBlock(),
              Wei.ZERO);
      if (result.isInvalid()) {
        return BlockProcessingResult.FAILED;
      }

      worldStateUpdater.commit();
      gasUsed = transaction.getGasLimit() - result.getGasRemaining() + gasUsed;
      final TransactionReceipt transactionReceipt =
          transactionReceiptFactory.create(transaction.getType(), result, worldState, gasUsed);
      receipts.add(transactionReceipt);
    }

    if (!rewardCoinbase(worldState, blockHeader, ommers, skipZeroBlockRewards)) {
      return BlockProcessingResult.FAILED;
    }
    BlockProcessingOutputs blockProcessingOutput = new BlockProcessingOutputs(worldState, receipts);
    return new BlockProcessingResult(Optional.of(blockProcessingOutput));
  }

  private boolean rewardCoinbase(
      final MutableWorldState worldState,
      final ProcessableBlockHeader header,
      final List<BlockHeader> ommers,
      final boolean skipZeroBlockRewards) {
    if (skipZeroBlockRewards && blockReward.isZero()) {
      return true;
    }

    final Wei coinbaseReward = blockReward.add(blockReward.multiply(ommers.size()).divide(32));
    final WorldUpdater updater = worldState.updater();
    final MutableAccount coinbase = updater.getOrCreate(header.getCoinbase());

    coinbase.incrementBalance(coinbaseReward);
    for (final BlockHeader ommerHeader : ommers) {
      if (ommerHeader.getNumber() - header.getNumber() > MAX_GENERATION) {
        LOG.warn(
            "Block processing error: ommer block number {} more than {} generations current block"
                + " number {}",
            ommerHeader.getNumber(),
            MAX_GENERATION,
            header.getNumber());
        return false;
      }

      final MutableAccount ommerCoinbase = updater.getOrCreate(ommerHeader.getCoinbase());
      final long distance = header.getNumber() - ommerHeader.getNumber();
      final Wei ommerReward = blockReward.subtract(blockReward.multiply(distance).divide(8));
      ommerCoinbase.incrementBalance(ommerReward);
    }

    return true;
  }
}
