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
import org.idnecology.idn.tests.acceptance.dsl.node.Node;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Web3Sha3AcceptanceTest extends AcceptanceTestBase {

  private Node node;

  @BeforeEach
  public void setUp() throws Exception {
    node = idn.createArchiveNode("node1");
    cluster.start(node);
  }

  @Test
  public void shouldReturnCorrectSha3() {
    final String input = "0x68656c6c6f20776f726c64";
    final String expectedHash =
        "0x47173285a8d7341e5e972fc677286384f802f8ef42a5ec5f03bbfa254cb01fad";

    node.verify(web3.sha3(input, expectedHash));
  }
}
