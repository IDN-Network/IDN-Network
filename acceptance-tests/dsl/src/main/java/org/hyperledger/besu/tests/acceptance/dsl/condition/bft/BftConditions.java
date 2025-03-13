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
package org.idnecology.idn.tests.acceptance.dsl.condition.bft;

import static org.idnecology.idn.tests.acceptance.dsl.transaction.clique.CliqueTransactions.LATEST;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.tests.acceptance.dsl.condition.Condition;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.acceptance.dsl.node.Node;
import org.idnecology.idn.tests.acceptance.dsl.transaction.bft.BftTransactions;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

public class BftConditions {

  private final BftTransactions bft;

  public BftConditions(final BftTransactions bft) {
    this.bft = bft;
  }

  public List<IdnNode> validators(final IdnNode[] nodes) {
    final Comparator<IdnNode> compareByAddress = Comparator.comparing(IdnNode::getAddress);
    List<IdnNode> idnNodes = Arrays.asList(nodes);
    idnNodes.sort(compareByAddress);
    return idnNodes;
  }

  public ExpectValidators validatorsEqual(final IdnNode... validators) {
    return new ExpectValidators(bft, validatorAddresses(validators));
  }

  private Address[] validatorAddresses(final IdnNode[] validators) {
    return Arrays.stream(validators).map(IdnNode::getAddress).sorted().toArray(Address[]::new);
  }

  public Condition awaitValidatorSetChange(final Node node) {
    return new AwaitValidatorSetChange(node.execute(bft.createGetValidators(LATEST)), bft);
  }

  public Condition noProposals() {
    return new ExpectProposals(bft, ImmutableMap.of());
  }

  public PendingVotesConfig pendingVotesEqual() {
    return new PendingVotesConfig(bft);
  }

  public static class PendingVotesConfig {
    private final Map<IdnNode, Boolean> proposals = new HashMap<>();
    private final BftTransactions bft;

    private PendingVotesConfig(final BftTransactions bft) {
      this.bft = bft;
    }

    public PendingVotesConfig addProposal(final IdnNode node) {
      proposals.put(node, true);
      return this;
    }

    public PendingVotesConfig removeProposal(final IdnNode node) {
      proposals.put(node, false);
      return this;
    }

    public Condition build() {
      final Map<Address, Boolean> proposalsAsAddress =
          this.proposals.entrySet().stream()
              .collect(Collectors.toMap(p -> p.getKey().getAddress(), Map.Entry::getValue));
      return new ExpectProposals(bft, proposalsAsAddress);
    }
  }
}
