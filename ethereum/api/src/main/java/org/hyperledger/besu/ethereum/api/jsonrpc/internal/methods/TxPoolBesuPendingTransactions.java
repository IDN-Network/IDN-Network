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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.PendingTransactionsParams;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.TransactionPendingResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.transaction.pool.PendingTransactionFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.transaction.pool.PendingTransactionFilter.Filter;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TxPoolIdnPendingTransactions implements JsonRpcMethod {

  final PendingTransactionFilter pendingTransactionFilter;

  private final TransactionPool transactionPool;

  public TxPoolIdnPendingTransactions(final TransactionPool transactionPool) {
    this.transactionPool = transactionPool;
    this.pendingTransactionFilter = new PendingTransactionFilter();
  }

  @Override
  public String getName() {
    return RpcMethod.TX_POOL_BESU_PENDING_TRANSACTIONS.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {

    final Collection<PendingTransaction> pendingTransactions =
        transactionPool.getPendingTransactions();
    final int limit;
    try {
      limit =
          requestContext.getOptionalParameter(0, Integer.class).orElse(pendingTransactions.size());
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction limit parameter (index 0)",
          RpcErrorType.INVALID_TRANSACTION_LIMIT_PARAMS,
          e);
    }
    final List<Filter> filters;
    try {
      filters =
          requestContext
              .getOptionalParameter(1, PendingTransactionsParams.class)
              .map(PendingTransactionsParams::filters)
              .orElse(Collections.emptyList());
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid pending transactions parameter (index 1)",
          RpcErrorType.INVALID_PENDING_TRANSACTIONS_PARAMS,
          e);
    }

    final Collection<Transaction> pendingTransactionsFiltered =
        pendingTransactionFilter.reduce(pendingTransactions, filters, limit);

    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(),
        pendingTransactionsFiltered.stream()
            .map(TransactionPendingResult::new)
            .collect(Collectors.toSet()));
  }
}
