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

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameterOrBlockHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.Quantity;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public class EthGetTransactionCount extends AbstractBlockParameterOrBlockHashMethod {
  private final Supplier<TransactionPool> transactionPoolSupplier;

  public EthGetTransactionCount(
      final BlockchainQueries blockchain, final TransactionPool transactionPoolSupplier) {
    this(Suppliers.ofInstance(blockchain), Suppliers.ofInstance(transactionPoolSupplier));
  }

  public EthGetTransactionCount(
      final Supplier<BlockchainQueries> blockchain,
      final Supplier<TransactionPool> transactionPoolSupplier) {
    super(blockchain);
    this.transactionPoolSupplier = transactionPoolSupplier;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_TRANSACTION_COUNT.getMethodName();
  }

  @Override
  protected BlockParameterOrBlockHash blockParameterOrBlockHash(
      final JsonRpcRequestContext request) {
    try {
      return request.getRequiredParameter(1, BlockParameterOrBlockHash.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block or block hash parameter (index 1)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }
  }

  @Override
  protected Object pendingResult(final JsonRpcRequestContext request) {
    final Address address;
    try {
      address = request.getRequiredParameter(0, Address.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid address parameter (index 0)", RpcErrorType.INVALID_ADDRESS_PARAMS, e);
    }
    final long pendingNonce =
        transactionPoolSupplier.get().getNextNonceForSender(address).orElse(0);
    final long latestNonce =
        getBlockchainQueries()
            .getTransactionCount(
                address, getBlockchainQueries().getBlockchain().getChainHead().getHash());

    if (Long.compareUnsigned(pendingNonce, latestNonce) > 0) {
      return Quantity.create(pendingNonce);
    }

    return Quantity.create(latestNonce);
  }

  @Override
  protected String resultByBlockHash(final JsonRpcRequestContext request, final Hash blockHash) {
    final Address address;
    try {
      address = request.getRequiredParameter(0, Address.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid address parameter (index 0)", RpcErrorType.INVALID_ADDRESS_PARAMS, e);
    }
    final long transactionCount = getBlockchainQueries().getTransactionCount(address, blockHash);

    return Quantity.create(transactionCount);
  }
}
