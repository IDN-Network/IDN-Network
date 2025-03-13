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

import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.RpcModules;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.network.P2PNetwork;
import org.idnecology.idn.ethereum.p2p.peers.EnodeDnsConfiguration;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.nat.NatService;
import org.idnecology.idn.plugin.IdnPlugin;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Vertx;

public class JsonRpcMethodsFactory {

  public Map<String, JsonRpcMethod> methods(
      final String clientNodeName,
      final String clientVersion,
      final String commit,
      final BigInteger networkId,
      final GenesisConfigOptions genesisConfigOptions,
      final P2PNetwork p2pNetwork,
      final BlockchainQueries blockchainQueries,
      final Synchronizer synchronizer,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final FilterManager filterManager,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final MiningCoordinator miningCoordinator,
      final ObservableMetricsSystem metricsSystem,
      final Set<Capability> supportedCapabilities,
      final Optional<AccountLocalConfigPermissioningController> accountsAllowlistController,
      final Optional<NodeLocalConfigPermissioningController> nodeAllowlistController,
      final Collection<String> rpcApis,
      final PrivacyParameters privacyParameters,
      final JsonRpcConfiguration jsonRpcConfiguration,
      final WebSocketConfiguration webSocketConfiguration,
      final MetricsConfiguration metricsConfiguration,
      final GraphQLConfiguration graphQLConfiguration,
      final NatService natService,
      final Map<String, IdnPlugin> namedPlugins,
      final Path dataDir,
      final EthPeers ethPeers,
      final Vertx consensusEngineServer,
      final ApiConfiguration apiConfiguration,
      final Optional<EnodeDnsConfiguration> enodeDnsConfiguration,
      final TransactionSimulator transactionSimulator,
      final EthScheduler ethScheduler) {
    final Map<String, JsonRpcMethod> enabled = new HashMap<>();
    if (!rpcApis.isEmpty()) {
      final JsonRpcMethod modules = new RpcModules(rpcApis);
      enabled.put(modules.getName(), modules);
      final List<JsonRpcMethods> availableApiGroups =
          List.of(
              new AdminJsonRpcMethods(
                  clientNodeName,
                  networkId,
                  genesisConfigOptions,
                  p2pNetwork,
                  blockchainQueries,
                  namedPlugins,
                  natService,
                  ethPeers,
                  enodeDnsConfiguration,
                  protocolSchedule),
              new DebugJsonRpcMethods(
                  blockchainQueries,
                  protocolContext,
                  protocolSchedule,
                  metricsSystem,
                  transactionPool,
                  synchronizer,
                  dataDir,
                  transactionSimulator,
                  ethScheduler),
              new EeaJsonRpcMethods(
                  blockchainQueries, protocolSchedule, transactionPool, privacyParameters),
              new ExecutionEngineJsonRpcMethods(
                  miningCoordinator,
                  protocolSchedule,
                  protocolContext,
                  ethPeers,
                  consensusEngineServer,
                  clientVersion,
                  commit,
                  transactionPool,
                  metricsSystem),
              new EthJsonRpcMethods(
                  blockchainQueries,
                  synchronizer,
                  protocolSchedule,
                  filterManager,
                  transactionPool,
                  miningCoordinator,
                  supportedCapabilities,
                  apiConfiguration,
                  transactionSimulator),
              new NetJsonRpcMethods(
                  p2pNetwork,
                  networkId,
                  jsonRpcConfiguration,
                  webSocketConfiguration,
                  metricsConfiguration,
                  graphQLConfiguration),
              new MinerJsonRpcMethods(miningConfiguration, miningCoordinator),
              new PermJsonRpcMethods(accountsAllowlistController, nodeAllowlistController),
              new PrivJsonRpcMethods(
                  blockchainQueries,
                  protocolSchedule,
                  transactionPool,
                  privacyParameters,
                  filterManager),
              new PrivxJsonRpcMethods(
                  blockchainQueries, protocolSchedule, transactionPool, privacyParameters),
              new Web3JsonRpcMethods(clientNodeName),
              new TraceJsonRpcMethods(
                  blockchainQueries,
                  protocolSchedule,
                  protocolContext,
                  apiConfiguration,
                  transactionSimulator,
                  metricsSystem,
                  ethScheduler),
              new TxPoolJsonRpcMethods(transactionPool),
              new PluginsJsonRpcMethods(namedPlugins));

      for (final JsonRpcMethods apiGroup : availableApiGroups) {
        enabled.putAll(apiGroup.create(rpcApis));
      }
    }

    return enabled;
  }
}
