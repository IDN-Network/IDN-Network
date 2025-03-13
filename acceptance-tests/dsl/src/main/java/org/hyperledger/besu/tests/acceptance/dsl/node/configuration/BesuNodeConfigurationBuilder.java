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
package org.idnecology.idn.tests.acceptance.dsl.node.configuration;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;

import org.idnecology.idn.cli.config.NetworkName;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.ImmutableInProcessRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.InProcessRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.authentication.JwtAlgorithm;
import org.idnecology.idn.ethereum.api.jsonrpc.ipc.JsonRpcIpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.permissioning.PermissioningConfiguration;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.plugin.services.storage.KeyValueStorageFactory;
import org.idnecology.idn.tests.acceptance.dsl.node.configuration.genesis.GenesisConfigurationProvider;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IdnNodeConfigurationBuilder {

  private String name;
  private Optional<Path> dataPath = Optional.empty();
  private MiningConfiguration miningConfiguration =
      ImmutableMiningConfiguration.builder()
          .mutableInitValues(
              MutableInitValues.builder().coinbase(AddressHelpers.ofValue(1)).build())
          .build();
  private TransactionPoolConfiguration transactionPoolConfiguration =
      TransactionPoolConfiguration.DEFAULT;
  private JsonRpcConfiguration jsonRpcConfiguration = JsonRpcConfiguration.createDefault();
  private JsonRpcConfiguration engineRpcConfiguration = JsonRpcConfiguration.createEngineDefault();
  private WebSocketConfiguration webSocketConfiguration = WebSocketConfiguration.createDefault();
  private JsonRpcIpcConfiguration jsonRpcIpcConfiguration = new JsonRpcIpcConfiguration();
  private InProcessRpcConfiguration inProcessRpcConfiguration =
      ImmutableInProcessRpcConfiguration.builder().build();
  private MetricsConfiguration metricsConfiguration = MetricsConfiguration.builder().build();
  private Optional<PermissioningConfiguration> permissioningConfiguration = Optional.empty();
  private ApiConfiguration apiConfiguration = ImmutableApiConfiguration.builder().build();
  private DataStorageConfiguration dataStorageConfiguration =
      DataStorageConfiguration.DEFAULT_FOREST_CONFIG;
  private String keyFilePath = null;
  private boolean devMode = true;
  private GenesisConfigurationProvider genesisConfigProvider = ignore -> Optional.empty();
  private Boolean p2pEnabled = true;
  private int p2pPort = 0;
  private final NetworkingConfiguration networkingConfiguration = NetworkingConfiguration.create();
  private boolean discoveryEnabled = true;
  private boolean bootnodeEligible = true;
  private boolean revertReasonEnabled = false;
  private NetworkName network = null;
  private boolean secp256K1Native = true;
  private boolean altbn128Native = true;
  private final List<String> plugins = new ArrayList<>();
  private final List<String> requestedPlugins = new ArrayList<>();
  private final List<String> extraCLIOptions = new ArrayList<>();
  private List<String> staticNodes = new ArrayList<>();
  private boolean isDnsEnabled = false;
  private Optional<PrivacyParameters> privacyParameters = Optional.empty();
  private List<String> runCommand = new ArrayList<>();
  private Optional<KeyPair> keyPair = Optional.empty();
  private Boolean strictTxReplayProtectionEnabled = false;
  private Map<String, String> environment = new HashMap<>();
  private SynchronizerConfiguration synchronizerConfiguration;
  private Optional<KeyValueStorageFactory> storageImplementation = Optional.empty();

  public IdnNodeConfigurationBuilder() {
    // Check connections more frequently during acceptance tests to cut down on
    // intermittent failures due to the fact that we're running over a real network
    networkingConfiguration.setInitiateConnectionsFrequency(5);
  }

  public IdnNodeConfigurationBuilder name(final String name) {
    this.name = name;
    return this;
  }

  public IdnNodeConfigurationBuilder dataPath(final Path dataPath) {
    checkNotNull(dataPath);
    this.dataPath = Optional.of(dataPath);
    return this;
  }

  public IdnNodeConfigurationBuilder miningEnabled() {
    return miningEnabled(true);
  }

  public IdnNodeConfigurationBuilder miningEnabled(final boolean enabled) {
    this.miningConfiguration = miningConfiguration.setMiningEnabled(enabled);
    this.jsonRpcConfiguration.addRpcApi(RpcApis.MINER.name());
    return this;
  }

  public IdnNodeConfigurationBuilder miningConfiguration(
      final MiningConfiguration miningConfiguration) {
    this.miningConfiguration = miningConfiguration;
    this.jsonRpcConfiguration.addRpcApi(RpcApis.MINER.name());
    return this;
  }

  public IdnNodeConfigurationBuilder transactionPoolConfiguration(
      final TransactionPoolConfiguration transactionPoolConfiguration) {
    this.transactionPoolConfiguration = transactionPoolConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcConfiguration(
      final JsonRpcConfiguration jsonRpcConfiguration) {
    this.jsonRpcConfiguration = jsonRpcConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder engineJsonRpcConfiguration(
      final JsonRpcConfiguration engineConfig) {
    this.engineRpcConfiguration = engineConfig;
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcEnabled() {
    this.jsonRpcConfiguration.setEnabled(true);
    this.jsonRpcConfiguration.setPort(0);
    this.jsonRpcConfiguration.setHostsAllowlist(singletonList("*"));

    return this;
  }

  public IdnNodeConfigurationBuilder engineRpcEnabled(final boolean enabled) {
    this.engineRpcConfiguration.setEnabled(enabled);
    this.engineRpcConfiguration.setPort(0);
    this.engineRpcConfiguration.setHostsAllowlist(singletonList("*"));
    this.engineRpcConfiguration.setAuthenticationEnabled(false);

    return this;
  }

  public IdnNodeConfigurationBuilder metricsEnabled() {
    this.metricsConfiguration =
        MetricsConfiguration.builder()
            .enabled(true)
            .port(0)
            .hostsAllowlist(singletonList("*"))
            .build();

    return this;
  }

  public IdnNodeConfigurationBuilder enablePrivateTransactions() {
    this.jsonRpcConfiguration.addRpcApi(RpcApis.EEA.name());
    this.jsonRpcConfiguration.addRpcApi(RpcApis.PRIV.name());
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcTxPool() {
    this.jsonRpcConfiguration.addRpcApi(RpcApis.TXPOOL.name());
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcAdmin() {
    this.jsonRpcConfiguration.addRpcApi(RpcApis.ADMIN.name());
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcDebug() {
    this.jsonRpcConfiguration.addRpcApi(RpcApis.DEBUG.name());
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcAuthenticationConfiguration(final String authFile)
      throws URISyntaxException {
    final String authTomlPath =
        Paths.get(ClassLoader.getSystemResource(authFile).toURI()).toAbsolutePath().toString();

    this.jsonRpcConfiguration.setAuthenticationEnabled(true);
    this.jsonRpcConfiguration.setAuthenticationCredentialsFile(authTomlPath);

    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcAuthenticationConfiguration(
      final String authFile, final List<String> noAuthApiMethods) throws URISyntaxException {
    final String authTomlPath =
        Paths.get(ClassLoader.getSystemResource(authFile).toURI()).toAbsolutePath().toString();

    this.jsonRpcConfiguration.setAuthenticationEnabled(true);
    this.jsonRpcConfiguration.setAuthenticationCredentialsFile(authTomlPath);
    this.jsonRpcConfiguration.setNoAuthRpcApis(noAuthApiMethods);

    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcAuthenticationUsingRSA() throws URISyntaxException {
    final File jwtPublicKey =
        Paths.get(ClassLoader.getSystemResource("authentication/jwt_public_key_rsa").toURI())
            .toAbsolutePath()
            .toFile();

    this.jsonRpcConfiguration.setAuthenticationEnabled(true);
    this.jsonRpcConfiguration.setAuthenticationPublicKeyFile(jwtPublicKey);

    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcAuthenticationUsingECDSA() throws URISyntaxException {
    final File jwtPublicKey =
        Paths.get(ClassLoader.getSystemResource("authentication/jwt_public_key_ecdsa").toURI())
            .toAbsolutePath()
            .toFile();

    this.jsonRpcConfiguration.setAuthenticationEnabled(true);
    this.jsonRpcConfiguration.setAuthenticationPublicKeyFile(jwtPublicKey);
    this.jsonRpcConfiguration.setAuthenticationAlgorithm(JwtAlgorithm.ES256);

    return this;
  }

  public IdnNodeConfigurationBuilder webSocketConfiguration(
      final WebSocketConfiguration webSocketConfiguration) {
    this.webSocketConfiguration = webSocketConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder jsonRpcIpcConfiguration(
      final JsonRpcIpcConfiguration jsonRpcIpcConfiguration) {
    this.jsonRpcIpcConfiguration = jsonRpcIpcConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder inProcessRpcConfiguration(
      final InProcessRpcConfiguration inProcessRpcConfiguration) {
    this.inProcessRpcConfiguration = inProcessRpcConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder metricsConfiguration(
      final MetricsConfiguration metricsConfiguration) {
    this.metricsConfiguration = metricsConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder network(final NetworkName network) {
    this.network = network;
    return this;
  }

  public IdnNodeConfigurationBuilder webSocketEnabled() {
    final WebSocketConfiguration config = WebSocketConfiguration.createDefault();
    config.setEnabled(true);
    config.setPort(0);
    config.setHostsAllowlist(Collections.singletonList("*"));

    this.webSocketConfiguration = config;
    return this;
  }

  public IdnNodeConfigurationBuilder bootnodeEligible(final boolean bootnodeEligible) {
    this.bootnodeEligible = bootnodeEligible;
    return this;
  }

  public IdnNodeConfigurationBuilder webSocketAuthenticationEnabled() throws URISyntaxException {
    final String authTomlPath =
        Paths.get(ClassLoader.getSystemResource("authentication/auth.toml").toURI())
            .toAbsolutePath()
            .toString();

    this.webSocketConfiguration.setAuthenticationEnabled(true);
    this.webSocketConfiguration.setAuthenticationCredentialsFile(authTomlPath);

    return this;
  }

  public IdnNodeConfigurationBuilder webSocketAuthenticationEnabledWithNoAuthMethods(
      final List<String> noAuthApiMethods) throws URISyntaxException {
    final String authTomlPath =
        Paths.get(ClassLoader.getSystemResource("authentication/auth.toml").toURI())
            .toAbsolutePath()
            .toString();

    this.webSocketConfiguration.setAuthenticationEnabled(true);
    this.webSocketConfiguration.setAuthenticationCredentialsFile(authTomlPath);
    this.webSocketConfiguration.setRpcApisNoAuth(noAuthApiMethods);

    return this;
  }

  public IdnNodeConfigurationBuilder webSocketAuthenticationUsingRsaPublicKeyEnabled()
      throws URISyntaxException {
    final File jwtPublicKey =
        Paths.get(ClassLoader.getSystemResource("authentication/jwt_public_key_rsa").toURI())
            .toAbsolutePath()
            .toFile();

    this.webSocketConfiguration.setAuthenticationEnabled(true);
    this.webSocketConfiguration.setAuthenticationPublicKeyFile(jwtPublicKey);

    return this;
  }

  public IdnNodeConfigurationBuilder webSocketAuthenticationUsingEcdsaPublicKeyEnabled()
      throws URISyntaxException {
    final File jwtPublicKey =
        Paths.get(ClassLoader.getSystemResource("authentication/jwt_public_key_ecdsa").toURI())
            .toAbsolutePath()
            .toFile();

    this.webSocketConfiguration.setAuthenticationEnabled(true);
    this.webSocketConfiguration.setAuthenticationPublicKeyFile(jwtPublicKey);
    this.webSocketConfiguration.setAuthenticationAlgorithm(JwtAlgorithm.ES256);

    return this;
  }

  public IdnNodeConfigurationBuilder permissioningConfiguration(
      final PermissioningConfiguration permissioningConfiguration) {
    this.permissioningConfiguration = Optional.of(permissioningConfiguration);
    return this;
  }

  public IdnNodeConfigurationBuilder keyFilePath(final String keyFilePath) {
    this.keyFilePath = keyFilePath;
    return this;
  }

  public IdnNodeConfigurationBuilder devMode(final boolean devMode) {
    this.devMode = devMode;
    return this;
  }

  public IdnNodeConfigurationBuilder genesisConfigProvider(
      final GenesisConfigurationProvider genesisConfigProvider) {
    this.genesisConfigProvider = genesisConfigProvider;
    return this;
  }

  public IdnNodeConfigurationBuilder p2pEnabled(final Boolean p2pEnabled) {
    this.p2pEnabled = p2pEnabled;
    return this;
  }

  public IdnNodeConfigurationBuilder p2pPort(final int p2pPort) {
    this.p2pPort = p2pPort;
    return this;
  }

  public IdnNodeConfigurationBuilder discoveryEnabled(final boolean discoveryEnabled) {
    this.discoveryEnabled = discoveryEnabled;
    return this;
  }

  public IdnNodeConfigurationBuilder plugins(final List<String> plugins) {
    this.plugins.clear();
    this.plugins.addAll(plugins);
    return this;
  }

  public IdnNodeConfigurationBuilder requestedPlugins(final List<String> requestedPlugins) {
    this.requestedPlugins.clear();
    this.requestedPlugins.addAll(requestedPlugins);
    return this;
  }

  public IdnNodeConfigurationBuilder extraCLIOptions(final List<String> extraCLIOptions) {
    this.extraCLIOptions.clear();
    this.extraCLIOptions.addAll(extraCLIOptions);
    return this;
  }

  public IdnNodeConfigurationBuilder revertReasonEnabled() {
    this.revertReasonEnabled = true;
    return this;
  }

  public IdnNodeConfigurationBuilder secp256k1Java() {
    this.secp256K1Native = true;
    return this;
  }

  public IdnNodeConfigurationBuilder altbn128Java() {
    this.altbn128Native = true;
    return this;
  }

  public IdnNodeConfigurationBuilder staticNodes(final List<String> staticNodes) {
    this.staticNodes = staticNodes;
    return this;
  }

  public IdnNodeConfigurationBuilder dnsEnabled(final boolean isDnsEnabled) {
    this.isDnsEnabled = isDnsEnabled;
    return this;
  }

  public IdnNodeConfigurationBuilder privacyParameters(final PrivacyParameters privacyParameters) {
    this.privacyParameters = Optional.ofNullable(privacyParameters);
    return this;
  }

  public IdnNodeConfigurationBuilder keyPair(final KeyPair keyPair) {
    this.keyPair = Optional.of(keyPair);
    return this;
  }

  public IdnNodeConfigurationBuilder run(final String... commands) {
    this.runCommand = List.of(commands);
    return this;
  }

  public IdnNodeConfigurationBuilder strictTxReplayProtectionEnabled(
      final Boolean strictTxReplayProtectionEnabled) {
    this.strictTxReplayProtectionEnabled = strictTxReplayProtectionEnabled;
    return this;
  }

  public IdnNodeConfigurationBuilder environment(final Map<String, String> environment) {
    this.environment = environment;
    return this;
  }

  public IdnNodeConfigurationBuilder apiConfiguration(final ApiConfiguration apiConfiguration) {
    this.apiConfiguration = apiConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder dataStorageConfiguration(
      final DataStorageConfiguration dataStorageConfiguration) {
    this.dataStorageConfiguration = dataStorageConfiguration;
    return this;
  }

  public IdnNodeConfigurationBuilder storageImplementation(
      final KeyValueStorageFactory storageFactory) {
    this.storageImplementation = Optional.of(storageFactory);
    return this;
  }

  public IdnNodeConfigurationBuilder synchronizerConfiguration(
      final SynchronizerConfiguration config) {
    this.synchronizerConfiguration = config;
    return this;
  }

  public IdnNodeConfiguration build() {
    if (name == null) {
      throw new IllegalStateException("Name is required");
    }

    return new IdnNodeConfiguration(
        name,
        dataPath,
        miningConfiguration,
        transactionPoolConfiguration,
        jsonRpcConfiguration,
        Optional.of(engineRpcConfiguration),
        webSocketConfiguration,
        jsonRpcIpcConfiguration,
        inProcessRpcConfiguration,
        metricsConfiguration,
        permissioningConfiguration,
        apiConfiguration,
        dataStorageConfiguration,
        Optional.ofNullable(keyFilePath),
        devMode,
        network,
        genesisConfigProvider,
        p2pEnabled,
        p2pPort,
        networkingConfiguration,
        discoveryEnabled,
        bootnodeEligible,
        revertReasonEnabled,
        secp256K1Native,
        altbn128Native,
        plugins,
        requestedPlugins,
        extraCLIOptions,
        staticNodes,
        isDnsEnabled,
        privacyParameters,
        runCommand,
        keyPair,
        strictTxReplayProtectionEnabled,
        environment,
        synchronizerConfiguration,
        storageImplementation);
  }
}
