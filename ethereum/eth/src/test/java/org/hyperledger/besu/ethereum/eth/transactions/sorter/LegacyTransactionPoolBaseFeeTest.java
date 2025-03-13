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
package org.idnecology.idn.ethereum.eth.transactions.sorter;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ExecutionContextTestFixture;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;
import org.idnecology.idn.ethereum.eth.transactions.AbstractTransactionPoolTest;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactions;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.testutil.TestClock;

import java.time.ZoneId;
import java.util.Optional;
import java.util.function.BiFunction;

public class LegacyTransactionPoolBaseFeeTest extends AbstractTransactionPoolTest {

  @Override
  protected PendingTransactions createPendingTransactions(
      final TransactionPoolConfiguration poolConfig,
      final BiFunction<PendingTransaction, PendingTransaction, Boolean>
          transactionReplacementTester) {

    return new BaseFeePendingTransactionsSorter(
        poolConfig,
        TestClock.system(ZoneId.systemDefault()),
        metricsSystem,
        protocolContext.getBlockchain()::getChainHeadHeader);
  }

  @Override
  protected Transaction createTransaction(final int transactionNumber, final Wei maxPrice) {
    return createTransactionBaseFeeMarket(transactionNumber, maxPrice);
  }

  @Override
  protected TransactionTestFixture createBaseTransaction(final int transactionNumber) {
    return createBaseTransactionBaseFeeMarket(transactionNumber);
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
