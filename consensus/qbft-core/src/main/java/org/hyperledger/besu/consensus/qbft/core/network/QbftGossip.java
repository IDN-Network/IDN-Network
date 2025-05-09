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

import org.idnecology.idn.consensus.common.bft.Gossiper;
import org.idnecology.idn.consensus.common.bft.network.ValidatorMulticaster;
import org.idnecology.idn.consensus.common.bft.payload.Authored;
import org.idnecology.idn.consensus.qbft.core.messagedata.CommitMessageData;
import org.idnecology.idn.consensus.qbft.core.messagedata.PrepareMessageData;
import org.idnecology.idn.consensus.qbft.core.messagedata.ProposalMessageData;
import org.idnecology.idn.consensus.qbft.core.messagedata.QbftV1;
import org.idnecology.idn.consensus.qbft.core.messagedata.RoundChangeMessageData;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Message;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

import java.util.List;

import com.google.common.collect.Lists;

/** Class responsible for rebroadcasting QBFT messages to known validators */
public class QbftGossip implements Gossiper {

  private final ValidatorMulticaster multicaster;
  private final QbftBlockCodec blockEncoder;

  /**
   * Constructor that attaches gossip logic to a set of multicaster
   *
   * @param multicaster Network connections to the remote validators
   * @param blockEncoder the block encoder
   */
  public QbftGossip(final ValidatorMulticaster multicaster, final QbftBlockCodec blockEncoder) {
    this.multicaster = multicaster;
    this.blockEncoder = blockEncoder;
  }

  /**
   * Retransmit a given QBFT message to other known validators nodes
   *
   * @param message The raw message to be gossiped
   */
  @Override
  public void send(final Message message) {
    final MessageData messageData = message.getData();
    final Authored decodedMessage =
        switch (messageData.getCode()) {
          case QbftV1.PROPOSAL ->
              ProposalMessageData.fromMessageData(messageData).decode(blockEncoder);
          case QbftV1.PREPARE -> PrepareMessageData.fromMessageData(messageData).decode();
          case QbftV1.COMMIT -> CommitMessageData.fromMessageData(messageData).decode();
          case QbftV1.ROUND_CHANGE ->
              RoundChangeMessageData.fromMessageData(messageData).decode(blockEncoder);
          default ->
              throw new IllegalArgumentException(
                  "Received message does not conform to any recognised QBFT message structure.");
        };
    final List<Address> excludeAddressesList =
        Lists.newArrayList(
            message.getConnection().getPeerInfo().getAddress(), decodedMessage.getAuthor());

    multicaster.send(messageData, excludeAddressesList);
  }
}
