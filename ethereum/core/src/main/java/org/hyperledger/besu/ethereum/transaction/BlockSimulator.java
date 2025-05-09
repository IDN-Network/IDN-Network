/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.ethereum.transaction;

import static org.idnecology.idn.ethereum.transaction.BlockStateCalls.fillBlockStateCalls;
import static org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams.withBlockHeaderAndNoUpdateNodeHead;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.StateOverride;
import org.idnecology.idn.datatypes.StateOverrideMap;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.ParsedExtraData;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.mainnet.BodyValidation;
import org.idnecology.idn.ethereum.mainnet.ImmutableTransactionValidationParams;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.MiningBeneficiaryCalculator;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.account.MutableAccount;
import org.idnecology.idn.evm.blockhash.BlockHashLookup;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.plugin.data.BlockOverrides;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes;

/**
 * Simulates the execution of a block, processing transactions and applying state overrides. This
 * class is responsible for simulating the execution of a block, which involves processing
 * transactions and applying state overrides. It provides a way to test and validate the behavior of
 * a block without actually executing it on the blockchain. The simulator takes into account various
 * factors, such as the block header, transaction calls, and state overrides, to simulate the
 * execution of the block. It returns a list of simulation results, which include the final block
 * header, transaction receipts, and other relevant information.
 */
public class BlockSimulator {
  private final TransactionSimulator transactionSimulator;
  private final WorldStateArchive worldStateArchive;
  private final ProtocolSchedule protocolSchedule;
  private final MiningConfiguration miningConfiguration;
  private final Blockchain blockchain;

  public BlockSimulator(
      final WorldStateArchive worldStateArchive,
      final ProtocolSchedule protocolSchedule,
      final TransactionSimulator transactionSimulator,
      final MiningConfiguration miningConfiguration,
      final Blockchain blockchain) {
    this.worldStateArchive = worldStateArchive;
    this.protocolSchedule = protocolSchedule;
    this.miningConfiguration = miningConfiguration;
    this.transactionSimulator = transactionSimulator;
    this.blockchain = blockchain;
  }

  /**
   * Processes a list of BlockStateCalls sequentially, collecting the results.
   *
   * @param header The block header for all simulations.
   * @param blockSimulationParameter The blockSimulationParameter containing the block state calls.
   * @return A list of BlockSimulationResult objects from processing each BlockStateCall.
   */
  public List<BlockSimulationResult> process(
      final BlockHeader header, final BlockSimulationParameter blockSimulationParameter) {
    try (final MutableWorldState ws =
        worldStateArchive
            .getWorldState(withBlockHeaderAndNoUpdateNodeHead(header))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Public world state not available for block " + header.toLogString()))) {
      return process(header, blockSimulationParameter, ws);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Error simulating block", e);
    }
  }

  /**
   * Processes a list of BlockStateCalls sequentially, collecting the results.
   *
   * @param header The block header for all simulations.
   * @param blockSimulationParameter The blockSimulationParameter containing the block state calls.
   * @param worldState The initial MutableWorldState to start with.
   * @return A list of BlockSimulationResult objects from processing each BlockStateCall.
   */
  public List<BlockSimulationResult> process(
      final BlockHeader header,
      final BlockSimulationParameter blockSimulationParameter,
      final MutableWorldState worldState) {
    List<BlockSimulationResult> simulationResults = new ArrayList<>();

    // Fill gaps between blocks
    List<BlockStateCall> blockStateCalls =
        fillBlockStateCalls(blockSimulationParameter.getBlockStateCalls(), header);

    for (BlockStateCall blockStateCall : blockStateCalls) {
      BlockSimulationResult simulationResult =
          processBlockStateCall(
              header, blockStateCall, worldState, blockSimulationParameter.isValidation());
      simulationResults.add(simulationResult);
    }
    return simulationResults;
  }

