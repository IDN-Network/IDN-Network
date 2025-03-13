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

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ExecutionContextTestFixture;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;

import java.util.Optional;
import java.util.function.BiFunction;

public class LayeredTransactionPoolBaseFeeTest extends AbstractLayeredTransactionPoolTest {

  @Override
  protected AbstractPrioritizedTransactions createPrioritizedTransactions(
      final TransactionPoolConfiguration poolConfig,
      final TransactionsLayer nextLayer,
      final TransactionPoolMetrics txPoolMetrics,
      final BiFunction<PendingTransaction, PendingTransaction, Boolean>
          transactionReplacementTester) {
    return new BaseFeePrioritizedTransactions(
        poolConfig,
        protocolContext.getBlockchain()::getChainHeadHeader,
        ethScheduler,
        nextLayer,
        txPoolMetrics,
        transactionReplacementTester,
        FeeMarket.london(0L),
        new BlobCache(),
        MiningConfiguration.newDefault());
  }

  @Override
  protected Transaction createTransaction(final int nonce, final Wei maxPrice) {
    return createTransactionBaseFeeMarket(nonce, maxPrice);
  }

  @Override
  protected TransactionTestFixture createBaseTransaction(final int nonce) {
    return createBaseTransactionBaseFeeMarket(nonce);
  }

  @Override
  protected ExecutionContextTestFixture createExecutionContextTestFixture() {
    return createExecutionContextTestFixtureBaseFeeMarket();
  }

  @Override
  protected FeeMarket getFeeMarket() {
    return FeeMarket.london(0L, Optional.of(BASE_FEE_FLOOR));
  }

  @Override
  protected Block appendBlock(
      final Difficulty difficulty,
      final BlockHeader parentBlock,
      final Transaction... transactionsToAdd) {
    return appendBlockBaseFeeMarket(difficulty, parentBlock, transactionsToAdd);
  }
}
