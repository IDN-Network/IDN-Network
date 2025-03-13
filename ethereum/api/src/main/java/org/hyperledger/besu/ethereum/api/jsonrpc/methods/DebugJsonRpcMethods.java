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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.DebugReplayBlock;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugAccountAt;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugAccountRange;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugBatchSendRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugGetBadBlocks;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugGetRawBlock;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugGetRawHeader;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugGetRawReceipts;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugGetRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugMetrics;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugResyncWorldstate;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugSetHead;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugStandardTraceBadBlockToFile;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugStandardTraceBlockToFile;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugStorageRangeAt;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugTraceBlock;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugTraceBlockByHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugTraceBlockByNumber;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugTraceCall;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.DebugTraceTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.BlockReplay;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.BlockTracer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.TransactionTracer;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.metrics.ObservableMetricsSystem;

import java.nio.file.Path;
import java.util.Map;

public class DebugJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final BlockResultFactory blockResult = new BlockResultFactory();

  private final BlockchainQueries blockchainQueries;
  private final ProtocolContext protocolContext;
  private final ProtocolSchedule protocolSchedule;
  private final ObservableMetricsSystem metricsSystem;
  private final TransactionPool transactionPool;
  private final Synchronizer synchronizer;
  private final Path dataDir;
  private final TransactionSimulator transactionSimulator;
  private final EthScheduler ethScheduler;

  DebugJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final ObservableMetricsSystem metricsSystem,
      final TransactionPool transactionPool,
      final Synchronizer synchronizer,
      final Path dataDir,
      final TransactionSimulator transactionSimulator,
      final EthScheduler ethScheduler) {
    this.blockchainQueries = blockchainQueries;
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.metricsSystem = metricsSystem;
    this.transactionPool = transactionPool;
    this.synchronizer = synchronizer;
    this.dataDir = dataDir;
    this.transactionSimulator = transactionSimulator;
    this.ethScheduler = ethScheduler;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.DEBUG.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final BlockReplay blockReplay =
        new BlockReplay(protocolSchedule, protocolContext, blockchainQueries.getBlockchain());

    return mapOf(
        new DebugTraceTransaction(blockchainQueries, new TransactionTracer(blockReplay)),
        new DebugAccountRange(blockchainQueries),
        new DebugStorageRangeAt(blockchainQueries, blockReplay),
        new DebugMetrics(metricsSystem),
        new DebugResyncWorldstate(protocolContext, synchronizer),
        new DebugTraceBlock(protocolSchedule, blockchainQueries, metricsSystem, ethScheduler),
        new DebugSetHead(blockchainQueries, protocolContext),
        new DebugReplayBlock(blockchainQueries, protocolContext, protocolSchedule),
        new DebugTraceBlockByNumber(
            protocolSchedule, blockchainQueries, metricsSystem, ethScheduler),
        new DebugTraceBlockByHash(protocolSchedule, blockchainQueries, metricsSystem, ethScheduler),
        new DebugBatchSendRawTransaction(transactionPool),
        new DebugGetBadBlocks(protocolContext, blockResult),
        new DebugStandardTraceBlockToFile(
            () -> new TransactionTracer(blockReplay), blockchainQueries, dataDir),
        new DebugStandardTraceBadBlockToFile(
            () -> new TransactionTracer(blockReplay), blockchainQueries, protocolContext, dataDir),
        new DebugAccountAt(blockchainQueries, () -> new BlockTracer(blockReplay)),
        new DebugGetRawHeader(blockchainQueries),
        new DebugGetRawBlock(blockchainQueries),
        new DebugGetRawReceipts(blockchainQueries),
        new DebugGetRawTransaction(blockchainQueries),
        new DebugTraceCall(blockchainQueries, protocolSchedule, transactionSimulator));
  }
}
