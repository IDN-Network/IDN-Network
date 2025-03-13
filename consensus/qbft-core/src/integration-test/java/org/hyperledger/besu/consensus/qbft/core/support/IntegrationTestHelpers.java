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
package org.idnecology.idn.consensus.qbft.core.support;

import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.adaptor.QbftBlockInterfaceAdaptor;
import org.idnecology.idn.consensus.qbft.core.payload.CommitPayload;
import org.idnecology.idn.consensus.qbft.core.payload.MessageFactory;
import org.idnecology.idn.consensus.qbft.core.statemachine.PreparedCertificate;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockInterface;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.cryptoservices.NodeKey;

public class IntegrationTestHelpers {

  public static SignedData<CommitPayload> createSignedCommitPayload(
      final ConsensusRoundIdentifier roundId,
      final QbftBlock block,
      final NodeKey nodeKey,
      final QbftBlockCodec blockEncoder) {

    final QbftBlock commitBlock =
        createCommitBlockFromProposalBlock(block, roundId.getRoundNumber());
    final SECPSignature commitSeal = nodeKey.sign(commitBlock.getHash());

    final MessageFactory messageFactory = new MessageFactory(nodeKey, blockEncoder);

    return messageFactory.createCommit(roundId, block.getHash(), commitSeal).getSignedPayload();
  }

  public static PreparedCertificate createValidPreparedCertificate(
      final TestContext context,
      final ConsensusRoundIdentifier preparedRound,
      final QbftBlock block) {
    final RoundSpecificPeers peers = context.roundSpecificPeers(preparedRound);

    return new PreparedCertificate(
        block,
        peers.createSignedPreparePayloadOfAllPeers(preparedRound, block.getHash()),
        preparedRound.getRoundNumber());
  }

  public static QbftBlock createCommitBlockFromProposalBlock(
      final QbftBlock proposalBlock, final int round) {
    final QbftExtraDataCodec bftExtraDataCodec = new QbftExtraDataCodec();
    final BftBlockInterface bftBlockInterface = new BftBlockInterface(bftExtraDataCodec);
    final QbftBlockInterface qbftBlockInterface = new QbftBlockInterfaceAdaptor(bftBlockInterface);
    return qbftBlockInterface.replaceRoundInBlock(proposalBlock, round);
  }
}
