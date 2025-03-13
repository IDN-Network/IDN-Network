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
package org.idnecology.idn.tests.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.WaitUtils;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class RunHelpTest extends AcceptanceTestBase {

  @Test
  public void testShowsHelpAndExits() throws IOException {
    final IdnNode node = idn.runCommand("--help");
    cluster.startConsoleCapture();
    cluster.runNodeStart(node);
    WaitUtils.waitFor(5000, () -> node.verify(exitedSuccessfully));

    // assert that no random startup or ending logging appears.
    // if the help text changes then updates are appropriate.
    final String consoleContents = cluster.getConsoleContents();
    assertThat(consoleContents)
        .startsWith("Usage:\n\nidn [OPTIONS] [COMMAND]\n\nDescription:\n\n");
  }
}
