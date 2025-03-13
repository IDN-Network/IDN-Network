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

import static org.idnecology.idn.services.pipeline.PipelineBuilder.createPipelineFrom;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.FilterParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.Tracer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.tracing.flat.RewardTraceGenerator;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.util.ArrayNodeWrapper;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.debug.TraceOptions;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.vm.DebugOperationTracer;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.plugin.services.metrics.LabelledMetric;
import org.idnecology.idn.services.pipeline.Pipeline;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceBlock extends AbstractBlockParameterMethod {
  private static final Logger LOG = LoggerFactory.getLogger(TraceBlock.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  protected final ProtocolSchedule protocolSchedule;
  private final LabelledMetric<Counter> outputCounter;
  protected final EthScheduler ethScheduler;

  public TraceBlock(
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries queries,
      final MetricsSystem metricsSystem,
      final EthScheduler ethScheduler) {
    super(queries);
    this.protocolSchedule = protocolSchedule;
    this.outputCounter =
        metricsSystem.createLabelledCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "transactions_traceblock_pipeline_processed_total",
            "Number of transactions processed for each block",
            "step",
            "action");
    this.ethScheduler = ethScheduler;
  }

  @Override
  public String getName() {
    return RpcMethod.TRACE_BLOCK.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    try {
      return request.getRequiredParameter(0, BlockParameter.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block parameter (index 0)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }
  }

  @Override
  protected Object resultByBlockNumber(
      final JsonRpcRequestContext request, final long blockNumber) {
    if (blockNumber == BlockHeader.GENESIS_BLOCK_NUMBER) {
      // Nothing to trace for the genesis block
      return emptyResult().getArrayNode();
    }
    LOG.trace("Received RPC rpcName={} block={}", getName(), blockNumber);

    return getBlockchainQueries()
        .getBlockchain()
        .getBlockByNumber(blockNumber)
        .map(block -> traceBlock(block, Optional.empty()))
        .map(ArrayNodeWrapper::getArrayNode)
        .orElse(null);
  }

  protected ArrayNodeWrapper traceBlock(
      final Block block, final Optional<FilterParameter> filterParameter) {

    if (block == null) {
      return emptyResult();
    }
    final BlockHeader header = block.getHeader();

    return Tracer.processTracing(
            getBlockchainQueries(),
            Optional.of(header),
            traceableState -> {
              ArrayNodeWrapper resultArrayNode = new ArrayNodeWrapper(MAPPER.createArrayNode());
              final ProtocolSpec protocolSpec = protocolSchedule.getByBlockHeader(header);
              final MainnetTransactionProcessor transactionProcessor =
                  protocolSpec.getTransactionProcessor();
              final ChainUpdater chainUpdater = new ChainUpdater(traceableState);

              TransactionSource transactionSource = new TransactionSource(block);
              DebugOperationTracer debugOperationTracer =
                  new DebugOperationTracer(new TraceOptions(false, false, true), false);
              ExecuteTransactionStep executeTransactionStep =
                  new ExecuteTransactionStep(
                      chainUpdater,
                      transactionProcessor,
                      getBlockchainQueries().getBlockchain(),
                      debugOperationTracer,
                      protocolSpec,
                      block);
              TraceFlatTransactionStep traceFlatTransactionStep =
                  new TraceFlatTransactionStep(protocolSchedule, block, filterParameter);
              BuildArrayNodeCompleterStep buildArrayNodeStep =
                  new BuildArrayNodeCompleterStep(resultArrayNode);
              Pipeline<TransactionTrace> traceBlockPipeline =
                  createPipelineFrom(
                          "getTransactions",
                          transactionSource,
                          4,
                          outputCounter,
                          false,
                          "trace_block_transactions")
                      .thenProcess("executeTransaction", executeTransactionStep)
                      .thenProcessAsyncOrdered("traceFlatTransaction", traceFlatTransactionStep, 4)
                      .andFinishWith(
                          "buildArrayNode",
                          traceStream -> traceStream.forEachOrdered(buildArrayNodeStep));

              try {
                ethScheduler.startPipeline(traceBlockPipeline).get();
              } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
              }

              resultArrayNode = buildArrayNodeStep.getResultArrayNode();
              generateRewardsFromBlock(filterParameter, block, resultArrayNode);
              return Optional.of(resultArrayNode);
            })
        .orElse(emptyResult());
  }

  protected void generateRewardsFromBlock(
      final Optional<FilterParameter> maybeFilterParameter,
      final Block block,
      final ArrayNodeWrapper resultArrayNode) {
    RewardTraceGenerator.generateFromBlock(protocolSchedule, block)
        .forEachOrdered(resultArrayNode::addPOJO);
  }

  ArrayNodeWrapper emptyResult() {
    return new ArrayNodeWrapper(MAPPER.createArrayNode());
  }

  public static class ChainUpdater {

    private final MutableWorldState worldState;
    private WorldUpdater updater;

    public ChainUpdater(final MutableWorldState worldState) {
      this.worldState = worldState;
    }

    public ChainUpdater(final MutableWorldState worldState, final WorldUpdater updater) {
      this.worldState = worldState;
      this.updater = updater;
    }

    public WorldUpdater getNextUpdater() {
      // if we have no prior updater, it must be the first TX, so use the block's initial state
      if (updater == null) {
        updater = worldState.updater();
      } else {
        updater.markTransactionBoundary();
      }
      updater = updater.updater();
      return updater;
    }
  }
}
