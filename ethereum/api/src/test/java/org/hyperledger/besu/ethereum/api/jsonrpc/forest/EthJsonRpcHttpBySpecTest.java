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
package org.idnecology.idn.ethereum.api.jsonrpc.forest;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.ethereum.api.jsonrpc.AbstractJsonRpcHttpBySpecTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EthJsonRpcHttpBySpecTest extends AbstractJsonRpcHttpBySpecTest {

  @Override
  @BeforeEach
  public void setup() throws Exception {
    setupBlockchain();
    startService();
  }

  public static Object[][] specs() {
    return findSpecFiles(new String[] {"eth"});
  }

  @Test
  void dryRunDetector() {
    assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
  }
}
