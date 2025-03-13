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
package org.idnecology.idn.ethereum.mainnet.parallelization;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.mainnet.AbstractBlockProcessor.PreprocessingFunction.NoPreprocessing;
import org.idnecology.idn.ethereum.mainnet.BlockProcessor;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockProcessor;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.MiningBeneficiaryCalculator;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecBuilder;
import org.idnecology.idn.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.ethereum.trie.diffbased.common.provider.DiffBasedWorldStateProvider;
import org.idnecology.idn.evm.blockhash.BlockHashLookup;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainnetParallelBlockProcessor extends MainnetBlockProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(MainnetParallelBlockProcessor.class);

  private final Optional<MetricsSystem> metricsSystem;
  private final Optional<Counter> confirmedParallelizedTransactionCounter;
  private final Optional<Counter> conflictingButCachedTransactionCounter;

  public MainnetParallelBlockProcessor(
      final MainnetTransactionProcessor transactionProcessor,
      final TransactionReceiptFactory transactionReceiptFactory,
      final Wei blockReward,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final boolean skipZeroBlockRewards,
      final ProtocolSchedule protocolSchedule,
      final MetricsSystem metricsSystem) {
    super(
        transactionProcessor,
        transactionReceiptFactory,
        blockReward,
        miningBeneficiaryCalculator,
        skipZeroBlockRewards,
        protocolSchedule);
    this.metricsSystem = Optional.of(metricsSystem);
    this.confirmedParallelizedTransactionCounter =
        Optional.of(
            this.metricsSystem
                .get()
                .createCounter(
                    IdnMetricCategory.BLOCK_PROCESSING,
                    "parallelized_transactions_counter",
                    "Counter for the number of parallelized transactions during block processing"));

    this.conflictingButCachedTransactionCounter =
        Optional.of(
            this.metricsSystem
                .get()
                .createCounter(
                    IdnMetricCategory.BLOCK_PROCESSING,
                    "conflicted_transactions_counter",
                    "Counter for the number of conflicted transactions during block processing"));
  }

  @Override
  protected TransactionProcessingResult getTransactionProcessingResult(
      final Optional<PreprocessingContext> preProcessingContext,
      final MutableWorldState worldState,
      final WorldUpdater blockUpdater,
      final PrivateMetadataUpdater privateMetadataUpdater,
      final BlockHeader blockHeader,
      final Wei blobGasPrice,
      final Address miningBeneficiary,
      final Transaction transaction,
      final int location,
      final BlockHashLookup blockHashLookup) {

    TransactionProcessingResult transactionProcessingResult = null;

    if (preProcessingContext.isPresent()) {
      final ParallelizedPreProcessingContext parallelizedPreProcessingContext =
          (ParallelizedPreProcessingContext) preProcessingContext.get();
      transactionProcessingResult =
          parallelizedPreProcessingContext
              .parallelizedConcurrentTransactionProcessor()
              .applyParallelizedTransactionResult(
                  worldState,
                  miningBeneficiary,
                  transaction,
                  location,
                  confirmedParallelizedTransactionCounter,
                  conflictingButCachedTransactionCounter)
              .orElse(null);
    }

    if (transactionProcessingResult == null) {
      return super.getTransactionProcessingResult(
          preProcessingContext,
          worldState,
          blockUpdater,
          privateMetadataUpdater,
          blockHeader,
          blobGasPrice,
          miningBeneficiary,
          transaction,
          location,
          blockHashLookup);
    } else {
      return transactionProcessingResult;
    }
  }

  @Override
  public BlockProcessingResult processBlock(
      final ProtocolContext protocolContext,
      final Blockchain blockchain,
      final MutableWorldState worldState,
      final BlockHeader blockHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers,
      final Optional<List<Withdrawal>> maybeWithdrawals,
      final PrivateMetadataUpdater privateMetadataUpdater) {
    final BlockProcessingResult blockProcessingResult =
        super.processBlock(
            protocolContext,
            blockchain,
            worldState,
            blockHeader,
            transactions,
            ommers,
            maybeWithdrawals,
            privateMetadataUpdater,
            new ParallelTransactionPreprocessing());

    if (blockProcessingResult.isFailed()) {
      // Fallback to non-parallel processing if there is a block processing exception .
      LOG.info(
          "Parallel transaction processing failure. Falling back to non-parallel processing for block #{} ({})",
          blockHeader.getNumber(),
          blockHeader.getBlockHash());
      return super.processBlock(
          protocolContext,
          blockchain,
          worldState,
          blockHeader,
          transactions,
          ommers,
          maybeWithdrawals,
          privateMetadataUpdater,
          new NoPreprocessing());
    }
    return blockProcessingResult;
  }

  record ParallelizedPreProcessingContext(
      ParallelizedConcurrentTransactionProcessor parallelizedConcurrentTransactionProcessor)
      implements PreprocessingContext {}

  public static class ParallelBlockProcessorBuilder
      implements ProtocolSpecBuilder.BlockProcessorBuilder {

    final MetricsSystem metricsSystem;

    public ParallelBlockProcessorBuilder(final MetricsSystem metricsSystem) {
      this.metricsSystem = metricsSystem;
    }

    @Override
    public BlockProcessor apply(
        final MainnetTransactionProcessor transactionProcessor,
        final TransactionReceiptFactory transactionReceiptFactory,
        final Wei blockReward,
        final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
        final boolean skipZeroBlockRewards,
        final ProtocolSchedule protocolSchedule) {
      return new MainnetParallelBlockProcessor(
          transactionProcessor,
          transactionReceiptFactory,
          blockReward,
          miningBeneficiaryCalculator,
          skipZeroBlockRewards,
          protocolSchedule,
          metricsSystem);
    }
  }

  class ParallelTransactionPreprocessing implements PreprocessingFunction {

    @Override
    public Optional<PreprocessingContext> run(
        final ProtocolContext protocolContext,
        final PrivateMetadataUpdater privateMetadataUpdater,
        final BlockHeader blockHeader,
        final List<Transaction> transactions,
        final Address miningBeneficiary,
        final BlockHashLookup blockHashLookup,
        final Wei blobGasPrice) {
      if ((protocolContext.getWorldStateArchive() instanceof DiffBasedWorldStateProvider)) {
        ParallelizedConcurrentTransactionProcessor parallelizedConcurrentTransactionProcessor =
            new ParallelizedConcurrentTransactionProcessor(transactionProcessor);
        // runAsyncBlock, if activated, facilitates the  non-blocking parallel execution of
        // transactions in the background through an optimistic strategy.
        parallelizedConcurrentTransactionProcessor.runAsyncBlock(
            protocolContext,
            blockHeader,
            transactions,
            miningBeneficiary,
            blockHashLookup,
            blobGasPrice,
            privateMetadataUpdater);
        return Optional.of(
            new ParallelizedPreProcessingContext(parallelizedConcurrentTransactionProcessor));
      }
      return Optional.empty();
    }
  }
}
