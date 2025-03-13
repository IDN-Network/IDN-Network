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
package org.idnecology.idn.tests.acceptance.dsl.node.cluster;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.tests.acceptance.dsl.condition.Condition;
import org.idnecology.idn.tests.acceptance.dsl.condition.net.NetConditions;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNodeRunner;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;
import org.idnecology.idn.tests.acceptance.dsl.node.RunnableNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cluster implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

  private final Map<String, RunnableNode> nodes = new HashMap<>();
  private final IdnNodeRunner idnNodeRunner;
  private final NetConditions net;
  private final ClusterConfiguration clusterConfiguration;
  private List<? extends RunnableNode> originalNodes = emptyList();
  private final List<URI> bootnodes = new ArrayList<>();

  public Cluster(final NetConditions net) {
    this(new ClusterConfigurationBuilder().build(), net, IdnNodeRunner.instance());
  }

  public Cluster(final ClusterConfiguration clusterConfiguration, final NetConditions net) {
    this(clusterConfiguration, net, IdnNodeRunner.instance());
  }

  public Cluster(
      final ClusterConfiguration clusterConfiguration,
      final NetConditions net,
      final IdnNodeRunner idnNodeRunner) {
    this.clusterConfiguration = clusterConfiguration;
    this.net = net;
    this.idnNodeRunner = idnNodeRunner;
  }

  public void start(final Node... nodes) {
    start(
        Arrays.stream(nodes)
            .map(
                n -> {
                  assertThat(n instanceof RunnableNode).isTrue();
                  return (RunnableNode) n;
                })
            .collect(toList()));
  }

  public void start(final List<? extends RunnableNode> nodes) {
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Can't start a cluster with no nodes");
    }
    this.originalNodes = nodes;
    this.nodes.clear();
    this.bootnodes.clear();
    nodes.forEach(node -> this.nodes.put(node.getName(), node));

    final Optional<? extends RunnableNode> bootnode = selectAndStartBootnode(nodes);

    nodes.parallelStream()
        .filter(node -> bootnode.map(boot -> boot != node).orElse(true))
        .peek(node -> LOG.info("starting non-bootnode {}", node.getName()))
        .forEach(this::startNode);

    if (clusterConfiguration.isAwaitPeerDiscovery()) {
      for (final RunnableNode node : nodes) {
        LOG.info(
            "Awaiting peer discovery for node {}, expecting {} peers",
            node.getName(),
            nodes.size() - 1);
        node.awaitPeerDiscovery(net.awaitPeerCount(nodes.size() - 1));
      }
    }
    LOG.info("Cluster startup complete.");
  }

  private Optional<? extends RunnableNode> selectAndStartBootnode(
      final List<? extends RunnableNode> nodes) {
    final Optional<? extends RunnableNode> bootnode =
        nodes.stream()
            .filter(node -> node.getConfiguration().isBootnodeEligible())
            .filter(node -> node.getConfiguration().isP2pEnabled())
            .filter(node -> node.getConfiguration().isDiscoveryEnabled())
            .findFirst();

    bootnode.ifPresent(
        b -> {
          LOG.info("Selected node {} as bootnode", b.getName());
          startNode(b, true);
          bootnodes.add(b.enodeUrl());
        });

    return bootnode;
  }

  public void addNode(final Node node) {
    assertThat(node).isInstanceOf(RunnableNode.class);
    final RunnableNode runnableNode = (RunnableNode) node;

    nodes.put(runnableNode.getName(), runnableNode);
    startNode(runnableNode);

    if (clusterConfiguration.isAwaitPeerDiscovery()) {
      runnableNode.awaitPeerDiscovery(net.awaitPeerCount(nodes.size() - 1));
    }
  }

  public void startNode(final RunnableNode node) {
    this.startNode(node, false);
  }

  public void runNodeStart(final RunnableNode node) {
    LOG.info(
        "Starting node {} (id = {}...{})",
        node.getName(),
        node.getNodeId().substring(0, 4),
        node.getNodeId().substring(124));
    node.start(idnNodeRunner);
  }

  private void startNode(final RunnableNode node, final boolean isBootNode) {
    node.getConfiguration().setBootnodes(isBootNode ? emptyList() : bootnodes);

    if (node.getConfiguration().getGenesisConfig().isEmpty()) {
      node.getConfiguration()
          .getGenesisConfigProvider()
          .create(originalNodes)
          .ifPresent(node.getConfiguration()::setGenesisConfig);
    }
    runNodeStart(node);
  }

  public void stop() {
    // stops nodes but do not shutdown idnNodeRunner
    for (final RunnableNode node : nodes.values()) {
      stopNode(node);
    }
  }

  public void stopNode(final RunnableNode node) {
    if (node instanceof IdnNode) {
      idnNodeRunner.stopNode((IdnNode) node); // idnNodeRunner.stopNode also calls node.stop
    } else {
      node.stop();
    }
  }

  public void stopNode(final IdnNode node) {
    idnNodeRunner.stopNode(node);
  }

  @Override
  public void close() {
    stop();
    for (final RunnableNode node : nodes.values()) {
      node.close();
    }
    idnNodeRunner.shutdown();
  }

  public void verify(final Condition expected) {
    if (nodes.size() == 0) {
      throw new IllegalStateException("Attempt to verify an empty cluster");
    }
    for (final Node node : nodes.values()) {
      expected.verify(node);
    }
  }

  public void verifyOnActiveNodes(final Condition condition) {
    nodes.values().stream()
        .filter(node -> idnNodeRunner.isActive(node.getName()))
        .forEach(condition::verify);
  }

  /**
   * Starts a capture of System.out and System.err. Once getConsole is called the capture will end.
   */
  public void startConsoleCapture() {
    idnNodeRunner.startConsoleCapture();
  }

  /**
   * If no capture was started an empty string is returned. After the call the original System.err
   * and out are restored.
   *
   * @return The console output since startConsoleCapture() was called.
   */
  public String getConsoleContents() {
    return idnNodeRunner.getConsoleContents();
  }
}
