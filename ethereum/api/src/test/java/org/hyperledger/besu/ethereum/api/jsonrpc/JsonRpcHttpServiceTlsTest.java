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

import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.api.jsonrpc.RpcApis.DEFAULT_RPC_APIS;
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
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JsonRpcHttpServiceTlsTest {
  @TempDir private Path folder;

  protected static final Vertx vertx = Vertx.vertx();

  private static final String JSON_HEADER = "application/json; charset=utf-8";
  private static final String CLIENT_NODE_NAME = "TestClientVersion/0.1.0";
  private static final String CLIENT_VERSION = "0.1.0";
  private static final String CLIENT_COMMIT = "12345678";
  private static final BigInteger CHAIN_ID = BigInteger.valueOf(123);
  private static final NatService natService = new NatService(Optional.empty());
  private JsonRpcHttpService service;
  private String baseUrl;
  private Map<String, JsonRpcMethod> rpcMethods;
  private final JsonRpcTestHelper testHelper = new JsonRpcTestHelper();
  private final SelfSignedP12Certificate idnCertificate = SelfSignedP12Certificate.create();

  @BeforeEach
  public void initServer() throws Exception {
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
                folder,
                mock(EthPeers.class),
                vertx,
                mock(ApiConfiguration.class),
                Optional.empty(),
                mock(TransactionSimulator.class),
                new DeterministicEthScheduler());
    service = createJsonRpcHttpService(createJsonRpcConfig());
    service.start().join();
    baseUrl = service.url();
  }

  private JsonRpcHttpService createJsonRpcHttpService(final JsonRpcConfiguration jsonRpcConfig)
      throws Exception {
    return new JsonRpcHttpService(
        vertx,
        Files.createTempDirectory(folder, "newFolder"),
        jsonRpcConfig,
        new NoOpMetricsSystem(),
        natService,
        rpcMethods,
        HealthService.ALWAYS_HEALTHY,
        HealthService.ALWAYS_HEALTHY);
  }

  private JsonRpcConfiguration createJsonRpcConfig() {
    final JsonRpcConfiguration config = JsonRpcConfiguration.createDefault();
    config.setPort(0);
    config.setHostsAllowlist(Collections.singletonList("*"));
    config.setTlsConfiguration(getRpcHttpTlsConfiguration());
    return config;
  }

  private Optional<TlsConfiguration> getRpcHttpTlsConfiguration() {
    final TlsConfiguration tlsConfiguration =
        aTlsConfiguration()
            .withKeyStorePath(idnCertificate.getKeyStoreFile())
            .withKeyStorePasswordSupplier(
                new FileBasedPasswordProvider(createPasswordFile(idnCertificate.getPassword())))
            .build();

    return Optional.of(tlsConfiguration);
  }

  private Path createPasswordFile(final char[] password) {
    try {
      return Files.writeString(createTempFile(), new String(password));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path createTempFile() {
    try {
      return Files.createFile(folder.resolve("tempFile"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @AfterEach
  public void shutdownServer() {
    service.stop().join();
  }

  @Test
  public void netVersionSuccessfulOnTlsWithHttp1_1() throws Exception {
    netVersionSuccessfulOnTls(true);
  }

  @Test
  public void netVersionSuccessfulOnTlsWithHttp2() throws Exception {
    netVersionSuccessfulOnTls(false);
  }

  public void netVersionSuccessfulOnTls(final boolean useHttp1) throws Exception {
    final String id = "123";
    final String json =
        "{\"jsonrpc\":\"2.0\",\"id\":" + Json.encode(id) + ",\"method\":\"net_version\"}";

    final OkHttpClient httpClient = getTlsHttpClient(useHttp1);
    try (final Response response = httpClient.newCall(buildPostRequest(json)).execute()) {
      assertThat(response.code()).isEqualTo(200);
      assertThat(response.protocol()).isEqualTo(useHttp1 ? HTTP_1_1 : HTTP_2);
      // Check general format of result
      final ResponseBody body = response.body();
      assertThat(body).isNotNull();
      final JsonObject jsonObject = new JsonObject(body.string());
      testHelper.assertValidJsonRpcResult(jsonObject, id);
      // Check result
      final String result = jsonObject.getString("result");
      assertThat(result).isEqualTo(String.valueOf(CHAIN_ID));
    } catch (final Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  private OkHttpClient getTlsHttpClient(final boolean useHttp1) {
    return TlsOkHttpClientBuilder.anOkHttpClient()
        .withIdnCertificate(idnCertificate)
        .withHttp1(useHttp1)
        .build();
  }

  private Request buildPostRequest(final String json) {
    final RequestBody body = RequestBody.create(json, MediaType.parse(JSON_HEADER));
    return new Request.Builder().post(body).url(baseUrl).build();
  }
}
