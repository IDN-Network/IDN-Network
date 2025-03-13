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
package org.idnecology.idn.tests.acceptance.bft;

import org.idnecology.idn.tests.acceptance.dsl.account.Account;
import org.idnecology.idn.tests.acceptance.dsl.blockchain.Amount;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BftMiningAcceptanceTest_Part2 extends ParameterizedBftTestBase {

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("factoryFunctions")
  public void shouldMineOnSingleNodeWithFreeGas_London(
      final String testName, final BftAcceptanceTestParameterization nodeFactory) throws Exception {
    setUp(testName, nodeFactory);
    final IdnNode minerNode = nodeFactory.createNode(idn, "miner1");
    updateGenesisConfigToLondon(minerNode, true);

    cluster.start(minerNode);

    cluster.verify(blockchain.reachesHeight(minerNode, 1));

    final Account sender = accounts.createAccount("account1");
    final Account receiver = accounts.createAccount("account2");

    minerNode.execute(accountTransactions.createTransfer(sender, 50, Amount.ZERO));
    cluster.verify(sender.balanceEquals(50));

    minerNode.execute(accountTransactions.create1559Transfer(sender, 50, 4, Amount.ZERO));
    cluster.verify(sender.balanceEquals(100));

    minerNode.execute(
        accountTransactions.createIncrementalTransfers(sender, receiver, 1, Amount.ZERO));
    cluster.verify(receiver.balanceEquals(1));

    minerNode.execute(
        accountTransactions.create1559IncrementalTransfers(sender, receiver, 2, 4, Amount.ZERO));
    cluster.verify(receiver.balanceEquals(3));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("factoryFunctions")
  public void shouldMineOnSingleNodeWithFreeGas_Shanghai(
      final String testName, final BftAcceptanceTestParameterization nodeFactory) throws Exception {
    setUp(testName, nodeFactory);
    final IdnNode minerNode = nodeFactory.createNode(idn, "miner1");
    updateGenesisConfigToShanghai(minerNode, true);

    cluster.start(minerNode);

    cluster.verify(blockchain.reachesHeight(minerNode, 1));

    final Account sender = accounts.createAccount("account1");
    final Account receiver = accounts.createAccount("account2");

    minerNode.execute(accountTransactions.createTransfer(sender, 50, Amount.ZERO));
    cluster.verify(sender.balanceEquals(50));

    minerNode.execute(accountTransactions.create1559Transfer(sender, 50, 4, Amount.ZERO));
    cluster.verify(sender.balanceEquals(100));

    minerNode.execute(
        accountTransactions.createIncrementalTransfers(sender, receiver, 1, Amount.ZERO));
    cluster.verify(receiver.balanceEquals(1));

    minerNode.execute(
        accountTransactions.create1559IncrementalTransfers(sender, receiver, 2, 4, Amount.ZERO));
    cluster.verify(receiver.balanceEquals(3));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("factoryFunctions")
  public void shouldMineOnMultipleNodes(
      final String testName, final BftAcceptanceTestParameterization nodeFactory) throws Exception {
    setUp(testName, nodeFactory);
    final IdnNode minerNode1 = nodeFactory.createNode(idn, "miner1");
    final IdnNode minerNode2 = nodeFactory.createNode(idn, "miner2");
    final IdnNode minerNode3 = nodeFactory.createNode(idn, "miner3");
    final IdnNode minerNode4 = nodeFactory.createNode(idn, "miner4");
    cluster.start(minerNode1, minerNode2, minerNode3, minerNode4);

    cluster.verify(blockchain.reachesHeight(minerNode1, 1, 85));

    final Account sender = accounts.createAccount("account1");
    final Account receiver = accounts.createAccount("account2");

    minerNode1.execute(accountTransactions.createTransfer(sender, 50));
    cluster.verify(sender.balanceEquals(50));

    minerNode2.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 1));
    cluster.verify(receiver.balanceEquals(1));

    minerNode3.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 2));
    cluster.verify(receiver.balanceEquals(3));

    minerNode4.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 3));
    cluster.verify(receiver.balanceEquals(6));
  }
}