  /**
   * Processes a single BlockStateCall, simulating the block execution.
   *
   * @param header The block header for the simulation.
   * @param blockStateCall The BlockStateCall to process.
   * @param ws The MutableWorldState to use for the simulation.
   * @return A BlockSimulationResult from processing the BlockStateCall.
   */
  private BlockSimulationResult processBlockStateCall(
      final BlockHeader header,
      final BlockStateCall blockStateCall,
      final MutableWorldState ws,
      final boolean shouldValidate) {
    BlockOverrides blockOverrides = blockStateCall.getBlockOverrides();
    long timestamp = blockOverrides.getTimestamp().orElse(header.getTimestamp() + 1);
    ProtocolSpec newProtocolSpec = protocolSchedule.getForNextBlockHeader(header, timestamp);

    // Apply block header overrides and state overrides
    BlockHeader blockHeader = applyBlockHeaderOverrides(header, newProtocolSpec, blockOverrides);
    blockStateCall.getStateOverrideMap().ifPresent(overrides -> applyStateOverrides(overrides, ws));

    // Override the mining beneficiary calculator if a fee recipient is specified, otherwise use the
    // default
    MiningBeneficiaryCalculator miningBeneficiaryCalculator =
        getMiningBeneficiaryCalculator(blockOverrides, newProtocolSpec);

    BlockHashLookup blockHashLookup =
        createBlockHashLookup(blockOverrides, newProtocolSpec, blockHeader);

    List<TransactionSimulatorResult> transactionSimulatorResults =
        processTransactions(
            blockHeader,
            blockStateCall,
            ws,
            shouldValidate,
            miningBeneficiaryCalculator,
            blockHashLookup);

    return finalizeBlock(
        blockHeader, blockStateCall, ws, newProtocolSpec, transactionSimulatorResults);
  }

  @VisibleForTesting
  protected List<TransactionSimulatorResult> processTransactions(
      final BlockHeader blockHeader,
      final BlockStateCall blockStateCall,
      final MutableWorldState ws,
      final boolean shouldValidate,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final BlockHashLookup blockHashLookup) {

    List<TransactionSimulatorResult> transactionSimulations = new ArrayList<>();

    for (CallParameter callParameter : blockStateCall.getCalls()) {
      final WorldUpdater transactionUpdater = ws.updater();

      final Optional<TransactionSimulatorResult> transactionSimulatorResult =
          transactionSimulator.processWithWorldUpdater(
              callParameter,
              Optional.empty(), // We have already applied state overrides on block level
              buildTransactionValidationParams(shouldValidate),
              OperationTracer.NO_TRACING,
              blockHeader,
              transactionUpdater,
              miningBeneficiaryCalculator,
              blockHashLookup);

      if (transactionSimulatorResult.isEmpty()) {
        throw new BlockSimulationException("Transaction simulator result is empty");
      }

      TransactionSimulatorResult result = transactionSimulatorResult.get();
      if (result.isInvalid()) {
        throw new BlockSimulationException(
            "Transaction simulator result is invalid: " + result.getInvalidReason().orElse(null));
      }
      transactionSimulations.add(transactionSimulatorResult.get());
      transactionUpdater.commit();
    }
    return transactionSimulations;
  }

  @VisibleForTesting
  protected BlockSimulationResult finalizeBlock(
      final BlockHeader blockHeader,
      final BlockStateCall blockStateCall,
      final MutableWorldState ws,
      final ProtocolSpec protocolSpec,
      final List<TransactionSimulatorResult> transactionSimulations) {

    long currentGasUsed = 0;
    final var transactionReceiptFactory = protocolSpec.getTransactionReceiptFactory();

    final List<TransactionReceipt> receipts = new ArrayList<>();
    final List<Transaction> transactions = new ArrayList<>();

    for (TransactionSimulatorResult transactionSimulatorResult : transactionSimulations) {

      TransactionProcessingResult transactionProcessingResult = transactionSimulatorResult.result();
      final Transaction transaction = transactionSimulatorResult.transaction();

      currentGasUsed += transaction.getGasLimit() - transactionProcessingResult.getGasRemaining();

      final TransactionReceipt transactionReceipt =
          transactionReceiptFactory.create(
              transaction.getType(), transactionProcessingResult, ws, currentGasUsed);

      receipts.add(transactionReceipt);
      transactions.add(transaction);
    }

    BlockHeader finalBlockHeader =
        createFinalBlockHeader(
            blockHeader,
            ws,
            transactions,
            blockStateCall.getBlockOverrides(),
            receipts,
            currentGasUsed);
    Block block = new Block(finalBlockHeader, new BlockBody(transactions, List.of()));
    return new BlockSimulationResult(block, receipts, transactionSimulations);
  }

