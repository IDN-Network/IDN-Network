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
package org.idnecology.idn.ethereum.blockcreation;

import static org.idnecology.idn.ethereum.core.BlockHeaderBuilder.createPending;
import static org.idnecology.idn.ethereum.mainnet.feemarket.ExcessBlobGasCalculator.calculateExcessBlobGasForParent;
import static org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams.withBlockHeaderAndNoUpdateNodeHead;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.BlobGas;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.txselection.BlockTransactionSelector;
import org.idnecology.idn.ethereum.blockcreation.txselection.TransactionSelectionResults;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.core.Request;
import org.idnecology.idn.ethereum.core.SealableBlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.AbstractBlockProcessor;
import org.idnecology.idn.ethereum.mainnet.BodyValidation;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.WithdrawalsProcessor;
import org.idnecology.idn.ethereum.mainnet.feemarket.ExcessBlobGasCalculator;
import org.idnecology.idn.ethereum.mainnet.requests.RequestProcessingContext;
import org.idnecology.idn.ethereum.mainnet.requests.RequestProcessorCoordinator;
import org.idnecology.idn.ethereum.mainnet.systemcall.BlockProcessingContext;
import org.idnecology.idn.evm.account.MutableAccount;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.plugin.services.exception.StorageException;
import org.idnecology.idn.plugin.services.securitymodule.SecurityModuleException;
import org.idnecology.idn.plugin.services.txselection.PluginTransactionSelector;
import org.idnecology.idn.plugin.services.txselection.SelectorsStateManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBlockCreator implements AsyncBlockCreator {

  public interface ExtraDataCalculator {

    Bytes get(final BlockHeader parent);
  }

  private static final Logger LOG = LoggerFactory.getLogger(AbstractBlockCreator.class);

  private final MiningBeneficiaryCalculator miningBeneficiaryCalculator;
  private final ExtraDataCalculator extraDataCalculator;
  private final TransactionPool transactionPool;
  protected final MiningConfiguration miningConfiguration;
  protected final ProtocolContext protocolContext;
  protected final ProtocolSchedule protocolSchedule;
  protected final BlockHeaderFunctions blockHeaderFunctions;
  private final EthScheduler ethScheduler;
  private final AtomicBoolean isCancelled = new AtomicBoolean(false);

  protected AbstractBlockCreator(
      final MiningConfiguration miningConfiguration,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final ExtraDataCalculator extraDataCalculator,
      final TransactionPool transactionPool,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final EthScheduler ethScheduler) {
    this.miningConfiguration = miningConfiguration;
    this.miningBeneficiaryCalculator = miningBeneficiaryCalculator;
    this.extraDataCalculator = extraDataCalculator;
    this.transactionPool = transactionPool;
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.ethScheduler = ethScheduler;
    blockHeaderFunctions = ScheduleBasedBlockHeaderFunctions.create(protocolSchedule);
  }

  /**
   * Create block will create a new block at the head of the blockchain specified in the
   * protocolContext.
   *
   * <p>It will select transactions from the PendingTransaction list for inclusion in the Block
   * body, and will supply an empty Ommers list.
   *
   * <p>Once transactions have been selected and applied to a disposable/temporary world state, the
   * block reward is paid to the relevant coinbase, and a sealable header is constructed.
   *
   * <p>The sealableHeader is then provided to child instances for sealing (i.e. proof of work or
   * otherwise).
   *
   * <p>The constructed block is then returned.
   *
   * @return a block with appropriately selected transactions, seals and ommers.
   */
  @Override
  public BlockCreationResult createBlock(final long timestamp, final BlockHeader parentHeader) {
    return createBlock(Optional.empty(), Optional.empty(), timestamp, parentHeader);
  }

  @Override
  public BlockCreationResult createBlock(
      final List<Transaction> transactions,
      final List<BlockHeader> ommers,
      final long timestamp,
      final BlockHeader parentHeader) {
    return createBlock(Optional.of(transactions), Optional.of(ommers), timestamp, parentHeader);
  }

  @Override
  public BlockCreationResult createBlock(
      final Optional<List<Transaction>> maybeTransactions,
      final Optional<List<BlockHeader>> maybeOmmers,
      final long timestamp,
      final BlockHeader parentHeader) {
    return createBlock(
        maybeTransactions,
        maybeOmmers,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        timestamp,
        true,
        parentHeader);
  }

  @Override
  public BlockCreationResult createEmptyWithdrawalsBlock(
      final long timestamp, final BlockHeader parentHeader) {
    throw new UnsupportedOperationException("Only used by BFT block creators");
  }

  public BlockCreationResult createBlock(
      final Optional<List<Transaction>> maybeTransactions,
      final Optional<List<BlockHeader>> maybeOmmers,
      final Optional<List<Withdrawal>> maybeWithdrawals,
      final long timestamp,
      final BlockHeader parentHeader) {
    return createBlock(
        maybeTransactions,
        maybeOmmers,
        maybeWithdrawals,
        Optional.empty(),
        Optional.empty(),
        timestamp,
        true,
        parentHeader);
  }

  protected BlockCreationResult createBlock(
      final Optional<List<Transaction>> maybeTransactions,
      final Optional<List<BlockHeader>> maybeOmmers,
      final Optional<List<Withdrawal>> maybeWithdrawals,
      final Optional<Bytes32> maybePrevRandao,
      final Optional<Bytes32> maybeParentBeaconBlockRoot,
      final long timestamp,
      boolean rewardCoinbase,
      final BlockHeader parentHeader) {

    final var timings = new BlockCreationTiming();

    try (final MutableWorldState disposableWorldState = duplicateWorldStateAtParent(parentHeader)) {
      timings.register("duplicateWorldState");
      final ProtocolSpec newProtocolSpec =
          protocolSchedule.getForNextBlockHeader(parentHeader, timestamp);

      final ProcessableBlockHeader processableBlockHeader =
          createPending(
                  newProtocolSpec,
                  parentHeader,
                  miningConfiguration,
                  timestamp,
                  maybePrevRandao,
                  maybeParentBeaconBlockRoot)
              .buildProcessableBlockHeader();

      final Address miningBeneficiary =
          miningBeneficiaryCalculator.getMiningBeneficiary(processableBlockHeader.getNumber());

      throwIfStopped();

      final List<BlockHeader> ommers = maybeOmmers.orElse(selectOmmers());

      throwIfStopped();

      final var selectorsStateManager = new SelectorsStateManager();
      final var pluginTransactionSelector =
          miningConfiguration
              .getTransactionSelectionService()
              .createPluginTransactionSelector(selectorsStateManager);
      final var operationTracer = pluginTransactionSelector.getOperationTracer();
      pluginTransactionSelector
          .getOperationTracer()
          .traceStartBlock(processableBlockHeader, miningBeneficiary);

      operationTracer.traceStartBlock(processableBlockHeader, miningBeneficiary);
      BlockProcessingContext blockProcessingContext =
          new BlockProcessingContext(
              processableBlockHeader,
              disposableWorldState,
              newProtocolSpec,
              newProtocolSpec
                  .getBlockHashProcessor()
                  .createBlockHashLookup(protocolContext.getBlockchain(), processableBlockHeader),
              operationTracer);
      newProtocolSpec.getBlockHashProcessor().process(blockProcessingContext);

      timings.register("preTxsSelection");
      final TransactionSelectionResults transactionResults =
          selectTransactions(
              processableBlockHeader,
              disposableWorldState,
              maybeTransactions,
              miningBeneficiary,
              newProtocolSpec,
              pluginTransactionSelector,
              selectorsStateManager,
              parentHeader);
      transactionResults.logSelectionStats();
      timings.register("txsSelection");
      throwIfStopped();

      final Optional<WithdrawalsProcessor> maybeWithdrawalsProcessor =
          newProtocolSpec.getWithdrawalsProcessor();
      final boolean withdrawalsCanBeProcessed =
          maybeWithdrawalsProcessor.isPresent() && maybeWithdrawals.isPresent();
      if (withdrawalsCanBeProcessed) {
        maybeWithdrawalsProcessor
            .get()
            .processWithdrawals(maybeWithdrawals.get(), disposableWorldState.updater());
      }

      throwIfStopped();

      // EIP-7685: process EL requests
      final Optional<RequestProcessorCoordinator> requestProcessor =
          newProtocolSpec.getRequestProcessorCoordinator();
      RequestProcessingContext requestProcessingContext =
          new RequestProcessingContext(blockProcessingContext, transactionResults.getReceipts());

      Optional<List<Request>> maybeRequests =
          requestProcessor.map(processor -> processor.process(requestProcessingContext));

      throwIfStopped();

      if (rewardCoinbase
          && !rewardBeneficiary(
              disposableWorldState,
              processableBlockHeader,
              ommers,
              miningBeneficiary,
              newProtocolSpec.getBlockReward(),
              newProtocolSpec.isSkipZeroBlockRewards(),
              newProtocolSpec)) {
        LOG.trace("Failed to apply mining reward, exiting.");
        throw new RuntimeException("Failed to apply mining reward.");
      }

      throwIfStopped();

      final GasUsage usage =
          computeExcessBlobGas(transactionResults, newProtocolSpec, parentHeader);

      throwIfStopped();

      BlockHeaderBuilder builder =
          BlockHeaderBuilder.create()
              .populateFrom(processableBlockHeader)
              .ommersHash(BodyValidation.ommersHash(ommers))
              .stateRoot(disposableWorldState.rootHash())
              .transactionsRoot(
                  BodyValidation.transactionsRoot(transactionResults.getSelectedTransactions()))
              .receiptsRoot(BodyValidation.receiptsRoot(transactionResults.getReceipts()))
              .logsBloom(BodyValidation.logsBloom(transactionResults.getReceipts()))
              .gasUsed(transactionResults.getCumulativeGasUsed())
              .extraData(extraDataCalculator.get(parentHeader))
              .withdrawalsRoot(
                  withdrawalsCanBeProcessed
                      ? BodyValidation.withdrawalsRoot(maybeWithdrawals.get())
                      : null)
              .requestsHash(maybeRequests.map(BodyValidation::requestsHash).orElse(null));
      if (usage != null) {
        builder.blobGasUsed(usage.used.toLong()).excessBlobGas(usage.excessBlobGas);
      }

      final SealableBlockHeader sealableBlockHeader = builder.buildSealableBlockHeader();

      final BlockHeader blockHeader = createFinalBlockHeader(sealableBlockHeader);

      final Optional<List<Withdrawal>> withdrawals =
          withdrawalsCanBeProcessed ? maybeWithdrawals : Optional.empty();
      final BlockBody blockBody =
          new BlockBody(transactionResults.getSelectedTransactions(), ommers, withdrawals);
      final Block block = new Block(blockHeader, blockBody);

      operationTracer.traceEndBlock(blockHeader, blockBody);
      timings.register("blockAssembled");
      return new BlockCreationResult(block, transactionResults, timings);
    } catch (final SecurityModuleException ex) {
      throw new IllegalStateException("Failed to create block signature", ex);
    } catch (final CancellationException | StorageException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new IllegalStateException(
          "Block creation failed unexpectedly. Will restart on next block added to chain.", ex);
    }
  }

  record GasUsage(BlobGas excessBlobGas, BlobGas used) {}

  private GasUsage computeExcessBlobGas(
      final TransactionSelectionResults transactionResults,
      final ProtocolSpec newProtocolSpec,
      final BlockHeader parentHeader) {

    if (newProtocolSpec.getFeeMarket().implementsDataFee()) {
      final var gasCalculator = newProtocolSpec.getGasCalculator();
      final int newBlobsCount =
          transactionResults.getTransactionsByType(TransactionType.BLOB).stream()
              .map(tx -> tx.getVersionedHashes().orElseThrow())
              .mapToInt(List::size)
              .sum();
      // casting parent excess blob gas to long since for the moment it should be well below that
      // limit
      BlobGas excessBlobGas =
          ExcessBlobGasCalculator.calculateExcessBlobGasForParent(newProtocolSpec, parentHeader);
      BlobGas used = BlobGas.of(gasCalculator.blobGasCost(newBlobsCount));
      return new GasUsage(excessBlobGas, used);
    }
    return null;
  }

  private TransactionSelectionResults selectTransactions(
      final ProcessableBlockHeader processableBlockHeader,
      final MutableWorldState disposableWorldState,
      final Optional<List<Transaction>> transactions,
      final Address miningBeneficiary,
      final ProtocolSpec protocolSpec,
      final PluginTransactionSelector pluginTransactionSelector,
      final SelectorsStateManager selectorsStateManager,
      final BlockHeader parentHeader)
      throws RuntimeException {
    final MainnetTransactionProcessor transactionProcessor = protocolSpec.getTransactionProcessor();

    final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory =
        protocolSpec.getTransactionReceiptFactory();

    Wei blobGasPrice =
        protocolSpec
            .getFeeMarket()
            .blobGasPricePerGas(calculateExcessBlobGasForParent(protocolSpec, parentHeader));

    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            miningConfiguration,
            transactionProcessor,
            protocolContext.getBlockchain(),
            disposableWorldState,
            transactionPool,
            processableBlockHeader,
            transactionReceiptFactory,
            isCancelled::get,
            miningBeneficiary,
            blobGasPrice,
            protocolSpec.getFeeMarket(),
            protocolSpec.getGasCalculator(),
            protocolSpec.getGasLimitCalculator(),
            protocolSpec.getBlockHashProcessor(),
            pluginTransactionSelector,
            ethScheduler,
            selectorsStateManager);

    if (transactions.isPresent()) {
      return selector.evaluateTransactions(transactions.get());
    } else {
      return selector.buildTransactionListForBlock();
    }
  }

  private MutableWorldState duplicateWorldStateAtParent(final BlockHeader parentHeader) {
    final Hash parentStateRoot = parentHeader.getStateRoot();
    return protocolContext
        .getWorldStateArchive()
        .getWorldState(withBlockHeaderAndNoUpdateNodeHead(parentHeader))
        .orElseThrow(
            () -> {
              LOG.info("Unable to create block because world state is not available");
              return new CancellationException(
                  "World state not available for block "
                      + parentHeader.getNumber()
                      + " with state root "
                      + parentStateRoot);
            });
  }

  private List<BlockHeader> selectOmmers() {
    return Lists.newArrayList();
  }

  @Override
  public void cancel() {
    isCancelled.set(true);
  }

  @Override
  public boolean isCancelled() {
    return isCancelled.get();
  }

  private void throwIfStopped() throws CancellationException {
    if (isCancelled.get()) {
      throw new CancellationException();
    }
  }

  /* Copied from BlockProcessor (with modifications). */
  boolean rewardBeneficiary(
      final MutableWorldState worldState,
      final ProcessableBlockHeader header,
      final List<BlockHeader> ommers,
      final Address miningBeneficiary,
      final Wei blockReward,
      final boolean skipZeroBlockRewards,
      final ProtocolSpec protocolSpec) {

    // TODO(tmm): Added to make this work, should come from blockProcessor.
    final int MAX_GENERATION = 6;
    if (skipZeroBlockRewards && blockReward.isZero()) {
      return true;
    }

    final Wei coinbaseReward =
        protocolSpec
            .getBlockProcessor()
            .getCoinbaseReward(blockReward, header.getNumber(), ommers.size());
    final WorldUpdater updater = worldState.updater();
    final MutableAccount beneficiary = updater.getOrCreate(miningBeneficiary);

    beneficiary.incrementBalance(coinbaseReward);
    for (final BlockHeader ommerHeader : ommers) {
      if (ommerHeader.getNumber() - header.getNumber() > MAX_GENERATION) {
        LOG.trace(
            "Block processing error: ommer block number {} more than {} generations current block number {}",
            ommerHeader.getNumber(),
            MAX_GENERATION,
            header.getNumber());
        return false;
      }

      final MutableAccount ommerCoinbase = updater.getOrCreate(ommerHeader.getCoinbase());
      final Wei ommerReward =
          protocolSpec
              .getBlockProcessor()
              .getOmmerReward(blockReward, header.getNumber(), ommerHeader.getNumber());
      ommerCoinbase.incrementBalance(ommerReward);
    }

    updater.commit();

    return true;
  }

  protected abstract BlockHeader createFinalBlockHeader(
      final SealableBlockHeader sealableBlockHeader);

  @FunctionalInterface
  protected interface MiningBeneficiaryCalculator {
    Address getMiningBeneficiary(long blockNumber);
  }
}
