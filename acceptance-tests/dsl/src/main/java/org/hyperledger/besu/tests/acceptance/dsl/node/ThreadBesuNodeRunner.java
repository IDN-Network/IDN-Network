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
package org.idnecology.idn.tests.acceptance.dsl.node;

import static org.idnecology.idn.controller.IdnController.DATABASE_PATH;

import org.idnecology.idn.Runner;
import org.idnecology.idn.RunnerBuilder;
import org.idnecology.idn.chainexport.RlpBlockExporter;
import org.idnecology.idn.chainimport.Era1BlockImporter;
import org.idnecology.idn.chainimport.JsonBlockImporter;
import org.idnecology.idn.chainimport.RlpBlockImporter;
import org.idnecology.idn.cli.IdnCommand;
import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.cli.config.NetworkName;
import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.controller.IdnControllerBuilder;
import org.idnecology.idn.crypto.KeyPairUtil;
import org.idnecology.idn.cryptoservices.KeyPairSecurityModule;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.InProcessRpcConfiguration;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.plugins.PluginConfiguration;
import org.idnecology.idn.ethereum.core.plugins.PluginInfo;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.BlobCacheModule;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.peers.EnodeURLImpl;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoaderModule;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.MetricCategoryRegistryImpl;
import org.idnecology.idn.metrics.MetricsSystemModule;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.plugin.data.EnodeURL;
import org.idnecology.idn.plugin.services.IdnConfiguration;
import org.idnecology.idn.plugin.services.IdnEvents;
import org.idnecology.idn.plugin.services.BlockchainService;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.PermissioningService;
import org.idnecology.idn.plugin.services.PicoCLIOptions;
import org.idnecology.idn.plugin.services.PrivacyPluginService;
import org.idnecology.idn.plugin.services.RpcEndpointService;
import org.idnecology.idn.plugin.services.SecurityModuleService;
import org.idnecology.idn.plugin.services.StorageService;
import org.idnecology.idn.plugin.services.TransactionPoolValidatorService;
import org.idnecology.idn.plugin.services.TransactionSelectionService;
import org.idnecology.idn.plugin.services.TransactionSimulationService;
import org.idnecology.idn.plugin.services.metrics.MetricCategoryRegistry;
import org.idnecology.idn.plugin.services.mining.MiningService;
import org.idnecology.idn.plugin.services.storage.KeyValueStorageFactory;
import org.idnecology.idn.plugin.services.storage.rocksdb.RocksDBPlugin;
import org.idnecology.idn.plugin.services.transactionpool.TransactionPoolService;
import org.idnecology.idn.services.IdnConfigurationImpl;
import org.idnecology.idn.services.IdnEventsImpl;
import org.idnecology.idn.services.IdnPluginContextImpl;
import org.idnecology.idn.services.BlockchainServiceImpl;
import org.idnecology.idn.services.MiningServiceImpl;
import org.idnecology.idn.services.PermissioningServiceImpl;
import org.idnecology.idn.services.PicoCLIOptionsImpl;
import org.idnecology.idn.services.PrivacyPluginServiceImpl;
import org.idnecology.idn.services.RpcEndpointServiceImpl;
import org.idnecology.idn.services.SecurityModuleServiceImpl;
import org.idnecology.idn.services.StorageServiceImpl;
import org.idnecology.idn.services.TransactionPoolServiceImpl;
import org.idnecology.idn.services.TransactionPoolValidatorServiceImpl;
import org.idnecology.idn.services.TransactionSelectionServiceImpl;
import org.idnecology.idn.services.TransactionSimulationServiceImpl;
import org.idnecology.idn.services.kvstore.InMemoryStoragePlugin;

