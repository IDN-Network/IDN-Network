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
package org.idnecology.idn.consensus.ibft.support;

import org.idnecology.idn.consensus.common.bft.BftBlockHashing;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.consensus.ibft.messagewrappers.Prepare;
import org.idnecology.idn.consensus.ibft.payload.CommitPayload;
import org.idnecology.idn.consensus.ibft.payload.MessageFactory;
import org.idnecology.idn.consensus.ibft.statemachine.PreparedRoundArtifacts;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.core.Block;

import java.util.Optional;
import java.util.stream.Collectors;

public class IntegrationTestHelpers {

  public static SignedData<CommitPayload> createSignedCommitPayload(
      final ConsensusRoundIdentifier roundId, final Block block, final NodeKey nodeKey) {

    final IbftExtraDataCodec ibftExtraDataEncoder = new IbftExtraDataCodec();
    final BftExtraData extraData = ibftExtraDataEncoder.decode(block.getHeader());

    final SECPSignature commitSeal =
        nodeKey.sign(
            new BftBlockHashing(ibftExtraDataEncoder)
                .calculateDataHashForCommittedSeal(block.getHeader(), extraData));

    final MessageFactory messageFactory = new MessageFactory(nodeKey);

    return messageFactory.createCommit(roundId, block.getHash(), commitSeal).getSignedPayload();
  }

  public static PreparedRoundArtifacts createValidPreparedRoundArtifacts(
      final TestContext context, final ConsensusRoundIdentifier preparedRound, final Block block) {
    final RoundSpecificPeers peers = context.roundSpecificPeers(preparedRound);

    return new PreparedRoundArtifacts(
        peers
            .getProposer()
            .getMessageFactory()
            .createProposal(preparedRound, block, Optional.empty()),
        peers.createSignedPreparePayloadOfNonProposing(preparedRound, block.getHash()).stream()
            .map(Prepare::new)
            .collect(Collectors.toList()));
  }
}
