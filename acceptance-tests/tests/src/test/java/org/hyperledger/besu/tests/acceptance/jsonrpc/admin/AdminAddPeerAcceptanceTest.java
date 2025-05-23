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
package org.idnecology.idn.tests.acceptance.jsonrpc.admin;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.Cluster;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfiguration;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfigurationBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdminAddPeerAcceptanceTest extends AcceptanceTestBase {
  private Cluster noDiscoveryCluster;

  private Node nodeA;
  private Node nodeB;

  @BeforeEach
  public void setUp() throws Exception {
    final ClusterConfiguration clusterConfiguration =
        new ClusterConfigurationBuilder().awaitPeerDiscovery(false).build();
    noDiscoveryCluster = new Cluster(clusterConfiguration, net);
    nodeA = idn.createArchiveNodeWithDiscoveryDisabledAndAdmin("nodeA");
    nodeB = idn.createArchiveNodeWithDiscoveryDisabledAndAdmin("nodeB");
    noDiscoveryCluster.start(nodeA, nodeB);
  }

  @AfterEach
  public void tearDown() {
    noDiscoveryCluster.stop();
  }

  @Test
  public void adminAddPeerForcesConnection() {
    nodeA.verify(net.awaitPeerCount(0));
    nodeA.verify(admin.addPeer(nodeB));
    nodeA.verify(net.awaitPeerCount(1));
    nodeB.verify(net.awaitPeerCount(1));
  }
}
