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
package org.idnecology.idn;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;
import static org.idnecology.idn.controller.IdnController.CACHE_PATH;
import static org.idnecology.idn.ethereum.core.PrivacyParameters.FLEXIBLE_PRIVACY;

import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.cli.config.NetworkName;
import org.idnecology.idn.cli.options.EthstatsOptions;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLContextType;
import org.idnecology.idn.ethereum.api.graphql.GraphQLDataFetchers;
import org.idnecology.idn.ethereum.api.graphql.GraphQLHttpService;
import org.idnecology.idn.ethereum.api.graphql.GraphQLProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.EngineJsonRpcService;
import org.idnecology.idn.ethereum.api.jsonrpc.InProcessRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcHttpService;
import org.idnecology.idn.ethereum.api.jsonrpc.authentication.AuthenticationService;
import org.idnecology.idn.ethereum.api.jsonrpc.authentication.DefaultAuthenticationService;
import org.idnecology.idn.ethereum.api.jsonrpc.authentication.EngineAuthService;
import org.idnecology.idn.ethereum.api.jsonrpc.execution.AuthenticatedJsonRpcProcessor;
import org.idnecology.idn.ethereum.api.jsonrpc.execution.BaseJsonRpcProcessor;
import org.idnecology.idn.ethereum.api.jsonrpc.execution.JsonRpcExecutor;
import org.idnecology.idn.ethereum.api.jsonrpc.execution.JsonRpcProcessor;
import org.idnecology.idn.ethereum.api.jsonrpc.health.HealthService;
import org.idnecology.idn.ethereum.api.jsonrpc.health.LivenessCheck;
import org.idnecology.idn.ethereum.api.jsonrpc.health.ReadinessCheck;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManagerBuilder;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.ipc.JsonRpcIpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.ipc.JsonRpcIpcService;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.JsonRpcMethodsFactory;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketMessageHandler;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketService;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.methods.PrivateWebSocketMethodsFactory;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.methods.WebSocketMethodsFactory;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.blockheaders.NewBlockHeadersSubscriptionService;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.logs.LogsSubscriptionService;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.pending.PendingTransactionDroppedSubscriptionService;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.pending.PendingTransactionSubscriptionService;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.syncing.SyncingSubscriptionService;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.query.PrivacyQueries;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.blockcreation.PoWMiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.precompiles.privacy.FlexiblePrivacyPrecompiledContract;
import org.idnecology.idn.ethereum.p2p.config.DiscoveryConfiguration;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.p2p.config.RlpxConfiguration;
import org.idnecology.idn.ethereum.p2p.config.SubProtocolConfiguration;
import org.idnecology.idn.ethereum.p2p.network.DefaultP2PNetwork;
import org.idnecology.idn.ethereum.p2p.network.NetworkRunner;
import org.idnecology.idn.ethereum.p2p.network.NetworkRunner.NetworkBuilder;
import org.idnecology.idn.ethereum.p2p.network.NoopP2PNetwork;
import org.idnecology.idn.ethereum.p2p.network.P2PNetwork;
import org.idnecology.idn.ethereum.p2p.network.ProtocolManager;
import org.idnecology.idn.ethereum.p2p.peers.DefaultPeer;
import org.idnecology.idn.ethereum.p2p.peers.EnodeDnsConfiguration;
import org.idnecology.idn.ethereum.p2p.permissions.PeerPermissionSubnet;
import org.idnecology.idn.ethereum.p2p.permissions.PeerPermissions;
import org.idnecology.idn.ethereum.p2p.permissions.PeerPermissionsDenylist;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.SubProtocol;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodePermissioningControllerFactory;
import org.idnecology.idn.ethereum.permissioning.PermissioningConfiguration;
import org.idnecology.idn.ethereum.permissioning.account.AccountPermissioningController;
import org.idnecology.idn.ethereum.permissioning.account.AccountPermissioningControllerFactory;
import org.idnecology.idn.ethereum.permissioning.node.InsufficientPeersPermissioningProvider;
import org.idnecology.idn.ethereum.permissioning.node.NodePermissioningController;
import org.idnecology.idn.ethereum.permissioning.node.PeerPermissionsAdapter;
import org.idnecology.idn.ethereum.privacy.PrivateTransactionObserver;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.stratum.StratumServer;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethstats.EthStatsService;
import org.idnecology.idn.ethstats.util.EthStatsConnectOptions;
import org.idnecology.idn.metrics.MetricsService;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.nat.NatMethod;
import org.idnecology.idn.nat.NatService;
import org.idnecology.idn.nat.core.NatManager;
import org.idnecology.idn.nat.docker.DockerDetector;
import org.idnecology.idn.nat.docker.DockerNatManager;
import org.idnecology.idn.nat.upnp.UpnpNatManager;
import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.data.EnodeURL;
import org.idnecology.idn.services.IdnPluginContextImpl;
import org.idnecology.idn.services.PermissioningServiceImpl;
import org.idnecology.idn.services.RpcEndpointServiceImpl;
import org.idnecology.idn.util.NetworkUtility;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import graphql.GraphQL;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The builder for Runner class. */
public class RunnerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(RunnerBuilder.class);

  private Vertx vertx;
  private IdnController idnController;

  private NetworkingConfiguration networkingConfiguration = NetworkingConfiguration.create();
  private final Collection<Bytes> bannedNodeIds = new ArrayList<>();
  private boolean p2pEnabled = true;
  private boolean discoveryEnabled;
  private String p2pAdvertisedHost;
  private String p2pListenInterface = NetworkUtility.INADDR_ANY;
  private int p2pListenPort;
  private NatMethod natMethod = NatMethod.AUTO;
  private boolean natMethodFallbackEnabled;
  private EthNetworkConfig ethNetworkConfig;
  private EthstatsOptions ethstatsOptions;
  private JsonRpcConfiguration jsonRpcConfiguration;
  private Optional<JsonRpcConfiguration> engineJsonRpcConfiguration = Optional.empty();
  private GraphQLConfiguration graphQLConfiguration;
  private WebSocketConfiguration webSocketConfiguration;
  private InProcessRpcConfiguration inProcessRpcConfiguration;
  private ApiConfiguration apiConfiguration;
  private Path dataDir;
  private Optional<Path> pidPath = Optional.empty();
  private MetricsConfiguration metricsConfiguration;
  private ObservableMetricsSystem metricsSystem;
  private PermissioningServiceImpl permissioningService;
  private Optional<PermissioningConfiguration> permissioningConfiguration = Optional.empty();
  private Collection<EnodeURL> staticNodes = Collections.emptyList();
  private Optional<String> identityString = Optional.empty();
  private IdnPluginContextImpl idnPluginContext;
  private boolean autoLogBloomCaching = true;
  private StorageProvider storageProvider;
  private RpcEndpointServiceImpl rpcEndpointServiceImpl;
  private JsonRpcIpcConfiguration jsonRpcIpcConfiguration;
  private boolean legacyForkIdEnabled;
  private Optional<EnodeDnsConfiguration> enodeDnsConfiguration;
  private List<SubnetInfo> allowedSubnets = new ArrayList<>();
  private boolean poaDiscoveryRetryBootnodes = true;

  /** Instantiates a new Runner builder. */
  public RunnerBuilder() {}

  /**
   * Add Vertx.
   *
   * @param vertx the vertx instance
   * @return runner builder
   */
  public RunnerBuilder vertx(final Vertx vertx) {
    this.vertx = vertx;
    return this;
  }

  /**
   * Add Idn controller.
   *
   * @param idnController the idn controller
   * @return the runner builder
   */
  public RunnerBuilder idnController(final IdnController idnController) {
    this.idnController = idnController;
    return this;
  }

  /**
   * P2p enabled.
   *
   * @param p2pEnabled the p 2 p enabled
   * @return the runner builder
   */
  public RunnerBuilder p2pEnabled(final boolean p2pEnabled) {
    this.p2pEnabled = p2pEnabled;
    return this;
  }

  /**
   * Enable Discovery.
   *
   * @param discoveryEnabled the discoveryEnabled
   * @return the runner builder
   */
  public RunnerBuilder discoveryEnabled(final boolean discoveryEnabled) {
    this.discoveryEnabled = discoveryEnabled;
    return this;
  }

  /**
   * Add Eth network config.
   *
   * @param ethNetworkConfig the eth network config
   * @return the runner builder
   */
  public RunnerBuilder ethNetworkConfig(final EthNetworkConfig ethNetworkConfig) {
    this.ethNetworkConfig = ethNetworkConfig;
    return this;
  }

  /**
   * Add Networking configuration.
   *
   * @param networkingConfiguration the networking configuration
   * @return the runner builder
   */
  public RunnerBuilder networkingConfiguration(
      final NetworkingConfiguration networkingConfiguration) {
    this.networkingConfiguration = networkingConfiguration;
    return this;
  }

  /**
   * Add P2p advertised host.
   *
   * @param p2pAdvertisedHost the P2P advertised host
   * @return the runner builder
   */
  public RunnerBuilder p2pAdvertisedHost(final String p2pAdvertisedHost) {
    this.p2pAdvertisedHost = p2pAdvertisedHost;
    return this;
  }

  /**
   * Add P2P Listener interface ip/host name.
   *
   * @param ip the ip
   * @return the runner builder
   */
  public RunnerBuilder p2pListenInterface(final String ip) {
    checkArgument(!isNull(ip), "Invalid null value supplied for p2pListenInterface");
    this.p2pListenInterface = ip;
    return this;
  }

  /**
   * Add P2P listen port.
   *
   * @param p2pListenPort the p 2 p listen port
   * @return the runner builder
   */
  public RunnerBuilder p2pListenPort(final int p2pListenPort) {
    this.p2pListenPort = p2pListenPort;
    return this;
  }

  /**
   * Add Nat method.
   *
   * @param natMethod the nat method
   * @return the runner builder
   */
  public RunnerBuilder natMethod(final NatMethod natMethod) {
    this.natMethod = natMethod;
    return this;
  }

  /**
   * Enable Nat method fallback.
   *
   * @param natMethodFallbackEnabled the nat method fallback enabled
   * @return the runner builder
   */
  public RunnerBuilder natMethodFallbackEnabled(final boolean natMethodFallbackEnabled) {
    this.natMethodFallbackEnabled = natMethodFallbackEnabled;
    return this;
  }

  /**
   * Add EthStatsOptions
   *
   * @param ethstatsOptions the ethstats options
   * @return Runner builder instance
   */
  public RunnerBuilder ethstatsOptions(final EthstatsOptions ethstatsOptions) {
    this.ethstatsOptions = ethstatsOptions;
    return this;
  }

  /**
   * Add Json RPC configuration.
   *
   * @param jsonRpcConfiguration the json rpc configuration
   * @return the runner builder
   */
  public RunnerBuilder jsonRpcConfiguration(final JsonRpcConfiguration jsonRpcConfiguration) {
    this.jsonRpcConfiguration = jsonRpcConfiguration;
    return this;
  }

  /**
   * Add Engine json RPC configuration.
   *
   * @param engineJsonRpcConfiguration the engine json rpc configuration
   * @return the runner builder
   */
  public RunnerBuilder engineJsonRpcConfiguration(
      final JsonRpcConfiguration engineJsonRpcConfiguration) {
    this.engineJsonRpcConfiguration = Optional.ofNullable(engineJsonRpcConfiguration);
    return this;
  }

  /**
   * Add GraphQl configuration.
   *
   * @param graphQLConfiguration the graph ql configuration
   * @return the runner builder
   */
  public RunnerBuilder graphQLConfiguration(final GraphQLConfiguration graphQLConfiguration) {
    this.graphQLConfiguration = graphQLConfiguration;
    return this;
  }

  /**
   * Add Web socket configuration.
   *
   * @param webSocketConfiguration the web socket configuration
   * @return the runner builder
   */
  public RunnerBuilder webSocketConfiguration(final WebSocketConfiguration webSocketConfiguration) {
    this.webSocketConfiguration = webSocketConfiguration;
    return this;
  }

  /**
   * Add In-Process RPC configuration.
   *
   * @param inProcessRpcConfiguration the in-process RPC configuration
   * @return the runner builder
   */
  public RunnerBuilder inProcessRpcConfiguration(
      final InProcessRpcConfiguration inProcessRpcConfiguration) {
    this.inProcessRpcConfiguration = inProcessRpcConfiguration;
    return this;
  }

  /**
   * Add Api configuration.
   *
   * @param apiConfiguration the api configuration
   * @return the runner builder
   */
  public RunnerBuilder apiConfiguration(final ApiConfiguration apiConfiguration) {
    this.apiConfiguration = apiConfiguration;
    return this;
  }

  /**
   * Add Permissioning configuration.
   *
   * @param permissioningConfiguration the permissioning configuration
   * @return the runner builder
   */
  public RunnerBuilder permissioningConfiguration(
      final Optional<PermissioningConfiguration> permissioningConfiguration) {
    this.permissioningConfiguration = permissioningConfiguration;
    return this;
  }

  /**
   * Add pid path.
   *
   * @param pidPath the pid path
   * @return the runner builder
   */
  public RunnerBuilder pidPath(final Path pidPath) {
    this.pidPath = Optional.ofNullable(pidPath);
    return this;
  }

  /**
   * Add Data dir.
   *
   * @param dataDir the data dir
   * @return the runner builder
   */
  public RunnerBuilder dataDir(final Path dataDir) {
    this.dataDir = dataDir;
    return this;
  }

  /**
   * Add list of Banned node id.
   *
   * @param bannedNodeIds the banned node ids
   * @return the runner builder
   */
  public RunnerBuilder bannedNodeIds(final Collection<Bytes> bannedNodeIds) {
    this.bannedNodeIds.addAll(bannedNodeIds);
    return this;
  }

  /**
   * Add Metrics configuration.
   *
   * @param metricsConfiguration the metrics configuration
   * @return the runner builder
   */
  public RunnerBuilder metricsConfiguration(final MetricsConfiguration metricsConfiguration) {
    this.metricsConfiguration = metricsConfiguration;
    return this;
  }

  /**
   * Add Metrics system.
   *
   * @param metricsSystem the metrics system
   * @return the runner builder
   */
  public RunnerBuilder metricsSystem(final ObservableMetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
    return this;
  }

  /**
   * Add Permissioning service.
   *
   * @param permissioningService the permissioning service
   * @return the runner builder
   */
  public RunnerBuilder permissioningService(final PermissioningServiceImpl permissioningService) {
    this.permissioningService = permissioningService;
    return this;
  }

  /**
   * Add Static nodes collection.
   *
   * @param staticNodes the static nodes
   * @return the runner builder
   */
  public RunnerBuilder staticNodes(final Collection<EnodeURL> staticNodes) {
    this.staticNodes = staticNodes;
    return this;
  }

  /**
   * Add Node identity string.
   *
   * @param identityString the identity string
   * @return the runner builder
   */
  public RunnerBuilder identityString(final Optional<String> identityString) {
    this.identityString = identityString;
    return this;
  }

  /**
   * Add Idn plugin context.
   *
   * @param idnPluginContext the idn plugin context
   * @return the runner builder
   */
  public RunnerBuilder idnPluginContext(final IdnPluginContextImpl idnPluginContext) {
    this.idnPluginContext = idnPluginContext;
    return this;
  }

  /**
   * Enable Auto log bloom caching.
   *
   * @param autoLogBloomCaching the auto log bloom caching
   * @return the runner builder
   */
  public RunnerBuilder autoLogBloomCaching(final boolean autoLogBloomCaching) {
    this.autoLogBloomCaching = autoLogBloomCaching;
    return this;
  }

  /**
   * Add Storage provider.
   *
   * @param storageProvider the storage provider
   * @return the runner builder
   */
  public RunnerBuilder storageProvider(final StorageProvider storageProvider) {
    this.storageProvider = storageProvider;
    return this;
  }

  /**
   * Add Rpc endpoint service.
   *
   * @param rpcEndpointService the rpc endpoint service
   * @return the runner builder
   */
  public RunnerBuilder rpcEndpointService(final RpcEndpointServiceImpl rpcEndpointService) {
    this.rpcEndpointServiceImpl = rpcEndpointService;
    return this;
  }

  /**
   * Add Json Rpc Ipc configuration.
   *
   * @param jsonRpcIpcConfiguration the json rpc ipc configuration
   * @return the runner builder
   */
  public RunnerBuilder jsonRpcIpcConfiguration(
      final JsonRpcIpcConfiguration jsonRpcIpcConfiguration) {
    this.jsonRpcIpcConfiguration = jsonRpcIpcConfiguration;
    return this;
  }

  /**
   * Add enode DNS configuration
   *
   * @param enodeDnsConfiguration the DNS configuration for enodes
   * @return the runner builder
   */
  public RunnerBuilder enodeDnsConfiguration(final EnodeDnsConfiguration enodeDnsConfiguration) {
    this.enodeDnsConfiguration =
        enodeDnsConfiguration != null ? Optional.of(enodeDnsConfiguration) : Optional.empty();
    return this;
  }

  /**
   * Add subnet configuration
   *
   * @param allowedSubnets the allowedSubnets
   * @return the runner builder
   */
  public RunnerBuilder allowedSubnets(final List<SubnetInfo> allowedSubnets) {
    this.allowedSubnets = allowedSubnets;
    return this;
  }

  /**
   * Flag to indicate if peer table refreshes should always query bootnodes
   *
   * @param poaDiscoveryRetryBootnodes whether to always query bootnodes
   * @return the runner builder
   */
  public RunnerBuilder poaDiscoveryRetryBootnodes(final boolean poaDiscoveryRetryBootnodes) {
    this.poaDiscoveryRetryBootnodes = poaDiscoveryRetryBootnodes;
    return this;
  }

  /**
   * Build Runner instance.
   *
   * @return the runner
   */
  public Runner build() {

    Preconditions.checkNotNull(idnController);

    final DiscoveryConfiguration discoveryConfiguration =
        DiscoveryConfiguration.create()
            .setBindHost(p2pListenInterface)
            .setBindPort(p2pListenPort)
            .setAdvertisedHost(p2pAdvertisedHost);
    if (discoveryEnabled) {
      final List<EnodeURL> bootstrap;
      if (ethNetworkConfig.bootNodes() == null) {
        bootstrap = EthNetworkConfig.getNetworkConfig(NetworkName.MAINNET).bootNodes();
      } else {
        bootstrap = ethNetworkConfig.bootNodes();
      }
      discoveryConfiguration.setBootnodes(bootstrap);
      discoveryConfiguration.setIncludeBootnodesOnPeerRefresh(
          idnController.getGenesisConfigOptions().isPoa() && poaDiscoveryRetryBootnodes);
      LOG.info("Resolved {} bootnodes.", bootstrap.size());
      LOG.debug("Bootnodes = {}", bootstrap);
      discoveryConfiguration.setDnsDiscoveryURL(ethNetworkConfig.dnsDiscoveryUrl());
      discoveryConfiguration.setDiscoveryV5Enabled(
          networkingConfiguration.getDiscovery().isDiscoveryV5Enabled());
      discoveryConfiguration.setFilterOnEnrForkId(
          networkingConfiguration.getDiscovery().isFilterOnEnrForkIdEnabled());
    } else {
      discoveryConfiguration.setEnabled(false);
    }

    final NodeKey nodeKey = idnController.getNodeKey();

    final SubProtocolConfiguration subProtocolConfiguration =
        idnController.getSubProtocolConfiguration();

    final ProtocolSchedule protocolSchedule = idnController.getProtocolSchedule();
    final ProtocolContext context = idnController.getProtocolContext();

    final List<SubProtocol> subProtocols = subProtocolConfiguration.getSubProtocols();
    final List<ProtocolManager> protocolManagers = subProtocolConfiguration.getProtocolManagers();
    final Set<Capability> supportedCapabilities =
        protocolManagers.stream()
            .flatMap(protocolManager -> protocolManager.getSupportedCapabilities().stream())
            .collect(Collectors.toSet());

    final RlpxConfiguration rlpxConfiguration =
        RlpxConfiguration.create()
            .setBindHost(p2pListenInterface)
            .setBindPort(p2pListenPort)
            .setSupportedProtocols(subProtocols)
            .setClientId(IdnInfo.nodeName(identityString));
    networkingConfiguration.setRlpx(rlpxConfiguration).setDiscovery(discoveryConfiguration);

    final PeerPermissionsDenylist bannedNodes = PeerPermissionsDenylist.create();
    bannedNodeIds.forEach(bannedNodes::add);

    PeerPermissionSubnet peerPermissionSubnet = new PeerPermissionSubnet(allowedSubnets);
    final PeerPermissions defaultPeerPermissions =
        PeerPermissions.combine(peerPermissionSubnet, bannedNodes);

    final List<EnodeURL> bootnodes = discoveryConfiguration.getBootnodes();

    final Synchronizer synchronizer = idnController.getSynchronizer();

    final TransactionSimulator transactionSimulator = idnController.getTransactionSimulator();

    final Bytes localNodeId = nodeKey.getPublicKey().getEncodedBytes();
    final Optional<NodePermissioningController> nodePermissioningController =
        buildNodePermissioningController(
            bootnodes, synchronizer, transactionSimulator, localNodeId, context.getBlockchain());

    final PeerPermissions peerPermissions =
        nodePermissioningController
            .map(nodePC -> new PeerPermissionsAdapter(nodePC, bootnodes, context.getBlockchain()))
            .map(nodePerms -> PeerPermissions.combine(nodePerms, defaultPeerPermissions))
            .orElse(defaultPeerPermissions);

    final EthPeers ethPeers = idnController.getEthPeers();

    LOG.info("Detecting NAT service.");
    final boolean fallbackEnabled = natMethod == NatMethod.AUTO || natMethodFallbackEnabled;
    final NatService natService = new NatService(buildNatManager(natMethod), fallbackEnabled);
    final NetworkBuilder inactiveNetwork = caps -> new NoopP2PNetwork();

    final NetworkBuilder activeNetwork =
        caps -> {
          return DefaultP2PNetwork.builder()
              .vertx(vertx)
              .nodeKey(nodeKey)
              .config(networkingConfiguration)
              .legacyForkIdEnabled(legacyForkIdEnabled)
              .peerPermissions(peerPermissions)
              .metricsSystem(metricsSystem)
              .supportedCapabilities(caps)
              .natService(natService)
              .storageProvider(storageProvider)
              .blockchain(context.getBlockchain())
              .blockNumberForks(idnController.getGenesisConfigOptions().getForkBlockNumbers())
              .timestampForks(idnController.getGenesisConfigOptions().getForkBlockTimestamps())
              .allConnectionsSupplier(ethPeers::streamAllConnections)
              .allActiveConnectionsSupplier(ethPeers::streamAllActiveConnections)
              .maxPeers(ethPeers.getMaxPeers())
              .build();
        };

    final NetworkRunner networkRunner =
        NetworkRunner.builder()
            .protocolManagers(protocolManagers)
            .subProtocols(subProtocols)
            .network(p2pEnabled ? activeNetwork : inactiveNetwork)
            .metricsSystem(metricsSystem)
            .ethPeersShouldConnect(ethPeers::shouldTryToConnect)
            .build();

    ethPeers.setRlpxAgent(networkRunner.getRlpxAgent());

    final P2PNetwork network = networkRunner.getNetwork();
    // ForkId in Ethereum Node Record needs updating when we transition to a new protocol spec
    context
        .getBlockchain()
        .observeBlockAdded(
            blockAddedEvent -> {
              if (protocolSchedule.isOnMilestoneBoundary(blockAddedEvent.getBlock().getHeader())) {
                network.updateNodeRecord();
              }
            });
    nodePermissioningController.ifPresent(
        n ->
            n.setInsufficientPeersPermissioningProvider(
                new InsufficientPeersPermissioningProvider(network, bootnodes)));

    final TransactionPool transactionPool = idnController.getTransactionPool();
    final MiningCoordinator miningCoordinator = idnController.getMiningCoordinator();
    final MiningConfiguration miningConfiguration = idnController.getMiningParameters();

    final BlockchainQueries blockchainQueries =
        new BlockchainQueries(
            protocolSchedule,
            context.getBlockchain(),
            context.getWorldStateArchive(),
            Optional.of(dataDir.resolve(CACHE_PATH)),
            Optional.of(idnController.getProtocolManager().ethContext().getScheduler()),
            apiConfiguration,
            miningConfiguration);

    final PrivacyParameters privacyParameters = idnController.getPrivacyParameters();

    final FilterManager filterManager =
        new FilterManagerBuilder()
            .blockchainQueries(blockchainQueries)
            .transactionPool(transactionPool)
            .privacyParameters(privacyParameters)
            .build();
    vertx.deployVerticle(filterManager);

    createPrivateTransactionObserver(
        filterManager, privacyParameters, context.getBlockchain().getGenesisBlockHeader());

    final P2PNetwork peerNetwork = networkRunner.getNetwork();

    Optional<StratumServer> stratumServer = Optional.empty();

    if (miningConfiguration.isStratumMiningEnabled()) {
      if (!(miningCoordinator instanceof PoWMiningCoordinator powMiningCoordinator)) {
        throw new IllegalArgumentException(
            "Stratum mining requires the network option(--network) to be set to CLASSIC. Stratum server requires a PoWMiningCoordinator not "
                + ((miningCoordinator == null) ? "null" : miningCoordinator.getClass().getName()));
      }
      stratumServer =
          Optional.of(
              new StratumServer(
                  vertx,
                  powMiningCoordinator,
                  miningConfiguration.getStratumPort(),
                  miningConfiguration.getStratumNetworkInterface(),
                  miningConfiguration.getUnstable().getStratumExtranonce(),
                  metricsSystem));
      miningCoordinator.addEthHashObserver(stratumServer.get());
      LOG.debug("added ethash observer: {}", stratumServer.get());
    }

    sanitizePeers(network, staticNodes)
        .map(DefaultPeer::fromEnodeURL)
        .forEach(peerNetwork::addMaintainedConnectionPeer);

    final Optional<NodeLocalConfigPermissioningController> nodeLocalConfigPermissioningController =
        nodePermissioningController.flatMap(NodePermissioningController::localConfigController);

    final Optional<AccountPermissioningController> accountPermissioningController =
        buildAccountPermissioningController(
            permissioningConfiguration, idnController, transactionSimulator);

    final Optional<AccountLocalConfigPermissioningController>
        accountLocalConfigPermissioningController =
            accountPermissioningController.flatMap(
                AccountPermissioningController::getAccountLocalConfigPermissioningController);

    Optional<JsonRpcHttpService> jsonRpcHttpService = Optional.empty();

    if (jsonRpcConfiguration.isEnabled()) {
      final Map<String, JsonRpcMethod> nonEngineMethods =
          jsonRpcMethods(
              protocolSchedule,
              context,
              idnController,
              peerNetwork,
              blockchainQueries,
              synchronizer,
              transactionPool,
              miningConfiguration,
              miningCoordinator,
              metricsSystem,
              supportedCapabilities,
              jsonRpcConfiguration.getRpcApis().stream()
                  .filter(apiGroup -> !apiGroup.toLowerCase(Locale.ROOT).startsWith("engine"))
                  .collect(Collectors.toList()),
              filterManager,
              accountLocalConfigPermissioningController,
              nodeLocalConfigPermissioningController,
              privacyParameters,
              jsonRpcConfiguration,
              webSocketConfiguration,
              metricsConfiguration,
              graphQLConfiguration,
              natService,
              idnPluginContext.getNamedPlugins(),
              dataDir,
              rpcEndpointServiceImpl,
              transactionSimulator,
              idnController.getProtocolManager().ethContext().getScheduler());

      jsonRpcHttpService =
          Optional.of(
              new JsonRpcHttpService(
                  vertx,
                  dataDir,
                  jsonRpcConfiguration,
                  metricsSystem,
                  natService,
                  nonEngineMethods,
                  new HealthService(new LivenessCheck()),
                  new HealthService(new ReadinessCheck(peerNetwork, synchronizer))));
    }

    final SubscriptionManager subscriptionManager =
        createSubscriptionManager(vertx, transactionPool, blockchainQueries);

    Optional<EngineJsonRpcService> engineJsonRpcService = Optional.empty();
    if (engineJsonRpcConfiguration.isPresent() && engineJsonRpcConfiguration.get().isEnabled()) {
      final Map<String, JsonRpcMethod> engineMethods =
          jsonRpcMethods(
              protocolSchedule,
              context,
              idnController,
              peerNetwork,
              blockchainQueries,
              synchronizer,
              transactionPool,
              miningConfiguration,
              miningCoordinator,
              metricsSystem,
              supportedCapabilities,
              engineJsonRpcConfiguration.get().getRpcApis(),
              filterManager,
              accountLocalConfigPermissioningController,
              nodeLocalConfigPermissioningController,
              privacyParameters,
              engineJsonRpcConfiguration.get(),
              webSocketConfiguration,
              metricsConfiguration,
              graphQLConfiguration,
              natService,
              idnPluginContext.getNamedPlugins(),
              dataDir,
              rpcEndpointServiceImpl,
              transactionSimulator,
              idnController.getProtocolManager().ethContext().getScheduler());

      final Optional<AuthenticationService> authToUse =
          engineJsonRpcConfiguration.get().isAuthenticationEnabled()
              ? Optional.of(
                  new EngineAuthService(
                      vertx,
                      Optional.ofNullable(
                          engineJsonRpcConfiguration.get().getAuthenticationPublicKeyFile()),
                      dataDir))
              : Optional.empty();

      final WebSocketConfiguration engineSocketConfig =
          webSocketConfiguration.isEnabled()
              ? webSocketConfiguration
              : WebSocketConfiguration.createEngineDefault();

      final WebSocketMethodsFactory websocketMethodsFactory =
          new WebSocketMethodsFactory(subscriptionManager, engineMethods);

      engineJsonRpcService =
          Optional.of(
              new EngineJsonRpcService(
                  vertx,
                  dataDir,
                  engineJsonRpcConfiguration.orElse(JsonRpcConfiguration.createEngineDefault()),
                  metricsSystem,
                  natService,
                  websocketMethodsFactory.methods(),
                  Optional.ofNullable(engineSocketConfig),
                  idnController.getProtocolManager().ethContext().getScheduler(),
                  authToUse,
                  new HealthService(new LivenessCheck()),
                  new HealthService(new ReadinessCheck(peerNetwork, synchronizer))));
    }

    Optional<GraphQLHttpService> graphQLHttpService = Optional.empty();
    if (graphQLConfiguration.isEnabled()) {
      final GraphQLDataFetchers fetchers = new GraphQLDataFetchers(supportedCapabilities);
      final Map<GraphQLContextType, Object> graphQlContextMap = new ConcurrentHashMap<>();
      graphQlContextMap.putIfAbsent(GraphQLContextType.BLOCKCHAIN_QUERIES, blockchainQueries);
      graphQlContextMap.putIfAbsent(GraphQLContextType.PROTOCOL_SCHEDULE, protocolSchedule);
      graphQlContextMap.putIfAbsent(GraphQLContextType.TRANSACTION_POOL, transactionPool);
      graphQlContextMap.putIfAbsent(GraphQLContextType.MINING_COORDINATOR, miningCoordinator);
      graphQlContextMap.putIfAbsent(GraphQLContextType.SYNCHRONIZER, synchronizer);
      graphQlContextMap.putIfAbsent(
          GraphQLContextType.CHAIN_ID, protocolSchedule.getChainId().map(UInt256::valueOf));
      graphQlContextMap.putIfAbsent(GraphQLContextType.TRANSACTION_SIMULATOR, transactionSimulator);
      final GraphQL graphQL;
      try {
        graphQL = GraphQLProvider.buildGraphQL(fetchers);
      } catch (final IOException ioe) {
        throw new RuntimeException(ioe);
      }

      graphQLHttpService =
          Optional.of(
              new GraphQLHttpService(
                  vertx,
                  dataDir,
                  graphQLConfiguration,
                  graphQL,
                  graphQlContextMap,
                  idnController.getProtocolManager().ethContext().getScheduler()));
    }

    Optional<WebSocketService> webSocketService = Optional.empty();
    if (webSocketConfiguration.isEnabled()) {
      final Map<String, JsonRpcMethod> nonEngineMethods =
          jsonRpcMethods(
              protocolSchedule,
              context,
              idnController,
              peerNetwork,
              blockchainQueries,
              synchronizer,
              transactionPool,
              miningConfiguration,
              miningCoordinator,
              metricsSystem,
              supportedCapabilities,
              webSocketConfiguration.getRpcApis().stream()
                  .filter(apiGroup -> !apiGroup.toLowerCase(Locale.ROOT).startsWith("engine"))
                  .collect(Collectors.toList()),
              filterManager,
              accountLocalConfigPermissioningController,
              nodeLocalConfigPermissioningController,
              privacyParameters,
              jsonRpcConfiguration,
              webSocketConfiguration,
              metricsConfiguration,
              graphQLConfiguration,
              natService,
              idnPluginContext.getNamedPlugins(),
              dataDir,
              rpcEndpointServiceImpl,
              transactionSimulator,
              idnController.getProtocolManager().ethContext().getScheduler());

      createLogsSubscriptionService(
          context.getBlockchain(), subscriptionManager, privacyParameters, blockchainQueries);

      createNewBlockHeadersSubscriptionService(
          context.getBlockchain(), blockchainQueries, subscriptionManager);

      createSyncingSubscriptionService(synchronizer, subscriptionManager);

      webSocketService =
          Optional.of(
              createWebsocketService(
                  vertx,
                  webSocketConfiguration,
                  subscriptionManager,
                  nonEngineMethods,
                  privacyParameters,
                  protocolSchedule,
                  blockchainQueries,
                  DefaultAuthenticationService.create(vertx, webSocketConfiguration),
                  metricsSystem));

      createPrivateTransactionObserver(
          subscriptionManager, privacyParameters, context.getBlockchain().getGenesisBlockHeader());
    }

    final Optional<MetricsService> metricsService = createMetricsService(metricsConfiguration);

    final Optional<EthStatsService> ethStatsService;
    if (isEthStatsEnabled()) {
      ethStatsService =
          Optional.of(
              new EthStatsService(
                  EthStatsConnectOptions.fromParams(
                      ethstatsOptions.getEthstatsUrl(),
                      ethstatsOptions.getEthstatsContact(),
                      ethstatsOptions.getEthstatsCaCert()),
                  blockchainQueries,
                  idnController.getProtocolManager(),
                  transactionPool,
                  miningCoordinator,
                  idnController.getSyncState(),
                  vertx,
                  IdnInfo.nodeName(identityString),
                  idnController.getGenesisConfigOptions(),
                  network));
    } else {
      ethStatsService = Optional.empty();
    }

    final Optional<JsonRpcIpcService> jsonRpcIpcService;
    if (jsonRpcIpcConfiguration.isEnabled()) {
      final Map<String, JsonRpcMethod> ipcMethods =
          jsonRpcMethods(
              protocolSchedule,
              context,
              idnController,
              peerNetwork,
              blockchainQueries,
              synchronizer,
              transactionPool,
              miningConfiguration,
              miningCoordinator,
              metricsSystem,
              supportedCapabilities,
              jsonRpcIpcConfiguration.getEnabledApis().stream()
                  .filter(apiGroup -> !apiGroup.toLowerCase(Locale.ROOT).startsWith("engine"))
                  .collect(Collectors.toList()),
              filterManager,
              accountLocalConfigPermissioningController,
              nodeLocalConfigPermissioningController,
              privacyParameters,
              jsonRpcConfiguration,
              webSocketConfiguration,
              metricsConfiguration,
              graphQLConfiguration,
              natService,
              idnPluginContext.getNamedPlugins(),
              dataDir,
              rpcEndpointServiceImpl,
              transactionSimulator,
              idnController.getProtocolManager().ethContext().getScheduler());

      jsonRpcIpcService =
          Optional.of(
              new JsonRpcIpcService(
                  vertx,
                  jsonRpcIpcConfiguration.getPath(),
                  new JsonRpcExecutor(new BaseJsonRpcProcessor(), ipcMethods)));
    } else {
      jsonRpcIpcService = Optional.empty();
    }

    final Map<String, JsonRpcMethod> inProcessRpcMethods;
    if (inProcessRpcConfiguration.isEnabled()) {
      inProcessRpcMethods =
          jsonRpcMethods(
              protocolSchedule,
              context,
              idnController,
              peerNetwork,
              blockchainQueries,
              synchronizer,
              transactionPool,
              miningConfiguration,
              miningCoordinator,
              metricsSystem,
              supportedCapabilities,
              inProcessRpcConfiguration.getInProcessRpcApis(),
              filterManager,
              accountLocalConfigPermissioningController,
              nodeLocalConfigPermissioningController,
              privacyParameters,
              jsonRpcConfiguration,
              webSocketConfiguration,
              metricsConfiguration,
              graphQLConfiguration,
              natService,
              idnPluginContext.getNamedPlugins(),
              dataDir,
              rpcEndpointServiceImpl,
              transactionSimulator,
              idnController.getProtocolManager().ethContext().getScheduler());
    } else {
      inProcessRpcMethods = Map.of();
    }

    return new Runner(
        vertx,
        networkRunner,
        natService,
        jsonRpcHttpService,
        engineJsonRpcService,
        graphQLHttpService,
        webSocketService,
        jsonRpcIpcService,
        inProcessRpcMethods,
        stratumServer,
        metricsService,
        ethStatsService,
        idnController,
        dataDir,
        pidPath,
        autoLogBloomCaching ? blockchainQueries.getTransactionLogBloomCacher() : Optional.empty(),
        context.getBlockchain());
  }

  private boolean isEthStatsEnabled() {
    return ethstatsOptions != null && !Strings.isNullOrEmpty(ethstatsOptions.getEthstatsUrl());
  }

  private Stream<EnodeURL> sanitizePeers(
      final P2PNetwork network, final Collection<EnodeURL> enodeURLS) {
    if (network.getLocalEnode().isEmpty()) {
      return enodeURLS.stream();
    }
    final EnodeURL localEnodeURL = network.getLocalEnode().get();
    return enodeURLS.stream()
        .filter(enodeURL -> !enodeURL.getNodeId().equals(localEnodeURL.getNodeId()));
  }

  private Optional<NodePermissioningController> buildNodePermissioningController(
      final List<EnodeURL> bootnodesAsEnodeURLs,
      final Synchronizer synchronizer,
      final TransactionSimulator transactionSimulator,
      final Bytes localNodeId,
      final Blockchain blockchain) {
    final Collection<EnodeURL> fixedNodes = getFixedNodes(bootnodesAsEnodeURLs, staticNodes);

    if (permissioningConfiguration.isPresent()) {
      final PermissioningConfiguration configuration = this.permissioningConfiguration.get();
      final NodePermissioningController nodePermissioningController =
          new NodePermissioningControllerFactory()
              .create(
                  configuration,
                  synchronizer,
                  fixedNodes,
                  localNodeId,
                  transactionSimulator,
                  metricsSystem,
                  blockchain,
                  permissioningService.getConnectionPermissioningProviders());

      return Optional.of(nodePermissioningController);
    } else if (permissioningService.getConnectionPermissioningProviders().size() > 0) {
      final NodePermissioningController nodePermissioningController =
          new NodePermissioningControllerFactory()
              .create(
                  new PermissioningConfiguration(Optional.empty(), Optional.empty()),
                  synchronizer,
                  fixedNodes,
                  localNodeId,
                  transactionSimulator,
                  metricsSystem,
                  blockchain,
                  permissioningService.getConnectionPermissioningProviders());

      return Optional.of(nodePermissioningController);
    } else {
      return Optional.empty();
    }
  }

  private Optional<AccountPermissioningController> buildAccountPermissioningController(
      final Optional<PermissioningConfiguration> permissioningConfiguration,
      final IdnController idnController,
      final TransactionSimulator transactionSimulator) {

    if (permissioningConfiguration.isPresent()
        || permissioningService.getTransactionPermissioningProviders().size() > 0) {
      final PermissioningConfiguration configuration =
          permissioningConfiguration.orElse(
              new PermissioningConfiguration(Optional.empty(), Optional.empty()));
      final Optional<AccountPermissioningController> accountPermissioningController =
          AccountPermissioningControllerFactory.create(
              configuration,
              transactionSimulator,
              metricsSystem,
              permissioningService.getTransactionPermissioningProviders());

      accountPermissioningController.ifPresent(
          permissioningController ->
              idnController
                  .getProtocolSchedule()
                  .setPermissionTransactionFilter(permissioningController::isPermitted));

      return accountPermissioningController;
    } else {
      return Optional.empty();
    }
  }

  private Optional<NatManager> buildNatManager(final NatMethod natMethod) {

    final NatMethod detectedNatMethod =
        Optional.of(natMethod)
            .filter(not(isEqual(NatMethod.AUTO)))
            .orElse(NatService.autoDetectNatMethod(new DockerDetector()));
    switch (detectedNatMethod) {
      case UPNP:
        return Optional.of(new UpnpNatManager());
      case DOCKER:
        return Optional.of(
            new DockerNatManager(p2pAdvertisedHost, p2pListenPort, jsonRpcConfiguration.getPort()));
      case NONE:
      default:
        return Optional.empty();
    }
  }

  /**
   * Gets fixed nodes. Visible for testing.
   *
   * @param someFixedNodes the fixed nodes
   * @param moreFixedNodes nodes added to fixed nodes
   * @return the fixed and more nodes combined
   */
  @VisibleForTesting
  public static Collection<EnodeURL> getFixedNodes(
      final Collection<EnodeURL> someFixedNodes, final Collection<EnodeURL> moreFixedNodes) {
    final Collection<EnodeURL> fixedNodes = new ArrayList<>(someFixedNodes);
    fixedNodes.addAll(moreFixedNodes);
    return fixedNodes;
  }

  private Map<String, JsonRpcMethod> jsonRpcMethods(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final IdnController idnController,
      final P2PNetwork network,
      final BlockchainQueries blockchainQueries,
      final Synchronizer synchronizer,
      final TransactionPool transactionPool,
      final MiningConfiguration miningConfiguration,
      final MiningCoordinator miningCoordinator,
      final ObservableMetricsSystem metricsSystem,
      final Set<Capability> supportedCapabilities,
      final Collection<String> jsonRpcApis,
      final FilterManager filterManager,
      final Optional<AccountLocalConfigPermissioningController> accountAllowlistController,
      final Optional<NodeLocalConfigPermissioningController> nodeAllowlistController,
      final PrivacyParameters privacyParameters,
      final JsonRpcConfiguration jsonRpcConfiguration,
      final WebSocketConfiguration webSocketConfiguration,
      final MetricsConfiguration metricsConfiguration,
      final GraphQLConfiguration graphQLConfiguration,
      final NatService natService,
      final Map<String, IdnPlugin> namedPlugins,
      final Path dataDir,
      final RpcEndpointServiceImpl rpcEndpointServiceImpl,
      final TransactionSimulator transactionSimulator,
      final EthScheduler ethScheduler) {
    // sync vertx for engine consensus API, to process requests in FIFO order;
    final Vertx consensusEngineServer = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1));

    final Map<String, JsonRpcMethod> methods =
        new JsonRpcMethodsFactory()
            .methods(
                IdnInfo.nodeName(identityString),
                IdnInfo.shortVersion(),
                IdnInfo.commit(),
                ethNetworkConfig.networkId(),
                idnController.getGenesisConfigOptions(),
                network,
                blockchainQueries,
                synchronizer,
                protocolSchedule,
                protocolContext,
                filterManager,
                transactionPool,
                miningConfiguration,
                miningCoordinator,
                metricsSystem,
                supportedCapabilities,
                accountAllowlistController,
                nodeAllowlistController,
                jsonRpcApis,
                privacyParameters,
                jsonRpcConfiguration,
                webSocketConfiguration,
                metricsConfiguration,
                graphQLConfiguration,
                natService,
                namedPlugins,
                dataDir,
                idnController.getProtocolManager().ethContext().getEthPeers(),
                consensusEngineServer,
                apiConfiguration,
                enodeDnsConfiguration,
                transactionSimulator,
                ethScheduler);
    methods.putAll(idnController.getAdditionalJsonRpcMethods(jsonRpcApis));

    final var pluginMethods =
        rpcEndpointServiceImpl.getPluginMethods(jsonRpcConfiguration.getRpcApis());

    final var overriddenMethods =
        methods.keySet().stream().filter(pluginMethods::containsKey).collect(Collectors.toList());
    if (overriddenMethods.size() > 0) {
      throw new RuntimeException("You can not override built in methods " + overriddenMethods);
    }

    methods.putAll(pluginMethods);
    return methods;
  }

  private SubscriptionManager createSubscriptionManager(
      final Vertx vertx,
      final TransactionPool transactionPool,
      final BlockchainQueries blockchainQueries) {
    final SubscriptionManager subscriptionManager =
        new SubscriptionManager(metricsSystem, blockchainQueries.getBlockchain());
    final PendingTransactionSubscriptionService pendingTransactions =
        new PendingTransactionSubscriptionService(subscriptionManager);
    final PendingTransactionDroppedSubscriptionService pendingTransactionsRemoved =
        new PendingTransactionDroppedSubscriptionService(subscriptionManager);
    transactionPool.subscribePendingTransactions(pendingTransactions);
    transactionPool.subscribeDroppedTransactions(pendingTransactionsRemoved);
    vertx.deployVerticle(subscriptionManager);

    return subscriptionManager;
  }

  private void createLogsSubscriptionService(
      final Blockchain blockchain,
      final SubscriptionManager subscriptionManager,
      final PrivacyParameters privacyParameters,
      final BlockchainQueries blockchainQueries) {

    Optional<PrivacyQueries> privacyQueries = Optional.empty();
    if (privacyParameters.isEnabled()) {
      privacyQueries =
          Optional.of(
              new PrivacyQueries(
                  blockchainQueries, privacyParameters.getPrivateWorldStateReader()));
    }

    final LogsSubscriptionService logsSubscriptionService =
        new LogsSubscriptionService(subscriptionManager, privacyQueries);

    // monitoring public logs
    blockchain.observeLogs(logsSubscriptionService);

    // monitoring private logs
    if (privacyParameters.isEnabled()) {
      blockchain.observeBlockAdded(logsSubscriptionService::checkPrivateLogs);
    }
  }

  private void createPrivateTransactionObserver(
      final PrivateTransactionObserver privateTransactionObserver,
      final PrivacyParameters privacyParameters,
      final BlockHeader genesisBlockHeader) {
    // register privateTransactionObserver as observer of events fired by the flexible precompile.
    if (privacyParameters.isFlexiblePrivacyGroupsEnabled()
        && privacyParameters.isMultiTenancyEnabled()) {
      final FlexiblePrivacyPrecompiledContract flexiblePrivacyPrecompiledContract =
          (FlexiblePrivacyPrecompiledContract)
              idnController
                  .getProtocolSchedule()
                  .getByBlockHeader(genesisBlockHeader)
                  .getPrecompileContractRegistry()
                  .get(FLEXIBLE_PRIVACY);

      flexiblePrivacyPrecompiledContract.addPrivateTransactionObserver(privateTransactionObserver);
    }
  }

  private void createSyncingSubscriptionService(
      final Synchronizer synchronizer, final SubscriptionManager subscriptionManager) {
    new SyncingSubscriptionService(subscriptionManager, synchronizer);
  }

  private void createNewBlockHeadersSubscriptionService(
      final Blockchain blockchain,
      final BlockchainQueries blockchainQueries,
      final SubscriptionManager subscriptionManager) {
    final NewBlockHeadersSubscriptionService newBlockHeadersSubscriptionService =
        new NewBlockHeadersSubscriptionService(subscriptionManager, blockchainQueries);

    blockchain.observeBlockAdded(newBlockHeadersSubscriptionService);
  }

  private WebSocketService createWebsocketService(
      final Vertx vertx,
      final WebSocketConfiguration configuration,
      final SubscriptionManager subscriptionManager,
      final Map<String, JsonRpcMethod> jsonRpcMethods,
      final PrivacyParameters privacyParameters,
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries,
      final Optional<AuthenticationService> authenticationService,
      final ObservableMetricsSystem metricsSystem) {

    final WebSocketMethodsFactory websocketMethodsFactory =
        new WebSocketMethodsFactory(subscriptionManager, jsonRpcMethods);

    if (privacyParameters.isEnabled()) {
      final PrivateWebSocketMethodsFactory privateWebSocketMethodsFactory =
          new PrivateWebSocketMethodsFactory(
              privacyParameters, subscriptionManager, protocolSchedule, blockchainQueries);

      privateWebSocketMethodsFactory.methods().forEach(websocketMethodsFactory::addMethods);
    }

    rpcEndpointServiceImpl
        .getPluginMethods(configuration.getRpcApis())
        .values()
        .forEach(websocketMethodsFactory::addMethods);

    final JsonRpcProcessor jsonRpcProcessor;
    if (authenticationService.isPresent()) {
      jsonRpcProcessor =
          new AuthenticatedJsonRpcProcessor(
              new BaseJsonRpcProcessor(),
              authenticationService.get(),
              configuration.getRpcApisNoAuth());
    } else {
      jsonRpcProcessor = new BaseJsonRpcProcessor();
    }
    final JsonRpcExecutor jsonRpcExecutor =
        new JsonRpcExecutor(jsonRpcProcessor, websocketMethodsFactory.methods());
    final WebSocketMessageHandler websocketMessageHandler =
        new WebSocketMessageHandler(
            vertx,
            jsonRpcExecutor,
            idnController.getProtocolManager().ethContext().getScheduler(),
            webSocketConfiguration.getTimeoutSec());

    return new WebSocketService(
        vertx, configuration, websocketMessageHandler, authenticationService, metricsSystem);
  }

  private Optional<MetricsService> createMetricsService(final MetricsConfiguration configuration) {
    return MetricsService.create(configuration, metricsSystem);
  }

  /**
   * Add Legacy fork id.
   *
   * @param legacyEth64ForkIdEnabled the legacy eth64 fork id enabled
   * @return the runner builder
   */
  public RunnerBuilder legacyForkId(final boolean legacyEth64ForkIdEnabled) {
    this.legacyForkIdEnabled = legacyEth64ForkIdEnabled;
    return this;
  }
}
