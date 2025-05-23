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
package org.idnecology.idn.ethereum.blockcreation.txselection;

import org.idnecology.idn.datatypes.PendingTransaction;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.plugin.data.ProcessableBlockHeader;

import com.google.common.base.Stopwatch;

public class TransactionEvaluationContext
    implements org.idnecology.idn.plugin.services.txselection.TransactionEvaluationContext {
  private final ProcessableBlockHeader pendingBlockHeader;
  private final PendingTransaction pendingTransaction;
  private final Stopwatch evaluationTimer;
  private final Wei transactionGasPrice;
  private final Wei minGasPrice;

  public TransactionEvaluationContext(
      final ProcessableBlockHeader pendingBlockHeader,
      final PendingTransaction pendingTransaction,
      final Stopwatch evaluationTimer,
      final Wei transactionGasPrice,
      final Wei minGasPrice) {
    this.pendingBlockHeader = pendingBlockHeader;
    this.pendingTransaction = pendingTransaction;
    this.evaluationTimer = evaluationTimer;
    this.transactionGasPrice = transactionGasPrice;
    this.minGasPrice = minGasPrice;
  }

  public Transaction getTransaction() {
    // ToDo: can we avoid this cast? either by always using the interface
    //  or moving our Transaction implementation in the datatypes package
    return (Transaction) pendingTransaction.getTransaction();
  }

  @Override
  public ProcessableBlockHeader getPendingBlockHeader() {
    return pendingBlockHeader;
  }

  @Override
  public PendingTransaction getPendingTransaction() {
    return pendingTransaction;
  }

  @Override
  public Stopwatch getEvaluationTimer() {
    return evaluationTimer;
  }

  @Override
  public Wei getTransactionGasPrice() {
    return transactionGasPrice;
  }

  @Override
  public Wei getMinGasPrice() {
    return minGasPrice;
  }
}
