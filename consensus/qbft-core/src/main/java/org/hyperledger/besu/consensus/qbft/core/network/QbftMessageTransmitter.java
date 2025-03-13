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
package org.idnecology.idn.consensus.qbft.core.network;

import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.network.ValidatorMulticaster;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.qbft.core.messagedata.CommitMessageData;
import org.idnecology.idn.consensus.qbft.core.messagedata.PrepareMessageData;
import org.idnecology.idn.consensus.qbft.core.messagedata.ProposalMessageData;
import org.idnecology.idn.consensus.qbft.core.messagedata.RoundChangeMessageData;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Commit;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Prepare;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Proposal;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.RoundChange;
import org.idnecology.idn.consensus.qbft.core.payload.MessageFactory;
import org.idnecology.idn.consensus.qbft.core.payload.PreparePayload;
import org.idnecology.idn.consensus.qbft.core.payload.RoundChangePayload;
import org.idnecology.idn.consensus.qbft.core.statemachine.PreparedCertificate;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.plugin.services.securitymodule.SecurityModuleException;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Qbft message transmitter. */
public class QbftMessageTransmitter {

  private static final Logger LOG = LoggerFactory.getLogger(QbftMessageTransmitter.class);

  private final MessageFactory messageFactory;
  private final ValidatorMulticaster multicaster;

  /**
   * Instantiates a new Qbft message transmitter.
   *
   * @param messageFactory the message factory
   * @param multicaster the multicaster
   */
  public QbftMessageTransmitter(
      final MessageFactory messageFactory, final ValidatorMulticaster multicaster) {
    this.messageFactory = messageFactory;
    this.multicaster = multicaster;
  }

  /**
   * Multicast proposal.
   *
   * @param roundIdentifier the round identifier
   * @param block the block
   * @param roundChanges the round changes
   * @param prepares the prepares
   */
  public void multicastProposal(
      final ConsensusRoundIdentifier roundIdentifier,
      final QbftBlock block,
      final List<SignedData<RoundChangePayload>> roundChanges,
      final List<SignedData<PreparePayload>> prepares) {
    try {
      final Proposal data =
          messageFactory.createProposal(roundIdentifier, block, roundChanges, prepares);

      final ProposalMessageData message = ProposalMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Proposal (not sent): {} ", e.getMessage());
    }
  }

  /**
   * Multicast prepare.
   *
   * @param roundIdentifier the round identifier
   * @param digest the digest
   */
  public void multicastPrepare(final ConsensusRoundIdentifier roundIdentifier, final Hash digest) {
    try {
      final Prepare data = messageFactory.createPrepare(roundIdentifier, digest);

      final PrepareMessageData message = PrepareMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Prepare (not sent): {} ", e.getMessage());
    }
  }

  /**
   * Multicast commit.
   *
   * @param roundIdentifier the round identifier
   * @param digest the digest
   * @param commitSeal the commit seal
   */
  public void multicastCommit(
      final ConsensusRoundIdentifier roundIdentifier,
      final Hash digest,
      final SECPSignature commitSeal) {
    try {
      final Commit data = messageFactory.createCommit(roundIdentifier, digest, commitSeal);

      final CommitMessageData message = CommitMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Commit (not sent): {} ", e.getMessage());
    }
  }

  /**
   * Multicast round change.
   *
   * @param roundIdentifier the round identifier
   * @param preparedRoundCertificate the prepared round certificate
   */
  public void multicastRoundChange(
      final ConsensusRoundIdentifier roundIdentifier,
      final Optional<PreparedCertificate> preparedRoundCertificate) {
    try {
      final RoundChange data =
          messageFactory.createRoundChange(roundIdentifier, preparedRoundCertificate);

      final RoundChangeMessageData message = RoundChangeMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for RoundChange (not sent): {} ", e.getMessage());
    }
  }
}
