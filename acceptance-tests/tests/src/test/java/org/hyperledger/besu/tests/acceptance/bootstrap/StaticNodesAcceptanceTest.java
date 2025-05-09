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
package org.idnecology.idn.tests.acceptance.bootstrap;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.WaitUtils;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StaticNodesAcceptanceTest extends AcceptanceTestBase {

  private Node otherNode;
  private Node node;

  @BeforeEach
  public void setUp() throws Exception {
    otherNode = idn.createNodeWithNoDiscovery("other-node");
    cluster.start(otherNode);
  }

  @Test
  public void shouldConnectToNodeAddedAsStaticNode() throws Exception {
    node = idn.createNodeWithStaticNodes("node", Arrays.asList(otherNode));
    cluster.addNode(node);

    node.verify(net.awaitPeerCount(1));

    WaitUtils.waitFor(1000000, () -> {});
  }
}
