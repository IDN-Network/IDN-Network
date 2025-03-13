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
package org.idnecology.idn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.VARIABLES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.MergeConfiguration;
import org.idnecology.idn.consensus.common.bft.BftEventQueue;
import org.idnecology.idn.consensus.common.bft.network.PeerConnectionTracker;
import org.idnecology.idn.consensus.common.bft.protocol.BftProtocolManager;
import org.idnecology.idn.consensus.ibft.protocol.IbftSubProtocol;
import org.idnecology.idn.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.crypto.SECP256K1;
import org.idnecology.idn.cryptoservices.KeyPairSecurityModule;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.InProcessRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.ipc.JsonRpcIpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.DefaultBlockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.p2p.config.SubProtocolConfiguration;
import org.idnecology.idn.ethereum.p2p.peers.EnodeURLImpl;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.nat.NatMethod;
import org.idnecology.idn.plugin.data.EnodeURL;
import org.idnecology.idn.services.IdnPluginContextImpl;
import org.idnecology.idn.services.PermissioningServiceImpl;
import org.idnecology.idn.services.RpcEndpointServiceImpl;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.ethereum.beacon.discovery.schema.NodeRecordFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public final class RunnerBuilderTest {

  @TempDir private Path dataDir;

  @Mock IdnController idnController;
  @Mock ProtocolSchedule protocolSchedule;
  @Mock ProtocolContext protocolContext;
  @Mock WorldStateArchive worldstateArchive;
  @Mock Vertx vertx;
  private NodeKey nodeKey;

  @BeforeEach
  public void setup() {
    final SubProtocolConfiguration subProtocolConfiguration = mock(SubProtocolConfiguration.class);
    final EthProtocolManager ethProtocolManager = mock(EthProtocolManager.class);
    final EthContext ethContext = mock(EthContext.class);
    nodeKey = new NodeKey(new KeyPairSecurityModule(new SECP256K1().generateKeyPair()));

    when(subProtocolConfiguration.getProtocolManagers())
        .thenReturn(
            Collections.singletonList(
                new BftProtocolManager(
                    mock(BftEventQueue.class),
                    mock(PeerConnectionTracker.class),
                    IbftSubProtocol.IBFV1,
                    IbftSubProtocol.get().getName())));
    when(ethContext.getScheduler()).thenReturn(mock(EthScheduler.class));
    when(ethProtocolManager.ethContext()).thenReturn(ethContext);
    when(subProtocolConfiguration.getSubProtocols())
        .thenReturn(Collections.singletonList(new IbftSubProtocol()));

    when(protocolContext.getWorldStateArchive()).thenReturn(worldstateArchive);
    when(idnController.getProtocolManager()).thenReturn(ethProtocolManager);
    when(idnController.getSubProtocolConfiguration()).thenReturn(subProtocolConfiguration);
    when(idnController.getProtocolContext()).thenReturn(protocolContext);
    when(idnController.getProtocolSchedule()).thenReturn(protocolSchedule);
    when(idnController.getNodeKey()).thenReturn(nodeKey);
    when(idnController.getMiningParameters()).thenReturn(mock(MiningConfiguration.class));
    when(idnController.getPrivacyParameters()).thenReturn(mock(PrivacyParameters.class));
    when(idnController.getTransactionPool())
        .thenReturn(mock(TransactionPool.class, RETURNS_DEEP_STUBS));
    when(idnController.getSynchronizer()).thenReturn(mock(Synchronizer.class));
    when(idnController.getMiningCoordinator()).thenReturn(mock(MiningCoordinator.class));
    when(idnController.getMiningCoordinator()).thenReturn(mock(MergeMiningCoordinator.class));
    when(idnController.getEthPeers()).thenReturn(mock(EthPeers.class));
    final GenesisConfigOptions genesisConfigOptions = mock(GenesisConfigOptions.class);
    when(genesisConfigOptions.getForkBlockNumbers()).thenReturn(Collections.emptyList());
    when(genesisConfigOptions.getForkBlockTimestamps()).thenReturn(Collections.emptyList());
    when(idnController.getGenesisConfigOptions()).thenReturn(genesisConfigOptions);
  }

  @Test
  public void enodeUrlShouldHaveAdvertisedHostWhenDiscoveryDisabled() {
    setupBlockchainAndBlock();

    final String p2pAdvertisedHost = "172.0.0.1";
    final int p2pListenPort = 30302;

    final Runner runner =
        new RunnerBuilder()
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(p2pListenPort)
            .p2pAdvertisedHost(p2pAdvertisedHost)
            .p2pEnabled(true)
            .discoveryEnabled(false)
            .idnController(idnController)
            .ethNetworkConfig(mock(EthNetworkConfig.class))
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .jsonRpcConfiguration(mock(JsonRpcConfiguration.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .webSocketConfiguration(mock(WebSocketConfiguration.class))
            .jsonRpcIpcConfiguration(mock(JsonRpcIpcConfiguration.class))
            .inProcessRpcConfiguration(mock(InProcessRpcConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(vertx)
            .dataDir(dataDir.getRoot())
            .storageProvider(mock(KeyValueStorageProvider.class, RETURNS_DEEP_STUBS))
            .rpcEndpointService(new RpcEndpointServiceImpl())
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();
    runner.startEthereumMainLoop();

    final EnodeURL expectedEnodeURL =
        EnodeURLImpl.builder()
            .ipAddress(p2pAdvertisedHost)
            .discoveryPort(0)
            .listeningPort(p2pListenPort)
            .nodeId(nodeKey.getPublicKey().getEncoded())
            .build();
    assertThat(runner.getLocalEnode().orElseThrow()).isEqualTo(expectedEnodeURL);
  }

  @Test
  public void movingAcrossProtocolSpecsUpdatesNodeRecord() {
    final BlockDataGenerator gen = new BlockDataGenerator();
    final String p2pAdvertisedHost = "172.0.0.1";
    final int p2pListenPort = 30301;
    final StorageProvider storageProvider = new InMemoryKeyValueStorageProvider();
    final Block genesisBlock = gen.genesisBlock();
    final MutableBlockchain inMemoryBlockchain =
        createInMemoryBlockchain(genesisBlock, new MainnetBlockHeaderFunctions());
    when(protocolContext.getBlockchain()).thenReturn(inMemoryBlockchain);
    final Runner runner =
        new RunnerBuilder()
            .discoveryEnabled(true)
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(p2pListenPort)
            .p2pAdvertisedHost(p2pAdvertisedHost)
            .p2pEnabled(true)
            .natMethod(NatMethod.NONE)
            .idnController(idnController)
            .ethNetworkConfig(mock(EthNetworkConfig.class))
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .jsonRpcConfiguration(mock(JsonRpcConfiguration.class))
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .webSocketConfiguration(mock(WebSocketConfiguration.class))
            .jsonRpcIpcConfiguration(mock(JsonRpcIpcConfiguration.class))
            .inProcessRpcConfiguration(mock(InProcessRpcConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(Vertx.vertx())
            .dataDir(dataDir.getRoot())
            .storageProvider(storageProvider)
            .rpcEndpointService(new RpcEndpointServiceImpl())
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();
    runner.startEthereumMainLoop();

    when(protocolSchedule.isOnMilestoneBoundary(any(BlockHeader.class))).thenReturn(true);

    for (int i = 0; i < 2; ++i) {
      final Block block =
          gen.block(
              BlockDataGenerator.BlockOptions.create()
                  .setBlockNumber(1 + i)
                  .setParentHash(inMemoryBlockchain.getChainHeadHash()));
      inMemoryBlockchain.appendBlock(block, gen.receipts(block));
      assertThat(
              storageProvider
                  .getStorageBySegmentIdentifier(VARIABLES)
                  .get("local-enr-seqno".getBytes(StandardCharsets.UTF_8))
                  .map(Bytes::of)
                  .map(NodeRecordFactory.DEFAULT::fromBytes)
                  .map(NodeRecord::getSeq))
          .contains(UInt64.valueOf(2 + i));
    }
  }

  @Test
  public void whenEngineApiAddedListensOnDefaultPort() {
    setupBlockchainAndBlock();

    final JsonRpcConfiguration jrpc = JsonRpcConfiguration.createDefault();
    jrpc.setEnabled(true);
    final JsonRpcConfiguration engine = JsonRpcConfiguration.createEngineDefault();
    engine.setEnabled(true);
    final EthNetworkConfig mockMainnet = mock(EthNetworkConfig.class);
    when(mockMainnet.networkId()).thenReturn(BigInteger.ONE);
    MergeConfiguration.setMergeEnabled(true);
    when(idnController.getMiningCoordinator()).thenReturn(mock(MergeMiningCoordinator.class));

    final Runner runner =
        new RunnerBuilder()
            .discoveryEnabled(true)
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(30303)
            .p2pAdvertisedHost("127.0.0.1")
            .p2pEnabled(true)
            .natMethod(NatMethod.NONE)
            .idnController(idnController)
            .ethNetworkConfig(mockMainnet)
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .jsonRpcConfiguration(jrpc)
            .engineJsonRpcConfiguration(engine)
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .webSocketConfiguration(mock(WebSocketConfiguration.class))
            .jsonRpcIpcConfiguration(mock(JsonRpcIpcConfiguration.class))
            .inProcessRpcConfiguration(mock(InProcessRpcConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(Vertx.vertx())
            .dataDir(dataDir.getRoot())
            .storageProvider(mock(KeyValueStorageProvider.class, RETURNS_DEEP_STUBS))
            .rpcEndpointService(new RpcEndpointServiceImpl())
            .idnPluginContext(mock(IdnPluginContextImpl.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();

    assertThat(runner.getJsonRpcPort()).isPresent();
    assertThat(runner.getEngineJsonRpcPort()).isPresent();
  }

  @Test
  public void whenEngineApiAddedWebSocketReadyOnSamePort() {
    setupBlockchainAndBlock();

    final WebSocketConfiguration wsRpc = WebSocketConfiguration.createDefault();
    wsRpc.setEnabled(true);
    final EthNetworkConfig mockMainnet = mock(EthNetworkConfig.class);
    when(mockMainnet.networkId()).thenReturn(BigInteger.ONE);
    MergeConfiguration.setMergeEnabled(true);
    when(idnController.getMiningCoordinator()).thenReturn(mock(MergeMiningCoordinator.class));
    final JsonRpcConfiguration engineConf = JsonRpcConfiguration.createEngineDefault();
    engineConf.setEnabled(true);

    final Runner runner =
        new RunnerBuilder()
            .discoveryEnabled(true)
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(30303)
            .p2pAdvertisedHost("127.0.0.1")
            .p2pEnabled(true)
            .natMethod(NatMethod.NONE)
            .idnController(idnController)
            .ethNetworkConfig(mockMainnet)
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .jsonRpcConfiguration(JsonRpcConfiguration.createDefault())
            .engineJsonRpcConfiguration(engineConf)
            .webSocketConfiguration(wsRpc)
            .jsonRpcIpcConfiguration(mock(JsonRpcIpcConfiguration.class))
            .inProcessRpcConfiguration(mock(InProcessRpcConfiguration.class))
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(Vertx.vertx())
            .dataDir(dataDir.getRoot())
            .storageProvider(mock(KeyValueStorageProvider.class, RETURNS_DEEP_STUBS))
            .rpcEndpointService(new RpcEndpointServiceImpl())
            .idnPluginContext(mock(IdnPluginContextImpl.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();

    assertThat(runner.getEngineJsonRpcPort()).isPresent();
  }

  @Test
  public void whenEngineApiAddedEthSubscribeAvailable() {
    setupBlockchainAndBlock();

    final WebSocketConfiguration wsRpc = WebSocketConfiguration.createDefault();
    wsRpc.setEnabled(true);
    final EthNetworkConfig mockMainnet = mock(EthNetworkConfig.class);
    when(mockMainnet.networkId()).thenReturn(BigInteger.ONE);
    MergeConfiguration.setMergeEnabled(true);
    when(idnController.getMiningCoordinator()).thenReturn(mock(MergeMiningCoordinator.class));
    final JsonRpcConfiguration engineConf = JsonRpcConfiguration.createEngineDefault();
    engineConf.setEnabled(true);

    final Runner runner =
        new RunnerBuilder()
            .discoveryEnabled(true)
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(30303)
            .p2pAdvertisedHost("127.0.0.1")
            .p2pEnabled(true)
            .natMethod(NatMethod.NONE)
            .idnController(idnController)
            .ethNetworkConfig(mockMainnet)
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .jsonRpcConfiguration(JsonRpcConfiguration.createDefault())
            .engineJsonRpcConfiguration(engineConf)
            .webSocketConfiguration(wsRpc)
            .jsonRpcIpcConfiguration(mock(JsonRpcIpcConfiguration.class))
            .inProcessRpcConfiguration(mock(InProcessRpcConfiguration.class))
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(Vertx.vertx())
            .dataDir(dataDir.getRoot())
            .storageProvider(mock(KeyValueStorageProvider.class, RETURNS_DEEP_STUBS))
            .rpcEndpointService(new RpcEndpointServiceImpl())
            .idnPluginContext(mock(IdnPluginContextImpl.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();

    assertThat(runner.getEngineJsonRpcPort()).isPresent();
    runner.startExternalServices();
    // assert that rpc method collection has eth_subscribe in it.
    runner.stop();
  }

  @Test
  public void noEngineApiNoServiceForMethods() {
    setupBlockchainAndBlock();

    final JsonRpcConfiguration defaultRpcConfig = JsonRpcConfiguration.createDefault();
    defaultRpcConfig.setEnabled(true);
    final WebSocketConfiguration defaultWebSockConfig = WebSocketConfiguration.createDefault();
    defaultWebSockConfig.setEnabled(true);
    final EthNetworkConfig mockMainnet = mock(EthNetworkConfig.class);
    when(mockMainnet.networkId()).thenReturn(BigInteger.ONE);
    MergeConfiguration.setMergeEnabled(true);

    final Runner runner =
        new RunnerBuilder()
            .discoveryEnabled(true)
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(30303)
            .p2pAdvertisedHost("127.0.0.1")
            .p2pEnabled(true)
            .natMethod(NatMethod.NONE)
            .idnController(idnController)
            .ethNetworkConfig(mockMainnet)
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .jsonRpcConfiguration(defaultRpcConfig)
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .webSocketConfiguration(defaultWebSockConfig)
            .jsonRpcIpcConfiguration(mock(JsonRpcIpcConfiguration.class))
            .inProcessRpcConfiguration(mock(InProcessRpcConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(Vertx.vertx())
            .dataDir(dataDir.getRoot())
            .storageProvider(mock(KeyValueStorageProvider.class, RETURNS_DEEP_STUBS))
            .rpcEndpointService(new RpcEndpointServiceImpl())
            .idnPluginContext(mock(IdnPluginContextImpl.class))
            .networkingConfiguration(NetworkingConfiguration.create())
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();

    assertThat(runner.getJsonRpcPort()).isPresent();
    assertThat(runner.getEngineJsonRpcPort()).isEmpty();
  }

  private void setupBlockchainAndBlock() {
    final DefaultBlockchain blockchain = mock(DefaultBlockchain.class);
    when(protocolContext.getBlockchain()).thenReturn(blockchain);
    final Block block = mock(Block.class);
    when(blockchain.getGenesisBlock()).thenReturn(block);
    when(block.getHash()).thenReturn(Hash.ZERO);
  }
}