import java.io.File;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class ThreadIdnNodeRunner implements IdnNodeRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadIdnNodeRunner.class);
  private final Map<String, Runner> idnRunners = new HashMap<>();

  private final Map<Node, IdnPluginContextImpl> idnPluginContextMap = new ConcurrentHashMap<>();

  @Override
  public void startNode(final IdnNode node) {

    if (MDC.get("node") != null) {
      LOG.error("ThreadContext node is already set to {}", MDC.get("node"));
    }
    MDC.put("node", node.getName());

    if (!node.getRunCommand().isEmpty()) {
      throw new UnsupportedOperationException("commands are not supported with thread runner");
    }

    IdnNodeProviderModule module = new IdnNodeProviderModule(node);
    AcceptanceTestIdnComponent component =
        DaggerThreadIdnNodeRunner_AcceptanceTestIdnComponent.builder()
            .idnNodeProviderModule(module)
            .build();

    final Path dataDir = node.homeDirectory();
    final PermissioningServiceImpl permissioningService = new PermissioningServiceImpl();

    GlobalOpenTelemetry.resetForTest();
    final ObservableMetricsSystem metricsSystem =
        (ObservableMetricsSystem) component.getMetricsSystem();
    final List<EnodeURL> bootnodes =
        node.getConfiguration().getBootnodes().stream().map(EnodeURLImpl::fromURI).toList();

    final EthNetworkConfig.Builder networkConfigBuilder = component.ethNetworkConfigBuilder();
    networkConfigBuilder.setBootNodes(bootnodes);
    node.getConfiguration()
        .getGenesisConfig()
        .map(GenesisConfig::fromConfig)
        .ifPresent(networkConfigBuilder::setGenesisConfig);
    final EthNetworkConfig ethNetworkConfig = networkConfigBuilder.build();
    final IdnControllerBuilder builder = component.idnControllerBuilder();
    builder.isRevertReasonEnabled(node.isRevertReasonEnabled());
    builder.networkConfiguration(node.getNetworkingConfiguration());

    builder.dataDirectory(dataDir);
    builder.nodeKey(new NodeKey(new KeyPairSecurityModule(KeyPairUtil.loadKeyPair(dataDir))));
    builder.privacyParameters(node.getPrivacyParameters());

    node.getGenesisConfig().map(GenesisConfig::fromConfig).ifPresent(builder::genesisConfig);

    final IdnController idnController = component.idnController();

    InProcessRpcConfiguration inProcessRpcConfiguration = node.inProcessRpcConfiguration();

    final IdnPluginContextImpl idnPluginContext =
        idnPluginContextMap.computeIfAbsent(node, n -> component.getIdnPluginContext());

    final RunnerBuilder runnerBuilder = new RunnerBuilder();
    runnerBuilder.permissioningConfiguration(node.getPermissioningConfiguration());
    runnerBuilder.apiConfiguration(node.getApiConfiguration());

    runnerBuilder
        .vertx(Vertx.vertx())
        .idnController(idnController)
        .ethNetworkConfig(ethNetworkConfig)
        .discoveryEnabled(node.isDiscoveryEnabled())
        .p2pAdvertisedHost(node.getHostName())
        .p2pListenPort(0)
        .networkingConfiguration(node.getNetworkingConfiguration())
        .jsonRpcConfiguration(node.jsonRpcConfiguration())
        .webSocketConfiguration(node.webSocketConfiguration())
        .jsonRpcIpcConfiguration(node.jsonRpcIpcConfiguration())
        .dataDir(node.homeDirectory())
        .metricsSystem(metricsSystem)
        .permissioningService(permissioningService)
        .metricsConfiguration(node.getMetricsConfiguration())
        .p2pEnabled(node.isP2pEnabled())
        .graphQLConfiguration(GraphQLConfiguration.createDefault())
        .staticNodes(node.getStaticNodes().stream().map(EnodeURLImpl::fromString).toList())
        .idnPluginContext(idnPluginContext)
        .autoLogBloomCaching(false)
        .storageProvider(idnController.getStorageProvider())
        .rpcEndpointService(component.rpcEndpointService())
        .inProcessRpcConfiguration(inProcessRpcConfiguration);
    node.engineRpcConfiguration().ifPresent(runnerBuilder::engineJsonRpcConfiguration);
    idnPluginContext.beforeExternalServices();
    final Runner runner = runnerBuilder.build();

    runner.startExternalServices();

    idnPluginContext.addService(
        IdnEvents.class,
        new IdnEventsImpl(
            idnController.getProtocolContext().getBlockchain(),
            idnController.getProtocolManager().getBlockBroadcaster(),
            idnController.getTransactionPool(),
            idnController.getSyncState(),
            idnController.getProtocolContext().getBadBlockManager()));
    idnPluginContext.addService(
        TransactionPoolService.class,
        new TransactionPoolServiceImpl(idnController.getTransactionPool()));
    idnPluginContext.addService(
        MiningService.class, new MiningServiceImpl(idnController.getMiningCoordinator()));

    component.rpcEndpointService().init(runner.getInProcessRpcMethods());

    idnPluginContext.startPlugins();

    runner.startEthereumMainLoop();

    idnRunners.put(node.getName(), runner);
    MDC.remove("node");
  }

  @Override
  public void stopNode(final IdnNode node) {
    final IdnPluginContextImpl pluginContext = idnPluginContextMap.remove(node);
    if (pluginContext != null) {
      pluginContext.stopPlugins();
    }
    node.stop();
    killRunner(node.getName());
  }

  @Override
  public void shutdown() {
    // stop all plugins from pluginContext
    idnPluginContextMap.values().forEach(IdnPluginContextImpl::stopPlugins);
    idnPluginContextMap.clear();

    // iterate over a copy of the set so that idnRunner can be updated when a runner is killed
    new HashSet<>(idnRunners.keySet()).forEach(this::killRunner);
  }

  @Override
  public boolean isActive(final String nodeName) {
    return idnRunners.containsKey(nodeName);
  }

  private void killRunner(final String name) {
    LOG.info("Killing " + name + " runner");

    if (idnRunners.containsKey(name)) {
      try {
        idnRunners.get(name).close();
        idnRunners.remove(name);
      } catch (final Exception e) {
        throw new RuntimeException("Error shutting down node " + name, e);
      }
    } else {
      LOG.error("There was a request to kill an unknown node: {}", name);
    }
  }

  @Override
  public void startConsoleCapture() {
    throw new RuntimeException("Console contents can only be captured in process execution");
  }

  @Override
  public String getConsoleContents() {
    throw new RuntimeException("Console contents can only be captured in process execution");
  }

  @Module
  @SuppressWarnings("CloseableProvides")
  static class IdnNodeProviderModule {

    private final IdnNode toProvide;

    public IdnNodeProviderModule(final IdnNode toProvide) {
      this.toProvide = toProvide;
    }

    @Provides
    @Singleton
    MetricsConfiguration provideMetricsConfiguration() {
      if (toProvide.getMetricsConfiguration() != null) {
        return toProvide.getMetricsConfiguration();
      } else {
        return MetricsConfiguration.builder().build();
      }
    }

    @Provides
    public IdnNode provideIdnNodeRunner() {
      return toProvide;
    }

    @Provides
    @Named("ExtraCLIOptions")
    public List<String> provideExtraCLIOptions() {
      return toProvide.getExtraCLIOptions();
    }

    @Provides
    @Named("RequestedPlugins")
    public List<String> provideRequestedPlugins() {
      return toProvide.getRequestedPlugins();
    }

    @Provides
    Path provideDataDir() {
      return toProvide.homeDirectory();
    }

    @Provides
    @Singleton
    RpcEndpointServiceImpl provideRpcEndpointService() {
      return new RpcEndpointServiceImpl();
    }

    @Provides
    @Singleton
    BlockchainServiceImpl provideBlockchainService(final IdnController idnController) {
      BlockchainServiceImpl retval = new BlockchainServiceImpl();
      retval.init(
          idnController.getProtocolContext().getBlockchain(),
          idnController.getProtocolSchedule());
      return retval;
    }

    @Provides
    @Singleton
    Blockchain provideBlockchain(final IdnController idnController) {
      return idnController.getProtocolContext().getBlockchain();
    }

    @Provides
    @SuppressWarnings("CloseableProvides")
    WorldStateArchive provideWorldStateArchive(final IdnController idnController) {
      return idnController.getProtocolContext().getWorldStateArchive();
    }

    @Provides
    ProtocolSchedule provideProtocolSchedule(final IdnController idnController) {
      return idnController.getProtocolSchedule();
    }

    @Provides
    ApiConfiguration provideApiConfiguration(final IdnNode node) {
      return node.getApiConfiguration();
    }

    @Provides
    @Singleton
    TransactionPoolValidatorServiceImpl provideTransactionPoolValidatorService() {
      return new TransactionPoolValidatorServiceImpl();
    }

    @Provides
    @Singleton
    TransactionSelectionServiceImpl provideTransactionSelectionService() {
      return new TransactionSelectionServiceImpl();
    }

    @Provides
    @Singleton
    TransactionPoolConfiguration provideTransactionPoolConfiguration(
        final IdnNode node,
        final TransactionPoolValidatorServiceImpl transactionPoolValidatorServiceImpl) {

      TransactionPoolConfiguration txPoolConfig =
          ImmutableTransactionPoolConfiguration.builder()
              .from(node.getTransactionPoolConfiguration())
              .strictTransactionReplayProtectionEnabled(node.isStrictTxReplayProtectionEnabled())
              .transactionPoolValidatorService(transactionPoolValidatorServiceImpl)
              .build();
      return txPoolConfig;
    }

    @Provides
    @Singleton
    TransactionSimulator provideTransactionSimulator(
        final Blockchain blockchain,
        final WorldStateArchive worldStateArchive,
        final ProtocolSchedule protocolSchedule,
        final MiningConfiguration miningConfiguration,
        final ApiConfiguration apiConfiguration) {
      return new TransactionSimulator(
          blockchain,
          worldStateArchive,
          protocolSchedule,
          miningConfiguration,
          apiConfiguration.getGasCap());
    }

    @Provides
    @Singleton
    TransactionSimulationServiceImpl provideTransactionSimulationService(
        final Blockchain blockchain, final TransactionSimulator transactionSimulator) {
      TransactionSimulationServiceImpl retval = new TransactionSimulationServiceImpl();
      retval.init(blockchain, transactionSimulator);
      return retval;
    }

    @Provides
    KeyValueStorageFactory provideKeyValueStorageFactory() {
      return toProvide
          .getStorageFactory()
          .orElse(new InMemoryStoragePlugin.InMemoryKeyValueStorageFactory("memory"));
    }

    @Provides
    @Singleton
    MetricCategoryRegistryImpl provideMetricCategoryRegistry() {
      return new MetricCategoryRegistryImpl();
    }
  }

  @Module
  public static class ThreadIdnNodeRunnerModule {
    @Provides
    @Singleton
    public ThreadIdnNodeRunner provideThreadIdnNodeRunner() {
      return new ThreadIdnNodeRunner();
    }
  }

  @Module
  @SuppressWarnings("CloseableProvides")
  public static class IdnControllerModule {
    @Provides
    @Singleton
    public SynchronizerConfiguration provideSynchronizationConfiguration() {
      final SynchronizerConfiguration synchronizerConfiguration =
          SynchronizerConfiguration.builder().build();
      return synchronizerConfiguration;
    }

    @Singleton
    @Provides
    public IdnControllerBuilder provideIdnControllerBuilder(
        final EthNetworkConfig ethNetworkConfig,
        final SynchronizerConfiguration synchronizerConfiguration,
        final TransactionPoolConfiguration transactionPoolConfiguration) {

      final IdnControllerBuilder builder =
          new IdnController.Builder()
              .fromEthNetworkConfig(ethNetworkConfig, synchronizerConfiguration.getSyncMode());
      builder.transactionPoolConfiguration(transactionPoolConfiguration);
      return builder;
    }

    @Provides
    @Singleton
    public IdnController provideIdnController(
        final SynchronizerConfiguration synchronizerConfiguration,
        final IdnControllerBuilder builder,
        final MetricsSystem metricsSystem,
        final KeyValueStorageProvider storageProvider,
        final MiningConfiguration miningConfiguration,
        final ApiConfiguration apiConfiguration) {

      builder
          .synchronizerConfiguration(synchronizerConfiguration)
          .metricsSystem((ObservableMetricsSystem) metricsSystem)
          .dataStorageConfiguration(DataStorageConfiguration.DEFAULT_FOREST_CONFIG)
          .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
          .clock(Clock.systemUTC())
          .storageProvider(storageProvider)
          .evmConfiguration(EvmConfiguration.DEFAULT)
          .maxPeers(25)
          .maxRemotelyInitiatedPeers(15)
          .miningParameters(miningConfiguration)
          .randomPeerPriority(false)
          .apiConfiguration(apiConfiguration)
          .idnComponent(null);
      return builder.build();
    }

    @Provides
    @Singleton
    public EthNetworkConfig.Builder provideEthNetworkConfigBuilder() {
      final EthNetworkConfig.Builder networkConfigBuilder =
          new EthNetworkConfig.Builder(EthNetworkConfig.getNetworkConfig(NetworkName.DEV));
      return networkConfigBuilder;
    }

    @Provides
    public EthNetworkConfig provideEthNetworkConfig(
        final EthNetworkConfig.Builder networkConfigBuilder) {

      final EthNetworkConfig ethNetworkConfig = networkConfigBuilder.build();
      return ethNetworkConfig;
    }

    @Provides
    public IdnPluginContextImpl providePluginContext(
        final StorageServiceImpl storageService,
        final SecurityModuleServiceImpl securityModuleService,
        final TransactionSimulationServiceImpl transactionSimulationServiceImpl,
        final TransactionSelectionServiceImpl transactionSelectionServiceImpl,
        final TransactionPoolValidatorServiceImpl transactionPoolValidatorServiceImpl,
        final BlockchainServiceImpl blockchainServiceImpl,
        final RpcEndpointServiceImpl rpcEndpointServiceImpl,
        final IdnConfiguration commonPluginConfiguration,
        final PermissioningServiceImpl permissioningService,
        final MetricsConfiguration metricsConfiguration,
        final MetricCategoryRegistryImpl metricCategoryRegistry,
        final MetricsSystem metricsSystem,
        final @Named("ExtraCLIOptions") List<String> extraCLIOptions,
        final @Named("RequestedPlugins") List<String> requestedPlugins) {
      final CommandLine commandLine = new CommandLine(CommandSpec.create());
      final IdnPluginContextImpl idnPluginContext = new IdnPluginContextImpl();
      idnPluginContext.addService(StorageService.class, storageService);
      idnPluginContext.addService(SecurityModuleService.class, securityModuleService);
      idnPluginContext.addService(PicoCLIOptions.class, new PicoCLIOptionsImpl(commandLine));
      idnPluginContext.addService(RpcEndpointService.class, rpcEndpointServiceImpl);
      idnPluginContext.addService(
          TransactionSelectionService.class, transactionSelectionServiceImpl);
      idnPluginContext.addService(
          TransactionPoolValidatorService.class, transactionPoolValidatorServiceImpl);
      idnPluginContext.addService(
          TransactionSimulationService.class, transactionSimulationServiceImpl);
      idnPluginContext.addService(BlockchainService.class, blockchainServiceImpl);
      idnPluginContext.addService(IdnConfiguration.class, commonPluginConfiguration);
      metricCategoryRegistry.setMetricsConfiguration(metricsConfiguration);
      idnPluginContext.addService(MetricCategoryRegistry.class, metricCategoryRegistry);
      idnPluginContext.addService(MetricsSystem.class, metricsSystem);

      final Path pluginsPath;
      final String pluginDir = System.getProperty("idn.plugins.dir");
      if (pluginDir == null || pluginDir.isEmpty()) {
        pluginsPath = commonPluginConfiguration.getDataPath().resolve("plugins");
        final File pluginsDirFile = pluginsPath.toFile();
        if (!pluginsDirFile.isDirectory()) {
          pluginsDirFile.mkdirs();
          pluginsDirFile.deleteOnExit();
        }
        System.setProperty("idn.plugins.dir", pluginsPath.toString());
      } else {
        pluginsPath = Path.of(pluginDir);
      }

      idnPluginContext.addService(IdnConfiguration.class, commonPluginConfiguration);
      idnPluginContext.addService(PermissioningService.class, permissioningService);
      idnPluginContext.addService(PrivacyPluginService.class, new PrivacyPluginServiceImpl());

      idnPluginContext.initialize(
          new PluginConfiguration.Builder()
              .pluginsDir(pluginsPath)
              .requestedPlugins(requestedPlugins.stream().map(PluginInfo::new).toList())
              .build());
      idnPluginContext.registerPlugins();
      commandLine.parseArgs(extraCLIOptions.toArray(new String[0]));

      // register built-in plugins
      new RocksDBPlugin().register(idnPluginContext);
      return idnPluginContext;
    }

    @Provides
    public KeyValueStorageProvider provideKeyValueStorageProvider(
        final IdnConfiguration commonPluginConfiguration,
        final MetricsSystem metricsSystem,
        final KeyValueStorageFactory keyValueStorageFactory) {

      final StorageServiceImpl storageService = new StorageServiceImpl();
      final KeyValueStorageFactory storageFactory = keyValueStorageFactory;
      storageService.registerKeyValueStorage(storageFactory);
      final KeyValueStorageProvider storageProvider =
          new KeyValueStorageProviderBuilder()
              .withStorageFactory(storageFactory)
              .withCommonConfiguration(commonPluginConfiguration)
              .withMetricsSystem(metricsSystem)
              .build();

      return storageProvider;
    }

    @Provides
    public MiningConfiguration provideMiningParameters(
        final TransactionSelectionServiceImpl transactionSelectionServiceImpl,
        final IdnNode node) {
      final var miningParameters =
          ImmutableMiningConfiguration.builder()
              .from(node.getMiningParameters())
              .transactionSelectionService(transactionSelectionServiceImpl)
              .build();

      return miningParameters;
    }

    @Provides
    @Inject
    IdnConfiguration provideIdnConfiguration(
        final Path dataDir, final MiningConfiguration miningConfiguration, final IdnNode node) {
      final IdnConfigurationImpl commonPluginConfiguration = new IdnConfigurationImpl();
      commonPluginConfiguration.init(
          dataDir, dataDir.resolve(DATABASE_PATH), node.getDataStorageConfiguration());
      commonPluginConfiguration.withMiningParameters(miningConfiguration);
      return commonPluginConfiguration;
    }
  }

  @Module
  public static class ObservableMetricsSystemModule {
    @Provides
    @Singleton
    public ObservableMetricsSystem provideObservableMetricsSystem() {
      return new NoOpMetricsSystem();
    }
  }

  @Module
  public static class MockIdnCommandModule {

    @Provides
    IdnCommand provideIdnCommand(final IdnPluginContextImpl pluginContext) {
      final IdnCommand idnCommand =
          new IdnCommand(
              RlpBlockImporter::new,
              JsonBlockImporter::new,
              Era1BlockImporter::new,
              RlpBlockExporter::new,
              new RunnerBuilder(),
              new IdnController.Builder(),
              pluginContext,
              System.getenv(),
              LoggerFactory.getLogger(MockIdnCommandModule.class));
      idnCommand.toCommandLine();
      return idnCommand;
    }

    @Provides
    @Named("idnCommandLogger")
    @Singleton
    Logger provideIdnCommandLogger() {
      return LoggerFactory.getLogger(MockIdnCommandModule.class);
    }
  }

  @Singleton
  @Component(
      modules = {
        ThreadIdnNodeRunner.IdnControllerModule.class,
        ThreadIdnNodeRunner.MockIdnCommandModule.class,
        ThreadIdnNodeRunner.ObservableMetricsSystemModule.class,
        ThreadIdnNodeRunnerModule.class,
        BonsaiCachedMerkleTrieLoaderModule.class,
        MetricsSystemModule.class,
        ThreadIdnNodeRunner.IdnNodeProviderModule.class,
        BlobCacheModule.class
      })
  public interface AcceptanceTestIdnComponent extends IdnComponent {
    IdnController idnController();

    IdnControllerBuilder idnControllerBuilder(); // TODO: needing this sucks

    EthNetworkConfig.Builder ethNetworkConfigBuilder();

    RpcEndpointServiceImpl rpcEndpointService();

    BlockchainServiceImpl blockchainService();

    ObservableMetricsSystem getObservableMetricsSystem();

    ThreadIdnNodeRunner getThreadIdnNodeRunner();
  }
}
