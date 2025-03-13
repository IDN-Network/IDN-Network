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
package org.idnecology.idn.services;

import static org.idnecology.idn.ethereum.mainnet.TransactionValidationParams.transactionSimulator;
import static org.idnecology.idn.ethereum.mainnet.TransactionValidationParams.transactionSimulatorAllowExceedingBalance;
import static org.idnecology.idn.ethereum.mainnet.TransactionValidationParams.transactionSimulatorAllowExceedingBalanceAndFutureNonce;
import static org.idnecology.idn.ethereum.mainnet.TransactionValidationParams.transactionSimulatorAllowFutureNonce;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.StateOverrideMap;
import org.idnecology.idn.datatypes.Transaction;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.ethereum.transaction.CallParameter;
import org.idnecology.idn.ethereum.transaction.TransactionInvalidReason;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.plugin.Unstable;
import org.idnecology.idn.plugin.data.ProcessableBlockHeader;
import org.idnecology.idn.plugin.data.TransactionSimulationResult;
import org.idnecology.idn.plugin.services.TransactionSimulationService;

import java.util.Optional;

/** TransactionSimulationServiceImpl */
@Unstable
public class TransactionSimulationServiceImpl implements TransactionSimulationService {
  private Blockchain blockchain;
  private TransactionSimulator transactionSimulator;

  /** Create an instance to be configured */
  public TransactionSimulationServiceImpl() {}

  /**
   * Configure the service
   *
   * @param blockchain the blockchain
   * @param transactionSimulator transaction simulator
   */
  public void init(final Blockchain blockchain, final TransactionSimulator transactionSimulator) {
    this.blockchain = blockchain;
    this.transactionSimulator = transactionSimulator;
  }

  @Override
  public ProcessableBlockHeader simulatePendingBlockHeader() {
    return transactionSimulator.simulatePendingBlockHeader();
  }

  @Override
  public Optional<TransactionSimulationResult> simulate(
      final Transaction transaction,
      final Optional<StateOverrideMap> maybeStateOverrides,
      final Hash blockHash,
      final OperationTracer operationTracer,
      final boolean isAllowExceedingBalance) {

    final CallParameter callParameter = CallParameter.fromTransaction(transaction);

    final var maybeBlockHeader =
        blockchain.getBlockHeader(blockHash).or(() -> blockchain.getBlockHeaderSafe(blockHash));

    if (maybeBlockHeader.isEmpty()) {
      return Optional.of(
          new TransactionSimulationResult(
              transaction,
              TransactionProcessingResult.invalid(
                  ValidationResult.invalid(TransactionInvalidReason.BLOCK_NOT_FOUND))));
    }

    return transactionSimulator
        .process(
            callParameter,
            isAllowExceedingBalance
                ? transactionSimulatorAllowExceedingBalance()
                : transactionSimulator(),
            operationTracer,
            maybeBlockHeader.get())
        .map(res -> new TransactionSimulationResult(transaction, res.result()));
  }

  @Override
  public Optional<TransactionSimulationResult> simulate(
      final Transaction transaction,
      final Optional<StateOverrideMap> maybeStateOverrides,
      final ProcessableBlockHeader pendingBlockHeader,
      final OperationTracer operationTracer,
      final boolean isAllowExceedingBalance,
      final boolean isAllowFutureNonce) {

    final CallParameter callParameter = CallParameter.fromTransaction(transaction);

    return transactionSimulator
        .processOnPending(
            callParameter,
            maybeStateOverrides,
            isAllowExceedingBalance
                ? isAllowFutureNonce
                    ? transactionSimulatorAllowExceedingBalanceAndFutureNonce()
                    : transactionSimulatorAllowExceedingBalance()
                : isAllowFutureNonce
                    ? transactionSimulatorAllowFutureNonce()
                    : transactionSimulator(),
            operationTracer,
            (org.idnecology.idn.ethereum.core.ProcessableBlockHeader) pendingBlockHeader)
        .map(res -> new TransactionSimulationResult(transaction, res.result()));
  }
}
