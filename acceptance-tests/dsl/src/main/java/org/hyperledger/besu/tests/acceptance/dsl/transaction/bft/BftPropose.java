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
package org.idnecology.idn.tests.acceptance.dsl.transaction.bft;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;

public class BftPropose implements Transaction<Boolean> {
  private final String address;
  private final boolean auth;

  public BftPropose(final String address, final boolean auth) {
    this.address = address;
    this.auth = auth;
  }

  @Override
  public Boolean execute(final NodeRequests node) {
    try {
      final BftRequestFactory.ProposeResponse result = node.bft().propose(address, auth).send();
      assertThat(result).isNotNull();
      assertThat(result.hasError()).isFalse();
      return result.getResult();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