  /**
   * Applies state overrides to the world state.
   *
   * @param stateOverrideMap The StateOverrideMap containing the state overrides.
   * @param ws The MutableWorldState to apply the overrides to.
   */
  @VisibleForTesting
  protected void applyStateOverrides(
      final StateOverrideMap stateOverrideMap, final MutableWorldState ws) {
    var updater = ws.updater();
    for (Address accountToOverride : stateOverrideMap.keySet()) {
      final StateOverride override = stateOverrideMap.get(accountToOverride);
      MutableAccount account = updater.getOrCreate(accountToOverride);
      TransactionSimulator.applyOverrides(account, override);
    }
    updater.commit();
  }

  /**
   * Applies block header overrides to the block header.
   *
   * @param header The original block header.
   * @param newProtocolSpec The ProtocolSpec for the block.
   * @param blockOverrides The BlockOverrides to apply.
   * @return The modified block header.
   */
  @VisibleForTesting
  protected BlockHeader applyBlockHeaderOverrides(
      final BlockHeader header,
      final ProtocolSpec newProtocolSpec,
      final BlockOverrides blockOverrides) {
    long timestamp = blockOverrides.getTimestamp().orElse(header.getTimestamp() + 1);
    long blockNumber = blockOverrides.getBlockNumber().orElse(header.getNumber() + 1);

    return BlockHeaderBuilder.createDefault()
        .parentHash(header.getHash())
        .timestamp(timestamp)
        .number(blockNumber)
        .coinbase(
            blockOverrides
                .getFeeRecipient()
                .orElseGet(() -> miningConfiguration.getCoinbase().orElseThrow()))
        .difficulty(
            blockOverrides.getDifficulty().isPresent()
                ? Difficulty.of(blockOverrides.getDifficulty().get())
                : header.getDifficulty())
        .gasLimit(
            blockOverrides
                .getGasLimit()
                .orElseGet(() -> getNextGasLimit(newProtocolSpec, header, blockNumber)))
        .baseFee(
            blockOverrides
                .getBaseFeePerGas()
                .orElseGet(() -> getNextBaseFee(newProtocolSpec, header, blockNumber)))
        .mixHash(blockOverrides.getMixHashOrPrevRandao().orElse(Hash.EMPTY))
        .extraData(blockOverrides.getExtraData().orElse(Bytes.EMPTY))
        .blockHeaderFunctions(new SimulatorBlockHeaderFunctions(blockOverrides))
        .buildBlockHeader();
  }

  /**
   * Creates the final block header after applying state changes and transaction processing.
   *
   * @param blockHeader The original block header.
   * @param ws The MutableWorldState after applying state overrides.
   * @param transactions The list of transactions in the block.
   * @param blockOverrides The BlockOverrides to apply.
   * @param receipts The list of transaction receipts.
   * @param currentGasUsed The total gas used in the block.
   * @return The final block header.
   */
  private BlockHeader createFinalBlockHeader(
      final BlockHeader blockHeader,
      final MutableWorldState ws,
      final List<Transaction> transactions,
      final BlockOverrides blockOverrides,
      final List<TransactionReceipt> receipts,
      final long currentGasUsed) {

    return BlockHeaderBuilder.createDefault()
        .populateFrom(blockHeader)
        .ommersHash(BodyValidation.ommersHash(List.of()))
        .stateRoot(blockOverrides.getStateRoot().orElse(ws.rootHash()))
        .transactionsRoot(BodyValidation.transactionsRoot(transactions))
        .receiptsRoot(BodyValidation.receiptsRoot(receipts))
        .logsBloom(BodyValidation.logsBloom(receipts))
        .gasUsed(currentGasUsed)
        .withdrawalsRoot(null)
        .requestsHash(null)
        .mixHash(blockOverrides.getMixHashOrPrevRandao().orElse(Hash.EMPTY))
        .extraData(blockOverrides.getExtraData().orElse(Bytes.EMPTY))
        .blockHeaderFunctions(new SimulatorBlockHeaderFunctions(blockOverrides))
        .buildBlockHeader();
  }

