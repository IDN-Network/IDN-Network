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
package org.idnecology.idn.ethereum.api.jsonrpc;

import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManagerBuilder;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.JsonRpcMethodsFactory;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.blockcreation.PoWMiningCoordinator;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.p2p.network.P2PNetwork;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.nat.NatService;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/** Provides a facade to construct the JSON-RPC component. */
public class JsonRpcTestMethodsFactory {

  private static final String CLIENT_NODE_NAME = "TestClientVersion/0.1.0";
  private static final String CLIENT_VERSION = "0.1.0";
  private static final String CLIENT_COMMIT = "12345678";

  private final BlockchainImporter importer;
  private final MutableBlockchain blockchain;
  private final WorldStateArchive stateArchive;
  private final ProtocolContext context;
  private final BlockchainQueries blockchainQueries;
  private final Synchronizer synchronizer;
  private final ProtocolSchedule protocolSchedule;
  private final TransactionSimulator transactionSimulator;

  public JsonRpcTestMethodsFactory(final BlockchainImporter importer) {
    this.importer = importer;
    this.blockchain = createInMemoryBlockchain(importer.getGenesisBlock());
    this.stateArchive = createInMemoryWorldStateArchive();
    this.importer.getGenesisState().writeStateTo(stateArchive.getWorldState());
    this.context =
        new ProtocolContext(
            blockchain, stateArchive, mock(ConsensusContext.class), new BadBlockManager());

    this.protocolSchedule = importer.getProtocolSchedule();
    this.synchronizer = mock(Synchronizer.class);

    for (final Block block : importer.getBlocks()) {
      final ProtocolSpec protocolSpec = protocolSchedule.getByBlockHeader(block.getHeader());
      final BlockImporter blockImporter = protocolSpec.getBlockImporter();
      blockImporter.importBlock(context, block, HeaderValidationMode.FULL);
    }
    final var miningConfiguration = MiningConfiguration.newDefault();
    this.blockchainQueries =
        new BlockchainQueries(protocolSchedule, blockchain, stateArchive, miningConfiguration);

    this.transactionSimulator =
        new TransactionSimulator(
            blockchain, stateArchive, protocolSchedule, miningConfiguration, 0L);
  }

  public JsonRpcTestMethodsFactory(
      final BlockchainImporter importer,
      final MutableBlockchain blockchain,
      final WorldStateArchive stateArchive,
      final ProtocolContext context) {
    this.importer = importer;
    this.blockchain = blockchain;
    this.stateArchive = stateArchive;
    this.context = context;
    this.protocolSchedule = importer.getProtocolSchedule();
    final var miningConfiguration = MiningConfiguration.newDefault();
    this.blockchainQueries =
        new BlockchainQueries(
            importer.getProtocolSchedule(), blockchain, stateArchive, miningConfiguration);
    this.synchronizer = mock(Synchronizer.class);
    this.transactionSimulator =
        new TransactionSimulator(
            blockchain, stateArchive, protocolSchedule, miningConfiguration, 0L);
  }

  public JsonRpcTestMethodsFactory(
      final BlockchainImporter importer,
      final MutableBlockchain blockchain,
      final WorldStateArchive stateArchive,
      final ProtocolContext context,
      final Synchronizer synchronizer) {
    this.importer = importer;
    this.blockchain = blockchain;
    this.stateArchive = stateArchive;
    this.context = context;
    this.synchronizer = synchronizer;
    this.protocolSchedule = importer.getProtocolSchedule();
    final var miningConfiguration = MiningConfiguration.newDefault();
    this.blockchainQueries =
        new BlockchainQueries(
            importer.getProtocolSchedule(), blockchain, stateArchive, miningConfiguration);
    this.transactionSimulator =
        new TransactionSimulator(
            blockchain, stateArchive, protocolSchedule, miningConfiguration, 0L);
  }

  public BlockchainQueries getBlockchainQueries() {
    return blockchainQueries;
  }

  public WorldStateArchive getStateArchive() {
    return stateArchive;
  }

  public BigInteger getChainId() {
    return protocolSchedule.getChainId().get();
  }

  public Map<String, JsonRpcMethod> methods() {
    final P2PNetwork peerDiscovery = mock(P2PNetwork.class);
    final EthPeers ethPeers = mock(EthPeers.class);
    final TransactionPool transactionPool = mock(TransactionPool.class);
    final MiningConfiguration miningConfiguration = mock(MiningConfiguration.class);
    final PoWMiningCoordinator miningCoordinator = mock(PoWMiningCoordinator.class);
    final ObservableMetricsSystem metricsSystem = new NoOpMetricsSystem();
    final Optional<AccountLocalConfigPermissioningController> accountWhitelistController =
        Optional.of(mock(AccountLocalConfigPermissioningController.class));
    final Optional<NodeLocalConfigPermissioningController> nodeWhitelistController =
        Optional.of(mock(NodeLocalConfigPermissioningController.class));
    final PrivacyParameters privacyParameters = mock(PrivacyParameters.class);

    final FilterManager filterManager =
        new FilterManagerBuilder()
            .blockchainQueries(blockchainQueries)
            .transactionPool(transactionPool)
            .privacyParameters(privacyParameters)
            .build();

    final JsonRpcConfiguration jsonRpcConfiguration = mock(JsonRpcConfiguration.class);
    final WebSocketConfiguration webSocketConfiguration = mock(WebSocketConfiguration.class);
    final MetricsConfiguration metricsConfiguration = mock(MetricsConfiguration.class);
    final GraphQLConfiguration graphQLConfiguration = mock(GraphQLConfiguration.class);

    final NatService natService = new NatService(Optional.empty());

    final List<String> apis = new ArrayList<>();
    apis.add(RpcApis.ETH.name());
    apis.add(RpcApis.NET.name());
    apis.add(RpcApis.WEB3.name());
    apis.add(RpcApis.PRIV.name());
    apis.add(RpcApis.DEBUG.name());

    final Path dataDir = mock(Path.class);

    return new JsonRpcMethodsFactory()
        .methods(
            CLIENT_NODE_NAME,
            CLIENT_VERSION,
            CLIENT_COMMIT,
            getChainId(),
            new StubGenesisConfigOptions(),
            peerDiscovery,
            blockchainQueries,
            synchronizer,
            importer.getProtocolSchedule(),
            context,
            filterManager,
            transactionPool,
            miningConfiguration,
            miningCoordinator,
            metricsSystem,
            new HashSet<>(),
            accountWhitelistController,
            nodeWhitelistController,
            apis,
            privacyParameters,
            jsonRpcConfiguration,
            webSocketConfiguration,
            metricsConfiguration,
            graphQLConfiguration,
            natService,
            new HashMap<>(),
            dataDir,
            ethPeers,
            Vertx.vertx(new VertxOptions().setWorkerPoolSize(1)),
            ImmutableApiConfiguration.builder().build(),
            Optional.empty(),
            transactionSimulator,
            new DeterministicEthScheduler());
  }
}
