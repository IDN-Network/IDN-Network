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
package org.idnecology.idn.consensus.qbft.core.payload;

import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.payload.Payload;
import org.idnecology.idn.consensus.qbft.core.messagedata.QbftV1;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.rlp.RLPInput;
import org.idnecology.idn.ethereum.rlp.RLPOutput;

import java.util.Objects;
import java.util.StringJoiner;

/** The Prepare payload. */
public class PreparePayload extends QbftPayload {
  private static final int TYPE = QbftV1.PREPARE;
  private final ConsensusRoundIdentifier roundIdentifier;
  private final Hash digest;

  /**
   * Instantiates a new Prepare payload.
   *
   * @param roundIdentifier the round identifier
   * @param digest the digest
   */
  public PreparePayload(final ConsensusRoundIdentifier roundIdentifier, final Hash digest) {
    this.roundIdentifier = roundIdentifier;
    this.digest = digest;
  }

  /**
   * Read from rlp input and return prepare payload.
   *
   * @param rlpInput the rlp input
   * @return the prepare payload
   */
  public static PreparePayload readFrom(final RLPInput rlpInput) {
    rlpInput.enterList();
    final ConsensusRoundIdentifier roundIdentifier = readConsensusRound(rlpInput);
    final Hash digest = Payload.readDigest(rlpInput);
    rlpInput.leaveList();
    return new PreparePayload(roundIdentifier, digest);
  }

  @Override
  public void writeTo(final RLPOutput rlpOutput) {
    rlpOutput.startList();
    writeConsensusRound(rlpOutput);
    rlpOutput.writeBytes(digest);
    rlpOutput.endList();
  }

  @Override
  public int getMessageType() {
    return TYPE;
  }

  /**
   * Gets digest.
   *
   * @return the digest
   */
  public Hash getDigest() {
    return digest;
  }

  @Override
  public ConsensusRoundIdentifier getRoundIdentifier() {
    return roundIdentifier;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final PreparePayload that = (PreparePayload) o;
    return Objects.equals(roundIdentifier, that.roundIdentifier)
        && Objects.equals(digest, that.digest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roundIdentifier, digest);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", PreparePayload.class.getSimpleName() + "[", "]")
        .add("roundIdentifier=" + roundIdentifier)
        .add("digest=" + digest)
        .toString();
  }
}
