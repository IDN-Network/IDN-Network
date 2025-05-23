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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthAccounts;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthBlobBaseFee;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthBlockNumber;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthCall;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthChainId;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthCoinbase;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthCreateAccessList;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthEstimateGas;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthFeeHistory;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGasPrice;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetBalance;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetBlockByHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetBlockByNumber;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetBlockReceipts;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetBlockTransactionCountByHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetBlockTransactionCountByNumber;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetCode;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetFilterChanges;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetFilterLogs;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetLogs;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetMinerDataByBlockHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetMinerDataByBlockNumber;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetProof;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetStorageAt;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetTransactionByBlockHashAndIndex;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetTransactionByBlockNumberAndIndex;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetTransactionByHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetTransactionCount;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetTransactionReceipt;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetUncleByBlockHashAndIndex;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetUncleByBlockNumberAndIndex;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetUncleCountByBlockHash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetUncleCountByBlockNumber;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthGetWork;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthHashrate;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthMaxPriorityFeePerGas;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthMining;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthNewBlockFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthNewFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthNewPendingTransactionFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthProtocolVersion;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthSendRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthSendTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthSubmitHashRate;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthSubmitWork;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthSyncing;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.EthUninstallFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;

import java.util.Map;
import java.util.Set;

public class EthJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final BlockResultFactory blockResult = new BlockResultFactory();

  private final BlockchainQueries blockchainQueries;
  private final Synchronizer synchronizer;
  private final ProtocolSchedule protocolSchedule;
  private final FilterManager filterManager;
  private final TransactionPool transactionPool;
  private final MiningCoordinator miningCoordinator;
  private final Set<Capability> supportedCapabilities;
  private final ApiConfiguration apiConfiguration;
  private final TransactionSimulator transactionSimulator;

  public EthJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final Synchronizer synchronizer,
      final ProtocolSchedule protocolSchedule,
      final FilterManager filterManager,
      final TransactionPool transactionPool,
      final MiningCoordinator miningCoordinator,
      final Set<Capability> supportedCapabilities,
      final ApiConfiguration apiConfiguration,
      final TransactionSimulator transactionSimulator) {
    this.blockchainQueries = blockchainQueries;
    this.synchronizer = synchronizer;
    this.protocolSchedule = protocolSchedule;
    this.filterManager = filterManager;
    this.transactionPool = transactionPool;
    this.miningCoordinator = miningCoordinator;
    this.supportedCapabilities = supportedCapabilities;
    this.apiConfiguration = apiConfiguration;
    this.transactionSimulator = transactionSimulator;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.ETH.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    return mapOf(
        new EthAccounts(),
        new EthBlockNumber(blockchainQueries),
        new EthGetBalance(blockchainQueries),
        new EthGetBlockByHash(blockchainQueries, blockResult),
        new EthGetBlockByNumber(blockchainQueries, blockResult, synchronizer),
        new EthGetBlockReceipts(blockchainQueries, protocolSchedule),
        new EthGetBlockTransactionCountByNumber(blockchainQueries),
        new EthGetBlockTransactionCountByHash(blockchainQueries),
        new EthCall(blockchainQueries, transactionSimulator),
        new EthFeeHistory(protocolSchedule, blockchainQueries, miningCoordinator, apiConfiguration),
        new EthGetCode(blockchainQueries),
        new EthGetLogs(blockchainQueries, apiConfiguration.getMaxLogsRange()),
        new EthGetProof(blockchainQueries),
        new EthGetUncleCountByBlockHash(blockchainQueries),
        new EthGetUncleCountByBlockNumber(blockchainQueries),
        new EthGetUncleByBlockNumberAndIndex(blockchainQueries),
        new EthGetUncleByBlockHashAndIndex(blockchainQueries),
        new EthNewBlockFilter(filterManager),
        new EthNewPendingTransactionFilter(filterManager),
        new EthNewFilter(filterManager),
        new EthGetTransactionByHash(blockchainQueries, transactionPool),
        new EthGetTransactionByBlockHashAndIndex(blockchainQueries),
        new EthGetTransactionByBlockNumberAndIndex(blockchainQueries),
        new EthGetTransactionCount(blockchainQueries, transactionPool),
        new EthGetTransactionReceipt(blockchainQueries, protocolSchedule),
        new EthUninstallFilter(filterManager),
        new EthGetFilterChanges(filterManager),
        new EthGetFilterLogs(filterManager),
        new EthSyncing(synchronizer),
        new EthGetStorageAt(blockchainQueries),
        new EthSendRawTransaction(transactionPool),
        new EthSendTransaction(),
        new EthEstimateGas(blockchainQueries, transactionSimulator),
        new EthCreateAccessList(blockchainQueries, transactionSimulator),
        new EthMining(miningCoordinator),
        new EthCoinbase(miningCoordinator),
        new EthProtocolVersion(supportedCapabilities),
        new EthGasPrice(blockchainQueries, apiConfiguration),
        new EthGetWork(miningCoordinator),
        new EthSubmitWork(miningCoordinator),
        new EthHashrate(miningCoordinator),
        new EthSubmitHashRate(miningCoordinator),
        new EthChainId(protocolSchedule.getChainId()),
        new EthGetMinerDataByBlockHash(blockchainQueries, protocolSchedule),
        new EthGetMinerDataByBlockNumber(blockchainQueries, protocolSchedule),
        new EthBlobBaseFee(blockchainQueries.getBlockchain(), protocolSchedule),
        new EthMaxPriorityFeePerGas(blockchainQueries));
  }
}
