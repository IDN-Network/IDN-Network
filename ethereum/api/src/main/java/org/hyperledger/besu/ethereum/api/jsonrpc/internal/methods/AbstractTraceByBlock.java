/*
 * Copyright contributors to Idn ecology Idn.
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

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.TraceTypeParameter.TraceType;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.TraceTypeParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.TraceCallResult;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.diff.StateDiffGenerator;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.diff.StateDiffTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTraceGenerator;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.vm.VmTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.vm.VmTraceGenerator;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.debug.TraceOptions;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.transaction.TransactionSimulatorResult;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractTraceByBlock extends AbstractBlockParameterMethod
    implements JsonRpcMethod {
  protected final ProtocolSchedule protocolSchedule;
  protected final TransactionSimulator transactionSimulator;

  protected static final ObjectMapper mapper = new ObjectMapper();

  protected AbstractTraceByBlock(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionSimulator transactionSimulator) {
    super(blockchainQueries);

    this.protocolSchedule = protocolSchedule;
    this.transactionSimulator = transactionSimulator;
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    final Optional<BlockParameter> maybeBlockParameter;
    try {
      maybeBlockParameter = request.getOptionalParameter(2, BlockParameter.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block parameter (index 2)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }

    if (maybeBlockParameter.isPresent()) {
      return maybeBlockParameter.get();
    }

    return BlockParameter.LATEST;
  }

  protected JsonNode getTraceCallResult(
      final ProtocolSchedule protocolSchedule,
      final Set<TraceTypeParameter.TraceType> traceTypes,
      final TransactionSimulatorResult simulatorResult,
      final TransactionTrace transactionTrace,
      final Block block) {
    final TraceCallResult.Builder builder = TraceCallResult.builder();

    transactionTrace
        .getResult()
        .getRevertReason()
        .ifPresentOrElse(
            revertReason -> builder.output(revertReason.toHexString()),
            () -> builder.output(simulatorResult.getOutput().toString()));

    if (traceTypes.contains(TraceType.STATE_DIFF)) {
      new StateDiffGenerator()
          .generateStateDiff(transactionTrace)
          .forEachOrdered(stateDiff -> builder.stateDiff((StateDiffTrace) stateDiff));
    }

    if (traceTypes.contains(TraceType.TRACE)) {
      FlatTraceGenerator.generateFromTransactionTrace(
              protocolSchedule, transactionTrace, block, new AtomicInteger(), false)
          .forEachOrdered(trace -> builder.addTrace((FlatTrace) trace));
    }

    if (traceTypes.contains(TraceType.VM_TRACE)) {
      new VmTraceGenerator(transactionTrace)
          .generateTraceStream()
          .forEachOrdered(vmTrace -> builder.vmTrace((VmTrace) vmTrace));
    }

    return mapper.valueToTree(builder.build());
  }

  protected TransactionValidationParams buildTransactionValidationParams() {
    return TransactionValidationParams.transactionSimulator();
  }

  protected TraceOptions buildTraceOptions(final Set<TraceTypeParameter.TraceType> traceTypes) {
    return new TraceOptions(
        traceTypes.contains(TraceType.STATE_DIFF),
        false,
        traceTypes.contains(TraceType.TRACE) || traceTypes.contains(TraceType.VM_TRACE));
  }
}
