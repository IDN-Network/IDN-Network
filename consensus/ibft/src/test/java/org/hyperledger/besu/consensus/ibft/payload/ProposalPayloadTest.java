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
package org.idnecology.idn.consensus.ibft.payload;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.ProposedBlockHelpers;
import org.idnecology.idn.consensus.ibft.messagedata.IbftV2;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.rlp.RLPInput;

import org.junit.jupiter.api.Test;

public class ProposalPayloadTest {

  private static final ConsensusRoundIdentifier ROUND_IDENTIFIER =
      new ConsensusRoundIdentifier(0x1234567890ABCDEFL, 0xFEDCBA98);

  @Test
  public void roundTripRlp() {
    final Block block =
        ProposedBlockHelpers.createProposalBlock(
            singletonList(AddressHelpers.ofValue(1)), ROUND_IDENTIFIER);
    final ProposalPayload expectedProposalPayload =
        new ProposalPayload(ROUND_IDENTIFIER, block.getHash());
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    expectedProposalPayload.writeTo(rlpOut);

    final RLPInput rlpInput = RLP.input(rlpOut.encoded());
    final ProposalPayload proposalPayload = ProposalPayload.readFrom(rlpInput);
    assertThat(proposalPayload.getRoundIdentifier()).isEqualTo(ROUND_IDENTIFIER);
    assertThat(proposalPayload.getDigest()).isEqualTo(block.getHash());
    assertThat(proposalPayload.getMessageType()).isEqualTo(IbftV2.PROPOSAL);
  }
}
