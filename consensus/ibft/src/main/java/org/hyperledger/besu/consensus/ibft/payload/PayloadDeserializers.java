/*
 * Copyright 2020 ConsenSys AG.
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

import org.idnecology.idn.consensus.common.bft.payload.Payload;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.ethereum.rlp.RLPInput;

/** The Payload deserializers. */
public class PayloadDeserializers {
  /** Default constructor. */
  protected PayloadDeserializers() {}

  /**
   * Read signed proposal payload from rlp input.
   *
   * @param rlpInput the rlp input
   * @return the signed data
   */
  public static SignedData<ProposalPayload> readSignedProposalPayloadFrom(final RLPInput rlpInput) {

    rlpInput.enterList();
    final ProposalPayload unsignedMessageData = ProposalPayload.readFrom(rlpInput);
    final SECPSignature signature = readSignature(rlpInput);
    rlpInput.leaveList();

    return from(unsignedMessageData, signature);
  }

  /**
   * Read signed prepare payload from rlp input.
   *
   * @param rlpInput the rlp input
   * @return the signed data
   */
  public static SignedData<PreparePayload> readSignedPreparePayloadFrom(final RLPInput rlpInput) {

    rlpInput.enterList();
    final PreparePayload unsignedMessageData = PreparePayload.readFrom(rlpInput);
    final SECPSignature signature = readSignature(rlpInput);
    rlpInput.leaveList();

    return from(unsignedMessageData, signature);
  }

  /**
   * Read signed commit payload from rlp input.
   *
   * @param rlpInput the rlp input
   * @return the signed data
   */
  public static SignedData<CommitPayload> readSignedCommitPayloadFrom(final RLPInput rlpInput) {

    rlpInput.enterList();
    final CommitPayload unsignedMessageData = CommitPayload.readFrom(rlpInput);
    final SECPSignature signature = readSignature(rlpInput);
    rlpInput.leaveList();

    return from(unsignedMessageData, signature);
  }

  /**
   * Read signed round change payload from rlp input.
   *
   * @param rlpInput the rlp input
   * @return the signed data
   */
  public static SignedData<RoundChangePayload> readSignedRoundChangePayloadFrom(
      final RLPInput rlpInput) {

    rlpInput.enterList();
    final RoundChangePayload unsignedMessageData = RoundChangePayload.readFrom(rlpInput);
    final SECPSignature signature = readSignature(rlpInput);
    rlpInput.leaveList();

    return from(unsignedMessageData, signature);
  }

  /**
   * Create signed payload data from unsigned message data.
   *
   * @param <M> the type parameter
   * @param unsignedMessageData the unsigned message data
   * @param signature the signature
   * @return the signed data
   */
  protected static <M extends Payload> SignedData<M> from(
      final M unsignedMessageData, final SECPSignature signature) {
    return SignedData.create(unsignedMessageData, signature);
  }

  /**
   * Read signature from signed message.
   *
   * @param signedMessage the signed message
   * @return the secp signature
   */
  protected static SECPSignature readSignature(final RLPInput signedMessage) {
    return signedMessage.readBytes(SignatureAlgorithmFactory.getInstance()::decodeSignature);
  }
}
