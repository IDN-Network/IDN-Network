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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.results.transaction.pool;

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.results.transaction.pool.Predicate.ACTION;
import static org.idnecology.idn.ethereum.api.jsonrpc.internal.results.transaction.pool.Predicate.EQ;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * This class allows to filter a list of pending transactions
 *
 * <p>Here is the list of fields that can be used to filter a transaction : from, to, gas, gasPrice,
 * value, nonce
 */
public class PendingTransactionFilter {

  public static final String FROM_FIELD = "from";
  public static final String TO_FIELD = "to";
  public static final String GAS_FIELD = "gas";
  public static final String GAS_PRICE_FIELD = "gasPrice";
  public static final String VALUE_FIELD = "value";
  public static final String NONCE_FIELD = "nonce";

  public Collection<Transaction> reduce(
      final Collection<PendingTransaction> pendingTransactions,
      final List<Filter> filters,
      final int limit)
      throws InvalidJsonRpcParameters {
    return pendingTransactions.stream()
        .filter(pendingTx -> applyFilters(pendingTx, filters))
        .limit(limit)
        .map(PendingTransaction::getTransaction)
        .toList();
  }

  private boolean applyFilters(
      final PendingTransaction pendingTransaction, final List<Filter> filters)
      throws InvalidJsonRpcParameters {
    boolean isValid = true;
    for (Filter filter : filters) {
      final Predicate predicate = filter.getPredicate();
      final String value = filter.getFieldValue();
      switch (filter.getFieldName()) {
        case FROM_FIELD:
          isValid = validateFrom(pendingTransaction, predicate, value);
          break;
        case TO_FIELD:
          isValid = validateTo(pendingTransaction, predicate, value);
          break;
        case GAS_PRICE_FIELD:
          isValid =
              validateWei(
                  pendingTransaction.getTransaction().getGasPrice().get(), predicate, value);
          break;
        case GAS_FIELD:
          isValid =
              validateWei(
                  Wei.of(pendingTransaction.getTransaction().getGasLimit()), predicate, value);
          break;
        case VALUE_FIELD:
          isValid = validateWei(pendingTransaction.getTransaction().getValue(), predicate, value);
          break;
        case NONCE_FIELD:
          isValid = validateNonce(pendingTransaction, predicate, value);
          break;
      }
      if (!isValid) {
        return false;
      }
    }
    return true;
  }

  private boolean validateFrom(
      final PendingTransaction pendingTransaction, final Predicate predicate, final String value)
      throws InvalidJsonRpcParameters {
    return predicate
        .getOperator()
        .apply(pendingTransaction.getTransaction().getSender(), Address.fromHexString(value));
  }

  private boolean validateTo(
      final PendingTransaction pendingTransaction, final Predicate predicate, final String value)
      throws InvalidJsonRpcParameters {
    final Optional<Address> maybeTo = pendingTransaction.getTransaction().getTo();
    if (maybeTo.isPresent() && predicate.equals(EQ)) {
      return predicate.getOperator().apply(maybeTo.get(), Address.fromHexString(value));
    } else if (predicate.equals(ACTION)) {
      return pendingTransaction.getTransaction().isContractCreation();
    }
    return false;
  }

  private boolean validateNonce(
      final PendingTransaction pendingTransaction, final Predicate predicate, final String value)
      throws InvalidJsonRpcParameters {
    return predicate
        .getOperator()
        .apply(pendingTransaction.getTransaction().getNonce(), Long.decode(value));
  }

  private boolean validateWei(
      final Wei transactionWei, final Predicate predicate, final String value)
      throws InvalidJsonRpcParameters {
    return predicate.getOperator().apply(transactionWei, Wei.fromHexString(value));
  }

  public static class Filter {

    private final String fieldName;
    private final String fieldValue;
    private final Predicate predicate;

    public Filter(final String fieldName, final String fieldValue, final Predicate predicate) {
      this.fieldName = fieldName;
      this.fieldValue = fieldValue;
      this.predicate = predicate;
    }

    public String getFieldName() {
      return fieldName;
    }

    public String getFieldValue() {
      return fieldValue;
    }

    public Predicate getPredicate() {
      return predicate;
    }
  }
}
