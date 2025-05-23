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
package org.idnecology.idn.ethereum.eth.transactions.layered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.transaction.TransactionInvalidReason.EXCEEDS_BLOCK_GAS_LIMIT;

import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.transactions.AbstractTransactionPoolTest;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactions;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.transaction.TransactionInvalidReason;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

public abstract class AbstractLayeredTransactionPoolTest extends AbstractTransactionPoolTest {
  @Override
  protected PendingTransactions createPendingTransactions(
      final TransactionPoolConfiguration poolConfig,
      final BiFunction<PendingTransaction, PendingTransaction, Boolean>
          transactionReplacementTester) {

    final var txPoolMetrics = new TransactionPoolMetrics(metricsSystem);
    final TransactionsLayer sparseLayer =
        new SparseTransactions(
            poolConfig,
            ethScheduler,
            new EndLayer(txPoolMetrics),
            txPoolMetrics,
            transactionReplacementTester,
            new BlobCache());
    final TransactionsLayer readyLayer =
        new ReadyTransactions(
            poolConfig,
            ethScheduler,
            sparseLayer,
            txPoolMetrics,
            transactionReplacementTester,
            new BlobCache());
    return new LayeredPendingTransactions(
        poolConfig,
        createPrioritizedTransactions(
            poolConfig, readyLayer, txPoolMetrics, transactionReplacementTester),
        ethScheduler);
  }

  protected abstract AbstractPrioritizedTransactions createPrioritizedTransactions(
      final TransactionPoolConfiguration poolConfig,
      final TransactionsLayer nextLayer,
      final TransactionPoolMetrics txPoolMetrics,
      final BiFunction<PendingTransaction, PendingTransaction, Boolean>
          transactionReplacementTester);

  @Test
  public void
      shouldAcceptAsPostponedLocalTransactionsEvenIfAnInvalidTransactionWithLowerNonceExists() {
    final Transaction invalidTx =
        createBaseTransaction(0).gasLimit(blockGasLimit + 1).createTransaction(KEY_PAIR1);

    final Transaction nextTx = createBaseTransaction(1).gasLimit(1).createTransaction(KEY_PAIR1);

    givenTransactionIsValid(invalidTx);
    givenTransactionIsValid(nextTx);

    addAndAssertTransactionViaApiInvalid(invalidTx, EXCEEDS_BLOCK_GAS_LIMIT);
    final ValidationResult<TransactionInvalidReason> result =
        transactionPool.addTransactionViaApi(nextTx);

    assertThat(result.isValid()).isTrue();
  }
}
