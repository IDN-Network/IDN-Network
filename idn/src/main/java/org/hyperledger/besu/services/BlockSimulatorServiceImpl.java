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
package org.idnecology.idn.services;

import org.idnecology.idn.datatypes.StateOverrideMap;
import org.idnecology.idn.datatypes.Transaction;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.transaction.BlockSimulationParameter;
import org.idnecology.idn.ethereum.transaction.BlockSimulationResult;
import org.idnecology.idn.ethereum.transaction.BlockSimulator;
import org.idnecology.idn.ethereum.transaction.BlockStateCall;
import org.idnecology.idn.ethereum.transaction.CallParameter;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.plugin.Unstable;
import org.idnecology.idn.plugin.data.BlockOverrides;
import org.idnecology.idn.plugin.data.PluginBlockSimulationResult;
import org.idnecology.idn.plugin.data.TransactionSimulationResult;
import org.idnecology.idn.plugin.services.BlockSimulationService;

import java.util.List;

/** This class is a service that simulates the processing of a block */
public class BlockSimulatorServiceImpl implements BlockSimulationService {
  private final BlockSimulator blockSimulator;
  private final WorldStateArchive worldStateArchive;
  private final Blockchain blockchain;

  /**
   * This constructor creates a BlockSimulatorServiceImpl object
   *
   * @param worldStateArchive the world state archive
   * @param miningConfiguration the mining configuration
   * @param transactionSimulator the transaction simulator
   * @param protocolSchedule the protocol schedule
   * @param blockchain the blockchain
   */
  public BlockSimulatorServiceImpl(
      final WorldStateArchive worldStateArchive,
      final MiningConfiguration miningConfiguration,
      final TransactionSimulator transactionSimulator,
      final ProtocolSchedule protocolSchedule,
      final Blockchain blockchain) {
    this.blockchain = blockchain;
    blockSimulator =
        new BlockSimulator(
            worldStateArchive,
            protocolSchedule,
            transactionSimulator,
            miningConfiguration,
            blockchain);
    this.worldStateArchive = worldStateArchive;
  }

  /**
   * Simulate the processing of a block given a header, a list of transactions, and blockOverrides.
   *
   * @param blockNumber the block number
   * @param transactions the transactions to include in the block
   * @param blockOverrides the blockSimulationOverride of the block
   * @param stateOverrides state overrides of the block
   * @return the block context
   */
  @Override
  public PluginBlockSimulationResult simulate(
      final long blockNumber,
      final List<? extends Transaction> transactions,
      final BlockOverrides blockOverrides,
      final StateOverrideMap stateOverrides) {
    return processSimulation(blockNumber, transactions, blockOverrides, stateOverrides, false);
  }

  /**
   * This method is experimental and should be used with caution. Simulate the processing of a block
   * given a header, a list of transactions, and blockOverrides and persist the WorldState
   *
   * @param blockNumber the block number
   * @param transactions the transactions to include in the block
   * @param blockOverrides block overrides for the block
   * @param stateOverrides state overrides of the block
   * @return the PluginBlockSimulationResult
   */
  @Unstable
  @Override
  public PluginBlockSimulationResult simulateAndPersistWorldState(
      final long blockNumber,
      final List<? extends Transaction> transactions,
      final BlockOverrides blockOverrides,
      final StateOverrideMap stateOverrides) {
    return processSimulation(blockNumber, transactions, blockOverrides, stateOverrides, true);
  }

  private PluginBlockSimulationResult processSimulation(
      final long blockNumber,
      final List<? extends Transaction> transactions,
      final BlockOverrides blockOverrides,
      final StateOverrideMap stateOverrides,
      final boolean persistWorldState) {
    BlockHeader header = getBlockHeader(blockNumber);
    List<CallParameter> callParameters =
        transactions.stream().map(CallParameter::fromTransaction).toList();
    BlockStateCall blockStateCall =
        new BlockStateCall(callParameters, blockOverrides, stateOverrides);
    try (final MutableWorldState ws = getWorldState(header, persistWorldState)) {
      BlockSimulationParameter blockSimulationParameter =
          new BlockSimulationParameter(blockStateCall, true);
      List<BlockSimulationResult> results =
          blockSimulator.process(header, blockSimulationParameter, ws);
      BlockSimulationResult result = results.getFirst();
      if (persistWorldState) {
        ws.persist(result.getBlock().getHeader());
      }
      return response(result);
    } catch (final Exception e) {
      throw new RuntimeException("Error simulating block", e);
    }
  }

  private BlockHeader getBlockHeader(final long blockNumber) {
    return blockchain
        .getBlockHeader(blockNumber)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Block header not found for block number: " + blockNumber));
  }

  private MutableWorldState getWorldState(final BlockHeader header, final boolean isPersisting) {
    final WorldStateQueryParams worldStateQueryParams =
        WorldStateQueryParams.newBuilder()
            .withBlockHeader(header)
            .withShouldWorldStateUpdateHead(isPersisting)
            .build();
    return worldStateArchive
        .getWorldState(worldStateQueryParams)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "World state not available for block number (block hash): "
                        + header.toLogString()));
  }

  private PluginBlockSimulationResult response(final BlockSimulationResult result) {
    return new PluginBlockSimulationResult(
        result.getBlockHeader(),
        result.getBlockBody(),
        result.getReceipts(),
        result.getTransactionSimulations().stream()
            .map(
                simulation ->
                    new TransactionSimulationResult(simulation.transaction(), simulation.result()))
            .toList());
  }
}
