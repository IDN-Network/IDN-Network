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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.idnecology.idn.ethereum.api.jsonrpc.RpcApis.DEFAULT_RPC_APIS;
import static org.idnecology.idn.ethereum.api.tls.KnownClientFileUtil.writeToKnownClientsFile;
import static org.idnecology.idn.ethereum.api.tls.TlsClientAuthConfiguration.Builder.aTlsClientAuthConfiguration;
import static org.idnecology.idn.ethereum.api.tls.TlsConfiguration.Builder.aTlsConfiguration;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.health.HealthService;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.JsonRpcMethodsFactory;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.tls.FileBasedPasswordProvider;
import org.idnecology.idn.ethereum.api.tls.SelfSignedP12Certificate;
import org.idnecology.idn.ethereum.api.tls.TlsConfiguration;
import org.idnecology.idn.ethereum.blockcreation.PoWMiningCoordinator;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.EthProtocol;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.network.P2PNetwork;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.nat.NatService;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import io.vertx.core.Vertx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;

class JsonRpcHttpServiceTlsMisconfigurationTest {

  protected static final Vertx vertx = Vertx.vertx();

  private static final String CLIENT_NODE_NAME = "TestClientVersion/0.1.0";
  private static final String CLIENT_VERSION = "0.1.0";
  private static final String CLIENT_COMMIT = "12345678";
  private static final BigInteger CHAIN_ID = BigInteger.valueOf(123);
  private static final NatService natService = new NatService(Optional.empty());
  private final SelfSignedP12Certificate idnCertificate = SelfSignedP12Certificate.create();
  @TempDir private Path tempDir;
  private Path knownClientsFile;
  private Map<String, JsonRpcMethod> rpcMethods;
  private JsonRpcHttpService service;

  @BeforeEach
  public void beforeEach() {
    knownClientsFile = tempDir.resolve("knownClients");
    writeToKnownClientsFile(
        idnCertificate.getCommonName(),
        idnCertificate.getCertificateHexFingerprint(),
        knownClientsFile);
    final P2PNetwork peerDiscoveryMock = mock(P2PNetwork.class);
    final BlockchainQueries blockchainQueries = mock(BlockchainQueries.class);
    final Synchronizer synchronizer = mock(Synchronizer.class);

    final Set<Capability> supportedCapabilities = new HashSet<>();
    supportedCapabilities.add(EthProtocol.ETH62);
    supportedCapabilities.add(EthProtocol.ETH63);

    rpcMethods =
        new JsonRpcMethodsFactory()
            .methods(
                CLIENT_NODE_NAME,
                CLIENT_VERSION,
                CLIENT_COMMIT,
                CHAIN_ID,
                new StubGenesisConfigOptions(),
                peerDiscoveryMock,
                blockchainQueries,
                synchronizer,
                MainnetProtocolSchedule.fromConfig(
                    new StubGenesisConfigOptions().constantinopleBlock(0).chainId(CHAIN_ID),
                    MiningConfiguration.MINING_DISABLED,
                    new BadBlockManager(),
                    false,
                    new NoOpMetricsSystem()),
                mock(ProtocolContext.class),
                mock(FilterManager.class),
                mock(TransactionPool.class),
                mock(MiningConfiguration.class),
                mock(PoWMiningCoordinator.class),
                new NoOpMetricsSystem(),
                supportedCapabilities,
                Optional.of(mock(AccountLocalConfigPermissioningController.class)),
                Optional.of(mock(NodeLocalConfigPermissioningController.class)),
                DEFAULT_RPC_APIS,
                mock(PrivacyParameters.class),
                mock(JsonRpcConfiguration.class),
                mock(WebSocketConfiguration.class),
                mock(MetricsConfiguration.class),
                mock(GraphQLConfiguration.class),
                natService,
                Collections.emptyMap(),
                tempDir.getRoot(),
                mock(EthPeers.class),
                vertx,
                mock(ApiConfiguration.class),
                Optional.empty(),
                mock(TransactionSimulator.class),
                new DeterministicEthScheduler());
  }

