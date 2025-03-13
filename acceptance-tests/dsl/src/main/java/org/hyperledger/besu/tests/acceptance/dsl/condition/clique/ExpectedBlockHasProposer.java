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
package org.idnecology.idn.tests.acceptance.dsl.condition.clique;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.consensus.clique.CliqueBlockHeaderFunctions;
import org.idnecology.idn.consensus.clique.CliqueExtraData;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.tests.acceptance.dsl.BlockUtils;
import org.idnecology.idn.tests.acceptance.dsl.WaitUtils;
import org.idnecology.idn.tests.acceptance.dsl.condition.Condition;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;
import org.idnecology.idn.tests.acceptance.dsl.transaction.eth.EthTransactions;

import org.web3j.protocol.core.methods.response.EthBlock.Block;

public class ExpectedBlockHasProposer implements Condition {
  private final EthTransactions eth;
  private final Address proposer;

  public ExpectedBlockHasProposer(final EthTransactions eth, final Address proposer) {
    this.eth = eth;
    this.proposer = proposer;
  }

  @Override
  public void verify(final Node node) {
    WaitUtils.waitFor(() -> assertThat(proposerAddress(node)).isEqualTo(proposer));
  }

  private Address proposerAddress(final Node node) {
    final Block block = node.execute(eth.block());
    final BlockHeader blockHeader =
        BlockUtils.createBlockHeader(block, new CliqueBlockHeaderFunctions());
    final CliqueExtraData cliqueExtraData = CliqueExtraData.decode(blockHeader);
    return cliqueExtraData.getProposerAddress();
  }
}
