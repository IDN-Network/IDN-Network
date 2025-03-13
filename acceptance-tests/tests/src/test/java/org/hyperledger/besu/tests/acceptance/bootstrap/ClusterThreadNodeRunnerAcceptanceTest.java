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

import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.idnecology.idn.plugin.services.storage.KeyValueStorageFactory;
import org.idnecology.idn.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.idnecology.idn.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.idnecology.idn.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.account.Account;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNodeRunner;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;
import org.idnecology.idn.tests.acceptance.dsl.node.ThreadIdnNodeRunner;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.Cluster;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfiguration;
import org.idnecology.idn.tests.acceptance.dsl.node.cluster.ClusterConfigurationBuilder;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClusterThreadNodeRunnerAcceptanceTest extends AcceptanceTestBase {

  private Node miner;
  private Cluster noDiscoveryCluster;

  @BeforeEach
  public void setUp() throws Exception {
    final ClusterConfiguration clusterConfiguration =
        new ClusterConfigurationBuilder().awaitPeerDiscovery(false).build();
    final IdnNodeRunner idnNodeRunner = new ThreadIdnNodeRunner();
    noDiscoveryCluster = new Cluster(clusterConfiguration, net, idnNodeRunner);
    final IdnNode noDiscoveryNode = idn.createNodeWithNoDiscovery("noDiscovery");
    miner =
        idn.createMinerNode(
            "miner",
            (builder) -> {
              KeyValueStorageFactory persistentStorageFactory =
                  new RocksDBKeyValueStorageFactory(
                      RocksDBCLIOptions.create()::toDomainObject,
                      List.of(KeyValueSegmentIdentifier.values()),
                      RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS);
              builder.storageImplementation(persistentStorageFactory);
              return builder;
            });
    noDiscoveryCluster.start(noDiscoveryNode, miner);
  }

  @Test
  public void shouldVerifySomething() {
    // we don't care what verifies, just that it gets to the point something can verify
    miner.verify(net.awaitPeerCount(0));
  }

  @Test
  void shouldMineTransactionsEvenAfterRestart() {
    final Account recipient = accounts.createAccount("account1");
    miner.execute(accountTransactions.createTransfer(recipient, 2));
    miner.verify(recipient.balanceEquals(2));

    noDiscoveryCluster.stop();
    noDiscoveryCluster.start(miner);
    // Checking that state is retained after restart
    miner.verify(recipient.balanceEquals(2));
  }

  @AfterEach
  @Override
  public void tearDownAcceptanceTestBase() {
    noDiscoveryCluster.stop();
    super.tearDownAcceptanceTestBase();
  }
}
