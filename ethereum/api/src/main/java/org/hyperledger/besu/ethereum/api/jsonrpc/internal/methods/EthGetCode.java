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
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;

import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;

public class EthGetCode extends AbstractBlockParameterOrBlockHashMethod {

  public EthGetCode(final BlockchainQueries blockchainQueries) {
    super(blockchainQueries);
  }

  public EthGetCode(final Supplier<BlockchainQueries> blockchainQueries) {
    super(blockchainQueries);
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_CODE.getMethodName();
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
  protected String resultByBlockHash(final JsonRpcRequestContext request, final Hash blockHash) {
    final Address address;
    try {
      address = request.getRequiredParameter(0, Address.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid address parameter (index 0)", RpcErrorType.INVALID_ADDRESS_PARAMS, e);
    }
    return getBlockchainQueries().getCode(address, blockHash).map(Bytes::toString).orElse(null);
  }
}
