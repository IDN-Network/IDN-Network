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

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.web3j.generated.SimpleStorage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeploySmartContractAcceptanceTest extends AcceptanceTestBase {

  private IdnNode minerNode;

  @BeforeEach
  public void setUp() throws Exception {
    minerNode = idn.createMinerNode("miner-node");
    cluster.start(minerNode);
  }

  @Test
  public void deployingMustGiveValidReceipt() {
    // Contract address is generated from sender address and transaction nonce
    final String contractAddress = "0x42699a7612a82f1d9c36148af9c77354759b210b";

    final SimpleStorage simpleStorageContract =
        minerNode.execute(contractTransactions.createSmartContract(SimpleStorage.class));

    contractVerifier.validTransactionReceipt(contractAddress).verify(simpleStorageContract);
  }
}
