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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivGetFilterChanges;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivGetFilterLogs;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivUninstallFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivCall;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivCreatePrivacyGroup;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivDebugGetStateRoot;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivDeletePrivacyGroup;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivDistributeRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivFindPrivacyGroup;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetCode;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetLogs;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetPrivacyPrecompileAddress;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetPrivateTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetTransactionCount;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetTransactionReceipt;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivNewFilter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivTraceTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.privateProcessor.PrivateBlockReplay;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.processor.privateProcessor.PrivateBlockTracer;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import java.util.Map;

@Deprecated(since = "24.12.0")
public class PrivJsonRpcMethods extends PrivacyApiGroupJsonRpcMethods {

  private final FilterManager filterManager;

  public PrivJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final PrivacyParameters privacyParameters,
      final FilterManager filterManager) {
    super(blockchainQueries, protocolSchedule, transactionPool, privacyParameters);
    this.filterManager = filterManager;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.PRIV.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create(
      final PrivacyController privacyController,
      final PrivacyIdProvider privacyIdProvider,
      final PrivateMarkerTransactionFactory privateMarkerTransactionFactory) {

    final PrivateBlockReplay blockReplay =
        new PrivateBlockReplay(
            getProtocolSchedule(), getBlockchainQueries().getBlockchain(), privacyController);
    final Map<String, JsonRpcMethod> RPC_METHODS =
        mapOf(
            new PrivCall(getBlockchainQueries(), privacyController, privacyIdProvider),
            new PrivDebugGetStateRoot(getBlockchainQueries(), privacyIdProvider, privacyController),
            new PrivDistributeRawTransaction(
                privacyController,
                privacyIdProvider,
                getPrivacyParameters().isFlexiblePrivacyGroupsEnabled()),
            new PrivGetCode(getBlockchainQueries(), privacyController, privacyIdProvider),
            new PrivGetLogs(
                getBlockchainQueries(), getPrivacyQueries(), privacyController, privacyIdProvider),
            new PrivGetPrivateTransaction(privacyController, privacyIdProvider),
            new PrivGetPrivacyPrecompileAddress(getPrivacyParameters()),
            new PrivGetTransactionCount(privacyController, privacyIdProvider),
            new PrivGetTransactionReceipt(
                getPrivacyParameters().getPrivateStateStorage(),
                privacyController,
                privacyIdProvider),
            new PrivGetFilterLogs(filterManager, privacyController, privacyIdProvider),
            new PrivGetFilterChanges(filterManager, privacyController, privacyIdProvider),
            new PrivNewFilter(filterManager, privacyController, privacyIdProvider),
            new PrivUninstallFilter(filterManager, privacyController, privacyIdProvider),
            new PrivTraceTransaction(
                () -> new PrivateBlockTracer(blockReplay),
                getBlockchainQueries(),
                getProtocolSchedule(),
                getPrivacyQueries(),
                privacyController,
                getPrivacyParameters(),
                privacyIdProvider));

    if (!getPrivacyParameters().isFlexiblePrivacyGroupsEnabled()) {
      final Map<String, JsonRpcMethod> OFFCHAIN_METHODS =
          mapOf(
              new PrivCreatePrivacyGroup(privacyController, privacyIdProvider),
              new PrivDeletePrivacyGroup(privacyController, privacyIdProvider),
              new PrivFindPrivacyGroup(privacyController, privacyIdProvider));
      OFFCHAIN_METHODS.forEach(
          (key, jsonRpcMethod) ->
              RPC_METHODS.merge(key, jsonRpcMethod, (oldVal, newVal) -> newVal));
    }
    return RPC_METHODS;
  }
}