  @AfterEach
  public void shutdownServer() {
    Optional.ofNullable(service).ifPresent(s -> service.stop().join());
  }

  @Test
  void exceptionRaisedWhenNonExistentKeystoreFileIsSpecified() {
    Assertions.setMaxStackTraceElementsDisplayed(60);
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKeystorePathTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class);
  }

  @Test
  void exceptionRaisedWhenIncorrectKeystorePasswordIsSpecified() {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidPasswordTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("keystore password was incorrect");
  }

  @Test
  void exceptionRaisedWhenIncorrectKeystorePasswordFileIsSpecified() {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidPasswordFileTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Unable to read keystore password file");
  }

  @Test
  @DisabledOnJre({JRE.JAVA_17, JRE.JAVA_18}) // error message changed
  void exceptionRaisedWhenInvalidKeystoreFileIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKeystoreFileTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Tag number over 30 is not supported");
  }

  @Test
  @DisabledOnJre({JRE.JAVA_11, JRE.JAVA_12, JRE.JAVA_13, JRE.JAVA_14, JRE.JAVA_15, JRE.JAVA_16})
  void exceptionRaisedWhenInvalidKeystoreFileIsSpecified_NewJava() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKeystoreFileTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Tag number over 30 is not supported");
  }

  @Test
  void exceptionRaisedWhenInvalidKnownClientsFileIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKnownClientsTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Invalid fingerprint in");
  }

  private TlsConfiguration invalidKeystoreFileTlsConfiguration() throws IOException {
    final Path tempFile = tempDir.resolve("invalidKeystoreFileTlsConfig");
    Files.createFile(tempFile);
    return aTlsConfiguration()
        .withKeyStorePath(tempFile)
        .withKeyStorePasswordSupplier(() -> "invalid_password")
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidKeystorePathTlsConfiguration() {
    return aTlsConfiguration()
        .withKeyStorePath(Path.of("/tmp/invalidkeystore.pfx"))
        .withKeyStorePasswordSupplier(() -> "invalid_password")
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidPasswordTlsConfiguration() {
    return aTlsConfiguration()
        .withKeyStorePath(idnCertificate.getKeyStoreFile())
        .withKeyStorePasswordSupplier(() -> "invalid_password")
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidPasswordFileTlsConfiguration() {
    return TlsConfiguration.Builder.aTlsConfiguration()
        .withKeyStorePath(idnCertificate.getKeyStoreFile())
        .withKeyStorePasswordSupplier(
            new FileBasedPasswordProvider(Path.of("/tmp/invalid_password_file.txt")))
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidKnownClientsTlsConfiguration() throws IOException {
    final Path tempKnownClientsFile = tempDir.resolve("invalidKnownClientsTlsConfiguration");
    Files.write(tempKnownClientsFile, List.of("cn invalid_sha256"));

    return TlsConfiguration.Builder.aTlsConfiguration()
        .withKeyStorePath(idnCertificate.getKeyStoreFile())
        .withKeyStorePasswordSupplier(() -> new String(idnCertificate.getPassword()))
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(tempKnownClientsFile).build())
        .build();
  }

  private JsonRpcHttpService createJsonRpcHttpService(
      final Map<String, JsonRpcMethod> rpcMethods, final JsonRpcConfiguration jsonRpcConfig) {
    final Path testDir = tempDir.resolve("createJsonRpcHttpSercice");
    testDir.toFile().mkdirs();
    return new JsonRpcHttpService(
        vertx,
        tempDir,
        jsonRpcConfig,
        new NoOpMetricsSystem(),
        natService,
        rpcMethods,
        HealthService.ALWAYS_HEALTHY,
        HealthService.ALWAYS_HEALTHY);
  }

  private JsonRpcConfiguration createJsonRpcConfig(final TlsConfiguration tlsConfiguration) {
    final JsonRpcConfiguration config = JsonRpcConfiguration.createDefault();
    config.setPort(0);
    config.setHostsAllowlist(Collections.singletonList("*"));
    config.setTlsConfiguration(Optional.of(tlsConfiguration));
    return config;
  }
}
