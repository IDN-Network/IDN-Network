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
package org.idnecology.idn.tests.acceptance.dsl.transaction.login;

import static org.assertj.core.api.Fail.fail;

import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;

public class LoginTransaction implements Transaction<String> {
  private final String username;
  private final String password;

  public LoginTransaction(final String username, final String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public String execute(final NodeRequests node) {
    try {
      return node.login().send(username, password);
    } catch (IOException e) {
      fail("Login request failed with exception: %s", e);
      return null;
    }
  }
}
