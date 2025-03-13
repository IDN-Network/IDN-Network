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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameterOrBlockHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockReceiptsResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.TransactionReceiptResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.TransactionReceiptRootResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.TransactionReceiptStatusResult;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.query.TransactionReceiptWithMetadata;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.TransactionReceiptType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;

public class EthGetBlockReceipts extends AbstractBlockParameterOrBlockHashMethod {

  private final ProtocolSchedule protocolSchedule;

  public EthGetBlockReceipts(
      final BlockchainQueries blockchain, final ProtocolSchedule protocolSchedule) {
    super(Suppliers.ofInstance(blockchain));
    this.protocolSchedule = protocolSchedule;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_BLOCK_RECEIPTS.getMethodName();
  }

  @Override
  protected BlockParameterOrBlockHash blockParameterOrBlockHash(
      final JsonRpcRequestContext request) {
    try {
      return request.getRequiredParameter(0, BlockParameterOrBlockHash.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block or block hash parameters (index 0)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }
  }

  @Override
  protected Object resultByBlockHash(final JsonRpcRequestContext request, final Hash blockHash) {
    return getBlockReceiptsResult(blockHash);
  }

  /*
   * For a given block, get receipts of transactions in the block and if they exist, wrap in transaction receipts of the correct type
   */
  private BlockReceiptsResult getBlockReceiptsResult(final Hash blockHash) {
    final List<TransactionReceiptResult> receiptList =
        blockchainQueries
            .get()
            .transactionReceiptsByBlockHash(blockHash, protocolSchedule)
            .orElse(new ArrayList<TransactionReceiptWithMetadata>())
            .stream()
            .map(
                receipt ->
                    receipt.getReceipt().getTransactionReceiptType() == TransactionReceiptType.ROOT
                        ? new TransactionReceiptRootResult(receipt)
                        : new TransactionReceiptStatusResult(receipt))
            .collect(Collectors.toList());

    return new BlockReceiptsResult(receiptList);
  }
}
