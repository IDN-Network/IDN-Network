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
package org.idnecology.idn.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.cli.config.NetworkName.DEV;
import static org.idnecology.idn.cli.config.NetworkName.MAINNET;
import static org.idnecology.idn.ethereum.api.jsonrpc.RpcApis.ETH;
import static org.idnecology.idn.ethereum.api.jsonrpc.RpcApis.WEB3;
import static org.idnecology.idn.ethereum.p2p.config.DefaultDiscoveryConfiguration.MAINNET_BOOTSTRAP_NODES;
import static org.idnecology.idn.ethereum.p2p.config.DefaultDiscoveryConfiguration.MAINNET_DISCOVERY_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.p2p.peers.EnodeURLImpl;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.plugin.data.EnodeURL;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.google.common.io.Resources;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

public class CascadingDefaultProviderTest extends CommandTestAbstract {
  private static final int GENESIS_CONFIG_TEST_CHAINID = 3141592;
  private static final String VALID_NODE_ID =
      "6f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0";
  private static final JsonObject GENESIS_VALID_JSON =
      (new JsonObject())
          .put("config", (new JsonObject()).put("chainId", GENESIS_CONFIG_TEST_CHAINID));

  /**
   * Test if the default values are overridden if the key is present in the configuration file. The
   * test checks if the configuration file correctly overrides the default values for various
   * settings, such as the JSON-RPC configuration, GraphQL configuration, WebSocket configuration,
   * and metrics configuration.
   */
  @Test
  public void overrideDefaultValuesIfKeyIsPresentInConfigFile(final @TempDir File dataFolder)
      throws IOException {
    final URL configFile = this.getClass().getResource("/complete_config.toml");
    final Path genesisFile = createFakeGenesisFile(GENESIS_VALID_JSON);
    final String updatedConfig =
        Resources.toString(configFile, UTF_8)
            .replace("/opt/idn/genesis.json", escapeTomlString(genesisFile.toString()))
            .replace(
                "data-path=\"/opt/idn\"",
                "data-path=\"" + escapeTomlString(dataFolder.getPath()) + "\"");

    final Path toml = createTempFile("toml", updatedConfig.getBytes(UTF_8));

    final List<String> expectedApis = asList(ETH.name(), WEB3.name());

    final JsonRpcConfiguration jsonRpcConfiguration = JsonRpcConfiguration.createDefault();
    jsonRpcConfiguration.setEnabled(false);
    jsonRpcConfiguration.setHost("5.6.7.8");
    jsonRpcConfiguration.setPort(5678);
    jsonRpcConfiguration.setCorsAllowedDomains(Collections.emptyList());
    jsonRpcConfiguration.setRpcApis(expectedApis);
    jsonRpcConfiguration.setMaxActiveConnections(1000);

    final GraphQLConfiguration graphQLConfiguration = GraphQLConfiguration.createDefault();
    graphQLConfiguration.setEnabled(false);
    graphQLConfiguration.setHost("6.7.8.9");
    graphQLConfiguration.setPort(6789);

    final WebSocketConfiguration webSocketConfiguration = WebSocketConfiguration.createDefault();
    webSocketConfiguration.setEnabled(false);
    webSocketConfiguration.setHost("9.10.11.12");
    webSocketConfiguration.setPort(9101);
    webSocketConfiguration.setRpcApis(expectedApis);

    final MetricsConfiguration metricsConfiguration =
        MetricsConfiguration.builder().enabled(false).host("8.6.7.5").port(309).build();

    parseCommand("--config-file", toml.toString());

    verify(mockRunnerBuilder).discoveryEnabled(eq(false));
    verify(mockRunnerBuilder).ethNetworkConfig(ethNetworkConfigArgumentCaptor.capture());
    verify(mockRunnerBuilder).p2pAdvertisedHost(eq("1.2.3.4"));
    verify(mockRunnerBuilder).p2pListenPort(eq(1234));
    verify(mockRunnerBuilder).jsonRpcConfiguration(eq(jsonRpcConfiguration));
    verify(mockRunnerBuilder).graphQLConfiguration(eq(graphQLConfiguration));
    verify(mockRunnerBuilder).webSocketConfiguration(eq(webSocketConfiguration));
    verify(mockRunnerBuilder).metricsConfiguration(eq(metricsConfiguration));
    verify(mockRunnerBuilder).build();

    final List<EnodeURL> nodes =
        asList(
            EnodeURLImpl.fromString("enode://" + VALID_NODE_ID + "@192.168.0.1:4567"),
            EnodeURLImpl.fromString("enode://" + VALID_NODE_ID + "@192.168.0.1:4567"),
            EnodeURLImpl.fromString("enode://" + VALID_NODE_ID + "@192.168.0.1:4567"));
    assertThat(ethNetworkConfigArgumentCaptor.getValue().bootNodes()).isEqualTo(nodes);

    final EthNetworkConfig networkConfig =
        new EthNetworkConfig.Builder(EthNetworkConfig.getNetworkConfig(MAINNET))
            .setNetworkId(BigInteger.valueOf(42))
            .setGenesisConfig(GenesisConfig.fromConfig(encodeJsonGenesis(GENESIS_VALID_JSON)))
            .setBootNodes(nodes)
            .setDnsDiscoveryUrl(null)
            .build();
    verify(mockControllerBuilder).dataDirectory(eq(dataFolder.toPath()));
    verify(mockControllerBuilderFactory).fromEthNetworkConfig(eq(networkConfig), any());
    verify(mockControllerBuilder).synchronizerConfiguration(syncConfigurationCaptor.capture());

    assertThat(syncConfigurationCaptor.getValue().getSyncMode()).isEqualTo(SyncMode.FAST);
    assertThat(syncConfigurationCaptor.getValue().getSyncMinimumPeerCount()).isEqualTo(13);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  /**
   * Test if the default values are not overridden if the key is not present in the configuration
   * file. The test checks if the default values for various settings remain unchanged when the
   * corresponding keys are not present in the configuration file.
   */
  @Test
  public void noOverrideDefaultValuesIfKeyIsNotPresentInConfigFile() {
    final String configFile = this.getClass().getResource("/partial_config.toml").getFile();

    parseCommand("--config-file", configFile);
    final JsonRpcConfiguration jsonRpcConfiguration = JsonRpcConfiguration.createDefault();

    final GraphQLConfiguration graphQLConfiguration = GraphQLConfiguration.createDefault();

    final WebSocketConfiguration webSocketConfiguration = WebSocketConfiguration.createDefault();

    final MetricsConfiguration metricsConfiguration = MetricsConfiguration.builder().build();

    verify(mockRunnerBuilder).discoveryEnabled(eq(true));
    verify(mockRunnerBuilder)
        .ethNetworkConfig(
            new EthNetworkConfig(
                GenesisConfig.fromResource(MAINNET.getGenesisFile()),
                MAINNET.getNetworkId(),
                MAINNET_BOOTSTRAP_NODES,
                MAINNET_DISCOVERY_URL));
    verify(mockRunnerBuilder).p2pAdvertisedHost(eq("127.0.0.1"));
    verify(mockRunnerBuilder).p2pListenPort(eq(30303));
    verify(mockRunnerBuilder).jsonRpcConfiguration(eq(jsonRpcConfiguration));
    verify(mockRunnerBuilder).graphQLConfiguration(eq(graphQLConfiguration));
    verify(mockRunnerBuilder).webSocketConfiguration(eq(webSocketConfiguration));
    verify(mockRunnerBuilder).metricsConfiguration(eq(metricsConfiguration));
    verify(mockRunnerBuilder).build();
    verify(mockControllerBuilder).build();
    verify(mockControllerBuilder).synchronizerConfiguration(syncConfigurationCaptor.capture());

    final SynchronizerConfiguration syncConfig = syncConfigurationCaptor.getValue();
    assertThat(syncConfig.getSyncMode()).isEqualTo(SyncMode.SNAP);
    assertThat(syncConfig.getSyncMinimumPeerCount()).isEqualTo(5);

    assertThat(commandOutput.toString(UTF_8)).isEmpty();
    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
  }

  /**
   * Test if the environment variable overrides the value from the configuration file. The test
   * checks if the value of the miner's coinbase address set through an environment variable
   * correctly overrides the value specified in the configuration file.
   */
  @Test
  public void envVariableOverridesValueFromConfigFile() {
    final String configFile = this.getClass().getResource("/partial_config.toml").getFile();
    final String expectedCoinbase = "0x0000000000000000000000000000000000000004";
    setEnvironmentVariable("BESU_MINER_COINBASE", expectedCoinbase);
    parseCommand("--config-file", configFile);

    final var captMiningParameters = ArgumentCaptor.forClass(MiningConfiguration.class);
    verify(mockControllerBuilder).miningParameters(captMiningParameters.capture());

    assertThat(captMiningParameters.getValue().getCoinbase())
        .contains(Address.fromHexString(expectedCoinbase));
  }

  /**
   * Test if the command line option overrides the environment variable and configuration. The test
   * checks if the value of the miner's coinbase address set through a command line option correctly
   * overrides the value specified in the environment variable and the configuration file.
   */
  @Test
  public void cliOptionOverridesEnvVariableAndConfig() {
    final String configFile = this.getClass().getResource("/partial_config.toml").getFile();
    final String expectedCoinbase = "0x0000000000000000000000000000000000000006";
    setEnvironmentVariable("BESU_MINER_COINBASE", "0x0000000000000000000000000000000000000004");
    parseCommand("--config-file", configFile, "--miner-coinbase", expectedCoinbase);

    final var captMiningParameters = ArgumentCaptor.forClass(MiningConfiguration.class);
    verify(mockControllerBuilder).miningParameters(captMiningParameters.capture());

    assertThat(captMiningParameters.getValue().getCoinbase())
        .contains(Address.fromHexString(expectedCoinbase));
  }

  /**
   * Test if the profile option sets the correct defaults. The test checks if the 'dev' profile
   * correctly sets the network ID to the expected value.
   */
  @Test
  public void profileOptionShouldSetCorrectDefaults() {
    final ArgumentCaptor<EthNetworkConfig> networkArg =
        ArgumentCaptor.forClass(EthNetworkConfig.class);

    parseCommand("--profile", "dev");
    verify(mockControllerBuilderFactory).fromEthNetworkConfig(networkArg.capture(), any());
    verify(mockControllerBuilder).build();

    final EthNetworkConfig config = networkArg.getValue();
    assertThat(config.networkId()).isEqualTo(DEV.getNetworkId());
  }

  /**
   * Test if the command line option overrides the profile configuration. The test checks if the
   * network ID set through a command line option correctly overrides the value specified in the
   * 'dev' profile.
   */
  @Test
  public void cliOptionOverridesProfileConfiguration() {
    final ArgumentCaptor<EthNetworkConfig> networkArg =
        ArgumentCaptor.forClass(EthNetworkConfig.class);

    parseCommand("--profile", "dev", "--network", "MAINNET");
    verify(mockControllerBuilderFactory).fromEthNetworkConfig(networkArg.capture(), any());
    verify(mockControllerBuilder).build();

    final EthNetworkConfig config = networkArg.getValue();
    assertThat(config.networkId()).isEqualTo(MAINNET.getNetworkId());
  }

  /**
   * Test if the configuration file overrides the profile configuration. The test checks if the
   * network ID specified in the configuration file correctly overrides the value specified in the
   * 'dev' profile.
   */
  @Test
  public void configFileOverridesProfileConfiguration() {
    final ArgumentCaptor<EthNetworkConfig> networkArg =
        ArgumentCaptor.forClass(EthNetworkConfig.class);

    final String configFile = this.getClass().getResource("/partial_config.toml").getFile();
    parseCommand("--profile", "dev", "--config-file", configFile);
    verify(mockControllerBuilderFactory).fromEthNetworkConfig(networkArg.capture(), any());
    verify(mockControllerBuilder).build();

    final EthNetworkConfig config = networkArg.getValue();
    assertThat(config.networkId()).isEqualTo(MAINNET.getNetworkId());
  }

  /**
   * Test if the environment variable overrides the profile configuration. The test checks if the
   * network ID set through an environment variable correctly overrides the value specified in the
   * 'dev' profile.
   */
  @Test
  public void environmentVariableOverridesProfileConfiguration() {
    final ArgumentCaptor<EthNetworkConfig> networkArg =
        ArgumentCaptor.forClass(EthNetworkConfig.class);
    setEnvironmentVariable("BESU_NETWORK", "MAINNET");
    parseCommand("--profile", "dev");
    verify(mockControllerBuilderFactory).fromEthNetworkConfig(networkArg.capture(), any());
    verify(mockControllerBuilder).build();

    final EthNetworkConfig config = networkArg.getValue();
    assertThat(config.networkId()).isEqualTo(MAINNET.getNetworkId());
  }
}
