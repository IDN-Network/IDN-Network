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

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.LogsResult;
import org.idnecology.idn.ethereum.core.LogWithMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class EthGetFilterChanges implements JsonRpcMethod {

  private final FilterManager filterManager;

  public EthGetFilterChanges(final FilterManager filterManager) {
    this.filterManager = filterManager;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_FILTER_CHANGES.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final String filterId;
    try {
      filterId = requestContext.getRequiredParameter(0, String.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid filter ID parameter (index 0)", RpcErrorType.INVALID_FILTER_PARAMS, e);
    }

    final List<Hash> blockHashes = filterManager.blockChanges(filterId);
    if (blockHashes != null) {
      return new JsonRpcSuccessResponse(
          requestContext.getRequest().getId(),
          blockHashes.stream().map(h -> h.toString()).collect(Collectors.toList()));
    }

    final List<Hash> transactionHashes = filterManager.pendingTransactionChanges(filterId);
    if (transactionHashes != null) {
      return new JsonRpcSuccessResponse(
          requestContext.getRequest().getId(),
          transactionHashes.stream().map(h -> h.toString()).collect(Collectors.toList()));
    }

    final List<LogWithMetadata> logs = filterManager.logsChanges(filterId);
    if (logs != null) {
      return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), new LogsResult(logs));
    }

    // Filter was not found.
    return new JsonRpcErrorResponse(
        requestContext.getRequest().getId(), RpcErrorType.FILTER_NOT_FOUND);
  }
}
