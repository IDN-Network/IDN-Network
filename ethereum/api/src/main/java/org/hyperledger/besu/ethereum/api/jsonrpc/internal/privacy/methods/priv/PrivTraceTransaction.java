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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.TraceTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.privateProcessor.PrivateBlockTracer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.privacy.privateTracing.PrivateFlatTrace;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.query.PrivacyQueries;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(since = "24.12.0")
public class PrivTraceTransaction extends AbstractPrivateTraceByHash implements JsonRpcMethod {
  private static final Logger LOG = LoggerFactory.getLogger(TraceTransaction.class);

  public PrivTraceTransaction(
      final Supplier<PrivateBlockTracer> blockTracerSupplier,
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final PrivacyQueries privacyQueries,
      final PrivacyController privacyController,
      final PrivacyParameters privacyParameters,
      final PrivacyIdProvider privacyIdProvider) {
    super(
        blockTracerSupplier,
        blockchainQueries,
        privacyQueries,
        protocolSchedule,
        privacyController,
        privacyParameters,
        privacyIdProvider);
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_TRACE_TRANSACTION.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final String privacyGroupId;
    try {
      privacyGroupId = requestContext.getRequiredParameter(0, String.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid privacy group ID parameter (index 0)",
          RpcErrorType.INVALID_PRIVACY_GROUP_PARAMS,
          e);
    }
    final Hash transactionHash;
    try {
      transactionHash = requestContext.getRequiredParameter(1, Hash.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction hash parameter (index 1)",
          RpcErrorType.INVALID_TRANSACTION_HASH_PARAMS,
          e);
    }
    LOG.trace("Received RPC rpcName={} txHash={}", getName(), transactionHash);

    if (privacyGroupId.isEmpty() || transactionHash.isEmpty()) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVALID_PARAMS);
    }

    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(),
        arrayNodeFromTraceStream(
            resultByTransactionHash(privacyGroupId, transactionHash, requestContext)));
  }

  protected JsonNode arrayNodeFromTraceStream(final Stream<PrivateFlatTrace> traceStream) {
    final ObjectMapper mapper = new ObjectMapper();
    final ArrayNode resultArrayNode = mapper.createArrayNode();
    traceStream.forEachOrdered(resultArrayNode::addPOJO);
    return resultArrayNode;
  }
}
