/*
 * Copyright contributors to Idn.
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

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.INTERNAL_ERROR;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.TraceTypeParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.debug.TraceOptions;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.transaction.PreCloseStateHandler;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.vm.DebugOperationTracer;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceCall extends AbstractTraceCall {
  private static final Logger LOG = LoggerFactory.getLogger(TraceCall.class);

  public TraceCall(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionSimulator transactionSimulator) {
    super(blockchainQueries, protocolSchedule, transactionSimulator, false);
  }

  @Override
  public String getName() {
    return transactionSimulator != null ? RpcMethod.TRACE_CALL.getMethodName() : null;
  }

  @Override
  protected TraceOptions getTraceOptions(final JsonRpcRequestContext requestContext) {
    return buildTraceOptions(getTraceTypes(requestContext));
  }

  private Set<TraceTypeParameter.TraceType> getTraceTypes(
      final JsonRpcRequestContext requestContext) {
    try {
      return requestContext.getRequiredParameter(1, TraceTypeParameter.class).getTraceTypes();
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid trace type parameter (index 1)", RpcErrorType.INVALID_TRACE_TYPE_PARAMS, e);
    }
  }

  @Override
  protected PreCloseStateHandler<Object> getSimulatorResultHandler(
      final JsonRpcRequestContext requestContext, final DebugOperationTracer tracer) {
    return (mutableWorldState, maybeSimulatorResult) ->
        maybeSimulatorResult.map(
            result -> {
              if (result.isInvalid()) {
                LOG.error("Invalid simulator result {}", result);
                return new JsonRpcErrorResponse(
                    requestContext.getRequest().getId(), INTERNAL_ERROR);
              }

              final TransactionTrace transactionTrace =
                  new TransactionTrace(
                      result.transaction(), result.result(), tracer.getTraceFrames());

              final Block block =
                  blockchainQueriesSupplier.get().getBlockchain().getChainHeadBlock();
              return getTraceCallResult(
                  protocolSchedule, getTraceTypes(requestContext), result, transactionTrace, block);
            });
  }
}
