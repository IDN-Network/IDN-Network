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
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.PendingTransactionsResult;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;

public class TxPoolIdnTransactions implements JsonRpcMethod {

  private final TransactionPool transactionPool;

  public TxPoolIdnTransactions(final TransactionPool transactionPool) {
    this.transactionPool = transactionPool;
  }

  @Override
  public String getName() {
    return RpcMethod.TX_POOL_BESU_TRANSACTIONS.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final JsonRpcSuccessResponse jsonRpcSuccessResponse =
        new JsonRpcSuccessResponse(
            requestContext.getRequest().getId(),
            new PendingTransactionsResult(transactionPool.getPendingTransactions()));
    return jsonRpcSuccessResponse;
  }
}
