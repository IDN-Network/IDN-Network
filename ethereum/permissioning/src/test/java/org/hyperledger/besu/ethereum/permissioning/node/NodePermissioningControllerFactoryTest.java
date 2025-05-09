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
package org.idnecology.idn.ethereum.permissioning.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.p2p.peers.EnodeURLImpl;
import org.idnecology.idn.ethereum.permissioning.LocalPermissioningConfiguration;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodePermissioningControllerFactory;
import org.idnecology.idn.ethereum.permissioning.NodeSmartContractPermissioningController;
import org.idnecology.idn.ethereum.permissioning.PermissioningConfiguration;
import org.idnecology.idn.ethereum.permissioning.SmartContractPermissioningConfiguration;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.data.EnodeURL;
import org.idnecology.idn.plugin.services.permissioning.NodeConnectionPermissioningProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class NodePermissioningControllerFactoryTest {

  @Mock private Synchronizer synchronizer;
  @Mock private TransactionSimulator transactionSimulator;
  @Mock private Blockchain blockchain;

  private final String enode =
      "enode://5f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@192.168.0.10:1111";
  Collection<EnodeURL> bootnodes = Collections.emptyList();
  EnodeURL selfEnode = EnodeURLImpl.fromString(enode);
  LocalPermissioningConfiguration localPermissioningConfig;
  SmartContractPermissioningConfiguration smartContractPermissioningConfiguration;
  PermissioningConfiguration config;

  @BeforeEach
  public void before() {
    when(transactionSimulator.doesAddressExistAtHead(any())).thenReturn(Optional.of(true));
  }

  @Test
  public void testCreateWithNeitherPermissioningEnabled() {
    config = new PermissioningConfiguration(Optional.empty(), Optional.empty());
    NodePermissioningControllerFactory factory = new NodePermissioningControllerFactory();
    NodePermissioningController controller =
        factory.create(
            config,
            synchronizer,
            bootnodes,
            selfEnode.getNodeId(),
            transactionSimulator,
            new NoOpMetricsSystem(),
            blockchain,
            Collections.emptyList());

    List<NodeConnectionPermissioningProvider> providers = controller.getProviders();
    assertThat(providers.size()).isEqualTo(0);
    assertThat(controller.getSyncStatusNodePermissioningProvider()).isNotPresent();
  }

  @Test
  public void testCreateWithSmartContractNodePermissioningEnabledOnly() {
    smartContractPermissioningConfiguration = new SmartContractPermissioningConfiguration();
    smartContractPermissioningConfiguration.setNodeSmartContractAddress(
        Address.fromHexString("0x0000000000000000000000000000000000001234"));
    smartContractPermissioningConfiguration.setSmartContractNodeAllowlistEnabled(true);
    config =
        new PermissioningConfiguration(
            Optional.empty(), Optional.of(smartContractPermissioningConfiguration));

    NodePermissioningControllerFactory factory = new NodePermissioningControllerFactory();
    NodePermissioningController controller =
        factory.create(
            config,
            synchronizer,
            bootnodes,
            selfEnode.getNodeId(),
            transactionSimulator,
            new NoOpMetricsSystem(),
            blockchain,
            Collections.emptyList());

    List<NodeConnectionPermissioningProvider> providers = controller.getProviders();
    assertThat(providers.size()).isEqualTo(1);

    NodeConnectionPermissioningProvider p1 = providers.get(0);
    assertThat(p1).isInstanceOf(NodeSmartContractPermissioningController.class);
    assertThat(controller.getSyncStatusNodePermissioningProvider()).isEmpty();
  }

  @Test
  public void testCreateWithLocalNodePermissioningEnabledOnly() {
    localPermissioningConfig = LocalPermissioningConfiguration.createDefault();
    localPermissioningConfig.setNodeAllowlist(Collections.emptyList());
    localPermissioningConfig.setNodePermissioningConfigFilePath("fake-file-path");
    config =
        new PermissioningConfiguration(Optional.of(localPermissioningConfig), Optional.empty());

    NodePermissioningControllerFactory factory = new NodePermissioningControllerFactory();
    NodePermissioningController controller =
        factory.create(
            config,
            synchronizer,
            bootnodes,
            selfEnode.getNodeId(),
            transactionSimulator,
            new NoOpMetricsSystem(),
            blockchain,
            Collections.emptyList());

    List<NodeConnectionPermissioningProvider> providers = controller.getProviders();
    assertThat(providers.size()).isEqualTo(1);

    NodeConnectionPermissioningProvider p1 = providers.get(0);
    assertThat(p1).isInstanceOf(NodeLocalConfigPermissioningController.class);
    assertThat(controller.getSyncStatusNodePermissioningProvider()).isNotPresent();
  }

  @Test
  public void
      testCreateWithLocalNodePermissioningEnabledAndSmartContractPresentButDisabledAndBootnode() {
    smartContractPermissioningConfiguration = new SmartContractPermissioningConfiguration();
    smartContractPermissioningConfiguration.setNodeSmartContractAddress(
        Address.fromHexString("0x0000000000000000000000000000000000001234"));
    smartContractPermissioningConfiguration.setSmartContractNodeAllowlistEnabled(false);
    final Collection<EnodeURL> fixedNodes = Collections.singleton(selfEnode);
    localPermissioningConfig = LocalPermissioningConfiguration.createDefault();
    localPermissioningConfig.setNodeAllowlist(Collections.emptyList());
    localPermissioningConfig.setNodePermissioningConfigFilePath("fake-file-path");
    config =
        new PermissioningConfiguration(
            Optional.of(localPermissioningConfig),
            Optional.of(smartContractPermissioningConfiguration));

    NodePermissioningControllerFactory factory = new NodePermissioningControllerFactory();
    NodePermissioningController controller =
        factory.create(
            config,
            synchronizer,
            fixedNodes,
            selfEnode.getNodeId(),
            transactionSimulator,
            new NoOpMetricsSystem(),
            blockchain,
            Collections.emptyList());

    List<NodeConnectionPermissioningProvider> providers = controller.getProviders();
    assertThat(providers.size()).isEqualTo(1);

    NodeConnectionPermissioningProvider p1 = providers.get(0);
    assertThat(p1).isInstanceOf(NodeLocalConfigPermissioningController.class);
    assertThat(controller.getSyncStatusNodePermissioningProvider()).isNotPresent();
  }

  @Test
  public void testCreateWithLocalNodeAndSmartContractPermissioningEnabled() {
    localPermissioningConfig = LocalPermissioningConfiguration.createDefault();
    localPermissioningConfig.setNodeAllowlist(Collections.emptyList());
    localPermissioningConfig.setNodePermissioningConfigFilePath("fake-file-path");

    smartContractPermissioningConfiguration = new SmartContractPermissioningConfiguration();
    smartContractPermissioningConfiguration.setNodeSmartContractAddress(
        Address.fromHexString("0x0000000000000000000000000000000000001234"));
    smartContractPermissioningConfiguration.setSmartContractNodeAllowlistEnabled(true);
    config =
        new PermissioningConfiguration(
            Optional.of(localPermissioningConfig),
            Optional.of(smartContractPermissioningConfiguration));

    NodePermissioningControllerFactory factory = new NodePermissioningControllerFactory();
    NodePermissioningController controller =
        factory.create(
            config,
            synchronizer,
            bootnodes,
            selfEnode.getNodeId(),
            transactionSimulator,
            new NoOpMetricsSystem(),
            blockchain,
            Collections.emptyList());

    List<NodeConnectionPermissioningProvider> providers = controller.getProviders();
    assertThat(providers.size()).isEqualTo(2);

    NodeConnectionPermissioningProvider p1 = providers.get(0);
    NodeConnectionPermissioningProvider p2 = providers.get(1);
    if (p1.getClass() == NodeLocalConfigPermissioningController.class) {
      assertThat(p2).isInstanceOf(NodeSmartContractPermissioningController.class);
    } else {
      assertThat(p2).isInstanceOf(NodeLocalConfigPermissioningController.class);
      assertThat(p1).isInstanceOf(NodeSmartContractPermissioningController.class);
    }
    assertThat(controller.getSyncStatusNodePermissioningProvider()).isEmpty();
  }

  @Test
  public void testCreateWithSmartContractNodePermissioningEnabledOnlyAndBootnode() {
    final Collection<EnodeURL> fixedNodes = Collections.singleton(selfEnode);

    smartContractPermissioningConfiguration = new SmartContractPermissioningConfiguration();
    smartContractPermissioningConfiguration.setNodeSmartContractAddress(
        Address.fromHexString("0x0000000000000000000000000000000000001234"));
    smartContractPermissioningConfiguration.setSmartContractNodeAllowlistEnabled(true);
    config =
        new PermissioningConfiguration(
            Optional.empty(), Optional.of(smartContractPermissioningConfiguration));

    NodePermissioningControllerFactory factory = new NodePermissioningControllerFactory();
    NodePermissioningController controller =
        factory.create(
            config,
            synchronizer,
            fixedNodes,
            selfEnode.getNodeId(),
            transactionSimulator,
            new NoOpMetricsSystem(),
            blockchain,
            Collections.emptyList());

    assertThat(controller.getSyncStatusNodePermissioningProvider()).isPresent();
  }

  @Test
  public void createOnchainShouldFailIfValidationFails() {
    smartContractPermissioningConfiguration = new SmartContractPermissioningConfiguration();
    smartContractPermissioningConfiguration.setNodeSmartContractAddress(
        Address.fromHexString("0x0000000000000000000000000000000000001234"));
    smartContractPermissioningConfiguration.setSmartContractNodeAllowlistEnabled(true);
    config =
        new PermissioningConfiguration(
            Optional.empty(), Optional.of(smartContractPermissioningConfiguration));

    when(transactionSimulator.processAtHead(any())).thenThrow(new RuntimeException());

    final Throwable thrown =
        catchThrowable(
            () ->
                new NodePermissioningControllerFactory()
                    .create(
                        config,
                        synchronizer,
                        bootnodes,
                        selfEnode.getNodeId(),
                        transactionSimulator,
                        new NoOpMetricsSystem(),
                        blockchain,
                        Collections.emptyList()));

    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Error: node permissioning contract at address 0x0000000000000000000000000000000000001234 does not match the expected interface version 1.");
  }
}
