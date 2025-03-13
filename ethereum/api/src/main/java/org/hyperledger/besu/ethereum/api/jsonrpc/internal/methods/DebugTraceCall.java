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

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.INTERNAL_ERROR;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.TransactionTraceParams;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionDetails;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.debug.TraceOptions;
import org.idnecology.idn.ethereum.mainnet.ImmutableTransactionValidationParams;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.transaction.PreCloseStateHandler;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.vm.DebugOperationTracer;

import java.util.Optional;

public class DebugTraceCall extends AbstractTraceCall {
  private static final TransactionValidationParams TRANSACTION_VALIDATION_PARAMS =
      ImmutableTransactionValidationParams.builder()
          .from(TransactionValidationParams.transactionSimulator())
          .isAllowFutureNonce(true)
          .isAllowExceedingBalance(true)
          .allowUnderpriced(true)
          .build();

  public DebugTraceCall(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionSimulator transactionSimulator) {
    super(blockchainQueries, protocolSchedule, transactionSimulator, true);
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_TRACE_CALL.getMethodName();
  }

  @Override
  protected TraceOptions getTraceOptions(final JsonRpcRequestContext requestContext) {
    try {
      return requestContext
          .getOptionalParameter(2, TransactionTraceParams.class)
          .map(TransactionTraceParams::traceOptions)
          .orElse(TraceOptions.DEFAULT);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction trace parameter (index 2)",
          RpcErrorType.INVALID_TRANSACTION_TRACE_PARAMS,
          e);
    }
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    final Optional<BlockParameter> maybeBlockParameter;
    try {
      maybeBlockParameter = request.getOptionalParameter(1, BlockParameter.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block parameter (index 1)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }

    return maybeBlockParameter.orElse(BlockParameter.LATEST);
  }

  @Override
  protected PreCloseStateHandler<Object> getSimulatorResultHandler(
      final JsonRpcRequestContext requestContext, final DebugOperationTracer tracer) {
    return (mutableWorldState, maybeSimulatorResult) ->
        maybeSimulatorResult.map(
            result -> {
              if (result.isInvalid()) {
                final JsonRpcError error =
                    new JsonRpcError(
                        INTERNAL_ERROR, result.getValidationResult().getErrorMessage());
                return new JsonRpcErrorResponse(requestContext.getRequest().getId(), error);
              }

              final TransactionTrace transactionTrace =
                  new TransactionTrace(
                      result.transaction(), result.result(), tracer.getTraceFrames());

              return new DebugTraceTransactionDetails(transactionTrace);
            });
  }

  @Override
  protected TransactionValidationParams buildTransactionValidationParams() {
    return TRANSACTION_VALIDATION_PARAMS;
  }
}
