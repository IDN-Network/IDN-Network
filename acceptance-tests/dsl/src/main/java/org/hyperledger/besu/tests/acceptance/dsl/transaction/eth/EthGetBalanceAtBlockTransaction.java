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
package org.idnecology.idn.tests.acceptance.dsl.transaction.eth;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.tests.acceptance.dsl.account.Account;
import org.idnecology.idn.tests.acceptance.dsl.transaction.NodeRequests;
import org.idnecology.idn.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetBalance;

public class EthGetBalanceAtBlockTransaction implements Transaction<BigInteger> {

  private final Account account;
  private final BigInteger block;

  EthGetBalanceAtBlockTransaction(final Account account, final BigInteger block) {
    this.account = account;
    this.block = block;
  }

  @Override
  public BigInteger execute(final NodeRequests node) {
    try {
      final EthGetBalance result =
          node.eth()
              .ethGetBalance(account.getAddress(), DefaultBlockParameter.valueOf(block))
              .send();
      assertThat(result).isNotNull();
      assertThat(result.hasError()).isFalse();

      return result.getBalance();

    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
