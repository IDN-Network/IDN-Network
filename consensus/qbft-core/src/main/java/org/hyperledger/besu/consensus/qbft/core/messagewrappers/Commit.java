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
package org.idnecology.idn.consensus.qbft.core.messagewrappers;

import org.idnecology.idn.consensus.common.bft.messagewrappers.BftMessage;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.qbft.core.payload.CommitPayload;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.rlp.RLPInput;

import org.apache.tuweni.bytes.Bytes;

/** The Commit payload message. */
public class Commit extends BftMessage<CommitPayload> {

  /**
   * Instantiates a new Commit.
   *
   * @param payload the payload
   */
  public Commit(final SignedData<CommitPayload> payload) {
    super(payload);
  }

  /**
   * Gets commit seal.
   *
   * @return the commit seal
   */
  public SECPSignature getCommitSeal() {
    return getPayload().getCommitSeal();
  }

  /**
   * Gets digest.
   *
   * @return the digest
   */
  public Hash getDigest() {
    return getPayload().getDigest();
  }

  /**
   * Decode.
   *
   * @param data the data
   * @return the commit
   */
  public static Commit decode(final Bytes data) {
    final RLPInput rlpIn = RLP.input(data);

    return new Commit(readPayload(rlpIn, CommitPayload::readFrom));
  }
}
