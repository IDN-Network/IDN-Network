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
package org.idnecology.idn.tests.acceptance.jsonrpc;

import org.idnecology.idn.tests.acceptance.dsl.node.cluster.Cluster;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfiguration;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfigurationBuilder;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;

public class JsonRpcWebsocketAuthenticationAcceptanceTest
    extends AbstractJsonRpcAuthenticationAcceptanceTest {

  @BeforeEach
  public void setUp() throws IOException, URISyntaxException {
    final ClusterConfiguration clusterConfiguration =
        new ClusterConfigurationBuilder().awaitPeerDiscovery(false).build();
    authenticatedCluster = new Cluster(clusterConfiguration, net);

    nodeUsingAuthFile = idn.createNodeWithAuthentication("node1", AUTH_FILE);
    nodeUsingRsaJwtPublicKey = idn.createNodeWithAuthenticationUsingRsaJwtPublicKey("node2");
    nodeUsingEcdsaJwtPublicKey = idn.createNodeWithAuthenticationUsingEcdsaJwtPublicKey("node3");
    nodeUsingAuthFileWithNoAuthApi =
        idn.createWsNodeWithAuthFileAndNoAuthApi("node4", AUTH_FILE, NO_AUTH_API_METHODS);
    authenticatedCluster.start(
        nodeUsingAuthFile,
        nodeUsingRsaJwtPublicKey,
        nodeUsingEcdsaJwtPublicKey,
        nodeUsingAuthFileWithNoAuthApi);

    nodeUsingAuthFile.useWebSocketsForJsonRpc();
    nodeUsingRsaJwtPublicKey.useWebSocketsForJsonRpc();
    nodeUsingEcdsaJwtPublicKey.useWebSocketsForJsonRpc();
    nodeUsingAuthFileWithNoAuthApi.useWebSocketsForJsonRpc();
    nodeUsingAuthFile.verify(login.awaitResponse("user", "badpassword"));
    nodeUsingRsaJwtPublicKey.verify(login.awaitResponse("user", "badpassword"));
    nodeUsingEcdsaJwtPublicKey.verify(login.awaitResponse("user", "badpassword"));
    nodeUsingAuthFileWithNoAuthApi.verify(login.awaitResponse("user", "badpassword"));
  }
}
