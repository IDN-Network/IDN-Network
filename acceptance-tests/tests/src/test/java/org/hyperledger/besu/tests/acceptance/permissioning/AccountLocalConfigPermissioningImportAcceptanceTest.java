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
package org.idnecology.idn.tests.acceptance.permissioning;

import org.idnecology.idn.ethereum.permissioning.AllowlistPersistor;
import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.account.Account;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.Cluster;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfigurationBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AccountLocalConfigPermissioningImportAcceptanceTest extends AcceptanceTestBase {

  @TempDir Path folder;

  private static final String GENESIS_FILE = "/ibft/ibft.json";

  private Account sender;
  private Account beneficiary;
  private IdnNode bootnode;
  private IdnNode nodeA;
  private IdnNode nodeB;
  private Cluster permissionedCluster;

  @BeforeEach
  public void setUp() throws IOException {
    sender = accounts.getPrimaryBenefactor();
    beneficiary = accounts.createAccount("beneficiary");
    final List<String> allowList = List.of(sender.getAddress(), beneficiary.getAddress());
    final Path sharedFile = Files.createFile(folder.resolve("sharedFile"));
    persistAllowList(allowList, sharedFile);
    bootnode = idn.createIbft2NonValidatorBootnode("bootnode", GENESIS_FILE);
    nodeA =
        idn.createIbft2NodeWithLocalAccountPermissioning(
            "nodeA", GENESIS_FILE, allowList, sharedFile.toFile());
    nodeB =
        idn.createIbft2NodeWithLocalAccountPermissioning(
            "nodeB", GENESIS_FILE, allowList, sharedFile.toFile());
    permissionedCluster =
        new Cluster(new ClusterConfigurationBuilder().awaitPeerDiscovery(false).build(), net);

    permissionedCluster.start(bootnode, nodeA, nodeB);
  }

  @Test
  public void transactionFromDeniedAccountShouldNotBreakBlockImport() throws IOException {
    final Path newPermissionsFile = Files.createFile(folder.resolve("newPermissionsFile"));
    final List<String> allowList = List.of(beneficiary.getAddress());
    persistAllowList(allowList, newPermissionsFile);
    final IdnNode nodeC =
        idn.createIbft2NodeWithLocalAccountPermissioning(
            "nodeC", GENESIS_FILE, allowList, newPermissionsFile.toFile());

    waitForBlockHeight(bootnode, 2);

    nodeA.verify(beneficiary.balanceEquals(0));
    nodeA.execute(accountTransactions.createTransfer(sender, beneficiary, 1));
    nodeA.verify(beneficiary.balanceEquals(1));

    permissionedCluster.startNode(nodeC);

    waitForBlockHeight(bootnode, 4);
    waitForBlockHeight(nodeC, 4);
  }

  private void persistAllowList(final List<String> allowList, final Path path) throws IOException {
    AllowlistPersistor.addNewConfigItem(
        AllowlistPersistor.ALLOWLIST_TYPE.ACCOUNTS, allowList, path);
  }

  @AfterEach
  public void tearDown() {
    permissionedCluster.stop();
  }
}
