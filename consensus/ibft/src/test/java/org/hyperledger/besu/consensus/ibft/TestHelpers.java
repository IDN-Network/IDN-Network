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
package org.idnecology.idn.consensus.ibft;

import static java.util.Collections.singletonList;

import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.ProposedBlockHelpers;
import org.idnecology.idn.consensus.ibft.messagewrappers.Proposal;
import org.idnecology.idn.consensus.ibft.messagewrappers.RoundChange;
import org.idnecology.idn.consensus.ibft.payload.MessageFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.Block;

import java.util.Optional;

public class TestHelpers {

  public static Proposal createSignedProposalPayload(final NodeKey signerNodeKey) {
    return createSignedProposalPayloadWithRound(signerNodeKey, 0xFEDCBA98);
  }

  public static Proposal createSignedProposalPayloadWithRound(
      final NodeKey signerNodeKey, final int round) {
    final MessageFactory messageFactory = new MessageFactory(signerNodeKey);
    final ConsensusRoundIdentifier roundIdentifier =
        new ConsensusRoundIdentifier(0x1234567890ABCDEFL, round);
    final Block block =
        ProposedBlockHelpers.createProposalBlock(
            singletonList(AddressHelpers.ofValue(1)), roundIdentifier);
    return messageFactory.createProposal(roundIdentifier, block, Optional.empty());
  }

  public static RoundChange createSignedRoundChangePayload(final NodeKey signerKeys) {
    final MessageFactory messageFactory = new MessageFactory(signerKeys);
    final ConsensusRoundIdentifier roundIdentifier =
        new ConsensusRoundIdentifier(0x1234567890ABCDEFL, 0xFEDCBA98);
    return messageFactory.createRoundChange(roundIdentifier, Optional.empty());
  }
}
