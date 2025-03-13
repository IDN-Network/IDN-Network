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
package org.idnecology.idn.cli.options;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NetworkingOptionsTest
    extends AbstractCLIOptionsTest<NetworkingConfiguration, NetworkingOptions> {

  @Test
  public void checkMaintainedConnectionsFrequencyFlag_isSet() {
    final TestIdnCommand cmd = parseCommand("--Xp2p-check-maintained-connections-frequency", "2");

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getCheckMaintainedConnectionsFrequencySec()).isEqualTo(2);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkMaintainedFrequencyConnectionsFlag_isNotSet() {
    final TestIdnCommand cmd = parseCommand();

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getCheckMaintainedConnectionsFrequencySec()).isEqualTo(60);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void initiateConnectionsFrequencyFlag_isSet() {
    final TestIdnCommand cmd = parseCommand("--Xp2p-initiate-connections-frequency", "2");

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getInitiateConnectionsFrequencySec()).isEqualTo(2);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void initiateConnectionsFrequencyFlag_isNotSet() {
    final TestIdnCommand cmd = parseCommand();

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getInitiateConnectionsFrequencySec()).isEqualTo(30);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkDnsServerOverrideFlag_isSet() {
    final TestIdnCommand cmd = parseCommand("--Xp2p-dns-discovery-server", "localhost");

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDnsDiscoveryServerOverride()).isPresent();
    assertThat(networkingConfig.getDnsDiscoveryServerOverride().get()).isEqualTo("localhost");

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkDnsServerOverrideFlag_isNotSet() {
    final TestIdnCommand cmd = parseCommand();

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDnsDiscoveryServerOverride()).isEmpty();

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkDiscoveryV5Enabled_isSet() {
    final TestIdnCommand cmd = parseCommand("--Xv5-discovery-enabled");

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDiscovery().isDiscoveryV5Enabled()).isTrue();

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkDiscoveryV5Enabled_isNotSet() {
    final TestIdnCommand cmd = parseCommand();

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDiscovery().isDiscoveryV5Enabled()).isFalse();

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkFilterByForkIdNotSet() {
    final TestIdnCommand cmd = parseCommand();

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDiscovery().isFilterOnEnrForkIdEnabled()).isEqualTo(true);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkFilterByForkIdSet() {
    final TestIdnCommand cmd = parseCommand(NetworkingOptions.FILTER_ON_ENR_FORK_ID + "=true");

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDiscovery().isFilterOnEnrForkIdEnabled()).isEqualTo(true);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Test
  public void checkFilterByForkIdSetToFalse() {
    final TestIdnCommand cmd = parseCommand(NetworkingOptions.FILTER_ON_ENR_FORK_ID + "=false");

    final NetworkingOptions options = cmd.getNetworkingOptions();
    final NetworkingConfiguration networkingConfig = options.toDomainObject();
    assertThat(networkingConfig.getDiscovery().isFilterOnEnrForkIdEnabled()).isEqualTo(false);

    assertThat(commandErrorOutput.toString(UTF_8)).isEmpty();
    assertThat(commandOutput.toString(UTF_8)).isEmpty();
  }

  @Override
  protected NetworkingConfiguration createDefaultDomainObject() {
    return NetworkingConfiguration.create();
  }

  @Override
  protected NetworkingConfiguration createCustomizedDomainObject() {
    final NetworkingConfiguration config = NetworkingConfiguration.create();
    config.setInitiateConnectionsFrequency(
        NetworkingConfiguration.DEFAULT_INITIATE_CONNECTIONS_FREQUENCY_SEC + 10);
    config.setCheckMaintainedConnectionsFrequency(
        NetworkingConfiguration.DEFAULT_CHECK_MAINTAINED_CONNECTIONS_FREQUENCY_SEC + 10);
    return config;
  }

  @Override
  protected NetworkingOptions optionsFromDomainObject(final NetworkingConfiguration domainObject) {
    return NetworkingOptions.fromConfig(domainObject);
  }

  @Override
  protected NetworkingOptions getOptionsFromIdnCommand(final TestIdnCommand idnCommand) {
    return idnCommand.getNetworkingOptions();
  }

  @Override
  protected List<String> getFieldsToIgnore() {
    return Arrays.asList("rlpx.peerLowerBound");
  }
}