  /**
   * Builds the TransactionValidationParams for the block simulation.
   *
   * @param shouldValidate Whether to validate transactions.
   * @return The TransactionValidationParams for the block simulation.
   */
  @VisibleForTesting
  ImmutableTransactionValidationParams buildTransactionValidationParams(
      final boolean shouldValidate) {

    if (shouldValidate) {
      return ImmutableTransactionValidationParams.builder()
          .from(TransactionValidationParams.processingBlock())
          .build();
    }

    return ImmutableTransactionValidationParams.builder()
        .from(TransactionValidationParams.transactionSimulator())
        .isAllowExceedingBalance(true)
        .build();
  }

  private long getNextGasLimit(
      final ProtocolSpec protocolSpec, final BlockHeader parentHeader, final long blockNumber) {
    return protocolSpec
        .getGasLimitCalculator()
        .nextGasLimit(
            parentHeader.getGasLimit(),
            miningConfiguration.getTargetGasLimit().orElse(parentHeader.getGasLimit()),
            blockNumber);
  }

  /**
   * Override the mining beneficiary calculator if a fee recipient is specified, otherwise use the
   * default
   */
  private MiningBeneficiaryCalculator getMiningBeneficiaryCalculator(
      final BlockOverrides blockOverrides, final ProtocolSpec newProtocolSpec) {
    if (blockOverrides.getFeeRecipient().isPresent()) {
      return blockHeader -> blockOverrides.getFeeRecipient().get();
    } else {
      return newProtocolSpec.getMiningBeneficiaryCalculator();
    }
  }

  private Wei getNextBaseFee(
      final ProtocolSpec protocolSpec, final BlockHeader parentHeader, final long blockNumber) {
    return Optional.of(protocolSpec.getFeeMarket())
        .filter(FeeMarket::implementsBaseFee)
        .map(BaseFeeMarket.class::cast)
        .map(
            feeMarket ->
                feeMarket.computeBaseFee(
                    blockNumber,
                    parentHeader.getBaseFee().orElse(Wei.ZERO),
                    parentHeader.getGasUsed(),
                    feeMarket.targetGasUsed(parentHeader)))
        .orElse(null);
  }

  private static class SimulatorBlockHeaderFunctions implements BlockHeaderFunctions {

    private final BlockOverrides blockOverrides;
    private final MainnetBlockHeaderFunctions blockHeaderFunctions =
        new MainnetBlockHeaderFunctions();

    private SimulatorBlockHeaderFunctions(final BlockOverrides blockOverrides) {
      this.blockOverrides = blockOverrides;
    }

    @Override
    public Hash hash(final BlockHeader header) {
      return blockOverrides.getBlockHash().orElseGet(() -> blockHeaderFunctions.hash(header));
    }

    @Override
    public ParsedExtraData parseExtraData(final BlockHeader header) {
      return blockHeaderFunctions.parseExtraData(header);
    }
  }

  /**
   * Creates a BlockHashLookup for the block simulation. If a BlockHashLookup is provided in the
   * BlockOverrides, it is used. Otherwise, the default BlockHashLookup is created.
   *
   * @param blockOverrides The BlockOverrides to use.
   * @param newProtocolSpec The ProtocolSpec for the block.
   * @param blockHeader The block header for the simulation.
   * @return The BlockHashLookup for the block simulation.
   */
  private BlockHashLookup createBlockHashLookup(
      final BlockOverrides blockOverrides,
      final ProtocolSpec newProtocolSpec,
      final BlockHeader blockHeader) {
    return blockOverrides
        .getBlockHashLookup()
        .<BlockHashLookup>map(
            blockHashLookup -> (frame, blockNumber) -> blockHashLookup.apply(blockNumber))
        .orElseGet(
            () ->
                newProtocolSpec
                    .getBlockHashProcessor()
                    .createBlockHashLookup(blockchain, blockHeader));
  }
}
