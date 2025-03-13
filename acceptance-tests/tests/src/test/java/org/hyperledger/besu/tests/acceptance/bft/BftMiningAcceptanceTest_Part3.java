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
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BftMiningAcceptanceTest_Part3 extends ParameterizedBftTestBase {

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("factoryFunctions")
  public void shouldMineOnMultipleNodesEvenWhenClusterContainsNonValidator(
      final String testName, final BftAcceptanceTestParameterization nodeFactory) throws Exception {
    setUp(testName, nodeFactory);
    final String[] validators = {"validator1", "validator2", "validator3"};
    final IdnNode validator1 =
        nodeFactory.createNodeWithValidators(idn, "validator1", validators);
    final IdnNode validator2 =
        nodeFactory.createNodeWithValidators(idn, "validator2", validators);
    final IdnNode validator3 =
        nodeFactory.createNodeWithValidators(idn, "validator3", validators);
    final IdnNode nonValidatorNode =
        nodeFactory.createNodeWithValidators(idn, "non-validator", validators);
    cluster.start(validator1, validator2, validator3, nonValidatorNode);

    cluster.verify(blockchain.reachesHeight(validator1, 1, 85));

    final Account sender = accounts.createAccount("account1");
    final Account receiver = accounts.createAccount("account2");

    validator1.execute(accountTransactions.createTransfer(sender, 50));
    cluster.verify(sender.balanceEquals(50));

    validator2.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 1));
    cluster.verify(receiver.balanceEquals(1));

    nonValidatorNode.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 2));
    cluster.verify(receiver.balanceEquals(3));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("factoryFunctions")
  public void shouldStillMineWhenANonProposerNodeFailsAndHasSufficientValidators(
      final String testName, final BftAcceptanceTestParameterization nodeFactory) throws Exception {
    setUp(testName, nodeFactory);
    final IdnNode minerNode1 = nodeFactory.createNode(idn, "miner1");
    final IdnNode minerNode2 = nodeFactory.createNode(idn, "miner2");
    final IdnNode minerNode3 = nodeFactory.createNode(idn, "miner3");
    final IdnNode minerNode4 = nodeFactory.createNode(idn, "miner4");
    final List<IdnNode> validators =
        bft.validators(new IdnNode[] {minerNode1, minerNode2, minerNode3, minerNode4});
    final IdnNode nonProposerNode = validators.get(validators.size() - 1);
    cluster.start(validators);

    cluster.verify(blockchain.reachesHeight(minerNode1, 1, 85));

    final Account receiver = accounts.createAccount("account2");

    cluster.stopNode(nonProposerNode);
    validators.get(0).execute(accountTransactions.createTransfer(receiver, 80));

    cluster.verifyOnActiveNodes(receiver.balanceEquals(80));
  }
}
