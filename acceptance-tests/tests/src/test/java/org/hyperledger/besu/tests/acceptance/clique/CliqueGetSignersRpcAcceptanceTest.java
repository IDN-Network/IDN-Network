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
package org.idnecology.idn.tests.acceptance.clique;

import static org.idnecology.idn.tests.acceptance.dsl.transaction.clique.CliqueTransactions.LATEST;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("flaky test due to hardcoded block numbers")
public class CliqueGetSignersRpcAcceptanceTest extends AcceptanceTestBase {
  private IdnNode minerNode1;
  private IdnNode minerNode2;

  @BeforeEach
  public void setUp() throws Exception {
    final String[] validators = {"miner1"};
    minerNode1 = idn.createCliqueNodeWithValidators("miner1", validators);
    minerNode2 = idn.createCliqueNodeWithValidators("miner2", validators);
    cluster.start(minerNode1, minerNode2);
  }

  @Test
  public void shouldBeAbleToGetValidatorsForBlockNumber() {
    cluster.verify(clique.validatorsAtBlockEqual("0x0", minerNode1));
    minerNode1.verify(blockchain.minimumHeight(1));

    minerNode1.execute(cliqueTransactions.createAddProposal(minerNode2));
    cluster.verify(blockchain.reachesHeight(minerNode1, 1));
    cluster.verify(clique.validatorsAtBlockEqual("0x2", minerNode1, minerNode2));
    cluster.verify(clique.validatorsAtBlockEqual(LATEST, minerNode1, minerNode2));
  }

  @Test
  public void shouldBeAbleToGetValidatorsForBlockHash() {
    cluster.verify(clique.validatorsAtBlockHashFromBlockNumberEqual(minerNode1, 0, minerNode1));
    minerNode1.verify(blockchain.minimumHeight(1));

    minerNode1.execute(cliqueTransactions.createAddProposal(minerNode2));
    cluster.verify(blockchain.reachesHeight(minerNode1, 1));
    cluster.verify(
        clique.validatorsAtBlockHashFromBlockNumberEqual(minerNode1, 2, minerNode1, minerNode2));
  }
}
