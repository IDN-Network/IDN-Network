/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.tests.acceptance.bft.qbft;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.account.Account;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import org.junit.jupiter.api.Test;

public class QbftContractAcceptanceTest extends AcceptanceTestBase {

  @Test
  public void shouldMineOnMultipleNodesEvenWhenClusterContainsNonValidator() throws Exception {
    final String[] validators = {"validator1", "validator2", "validator3"};
    final IdnNode validator1 =
        idn.createQbftNodeWithContractBasedValidators("validator1", validators);
    final IdnNode validator2 =
        idn.createQbftNodeWithContractBasedValidators("validator2", validators);
    final IdnNode validator3 =
        idn.createQbftNodeWithContractBasedValidators("validator3", validators);
    final IdnNode nonValidatorNode =
        idn.createQbftNodeWithContractBasedValidators("non-validator", validators);
    cluster.start(validator1, validator2, validator3, nonValidatorNode);

    cluster.verify(blockchain.reachesHeight(validator1, 1, 85));

    cluster.verify(bft.validatorsEqual(validator1, validator2, validator3));

    final Account sender = accounts.createAccount("account1");
    final Account receiver = accounts.createAccount("account2");

    validator1.execute(accountTransactions.createTransfer(sender, 50));
    cluster.verify(sender.balanceEquals(50));

    validator2.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 1));
    cluster.verify(receiver.balanceEquals(1));

    nonValidatorNode.execute(accountTransactions.createIncrementalTransfers(sender, receiver, 2));
    cluster.verify(receiver.balanceEquals(3));
  }
}
