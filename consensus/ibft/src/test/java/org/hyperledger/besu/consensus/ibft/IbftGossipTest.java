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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.verify;

import org.idnecology.idn.consensus.common.bft.messagewrappers.BftMessage;
import org.idnecology.idn.consensus.common.bft.network.MockPeerFactory;
import org.idnecology.idn.consensus.common.bft.network.ValidatorMulticaster;
import org.idnecology.idn.consensus.ibft.messagedata.ProposalMessageData;
import org.idnecology.idn.consensus.ibft.messagedata.RoundChangeMessageData;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.p2p.rlpx.connections.PeerConnection;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.DefaultMessage;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Message;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IbftGossipTest {
  private IbftGossip ibftGossip;
  @Mock private ValidatorMulticaster validatorMulticaster;
  private PeerConnection peerConnection;
  private static final Address senderAddress = AddressHelpers.ofValue(9);

  @BeforeEach
  public void setup() {
    ibftGossip = new IbftGossip(validatorMulticaster);
    peerConnection = MockPeerFactory.create(senderAddress);
  }

  private <P extends BftMessage<?>> void assertRebroadcastToAllExceptSignerAndSender(
      final Function<NodeKey, P> createPayload, final Function<P, MessageData> createMessageData) {
    final NodeKey nodeKey = NodeKeyUtils.generate();
    final P payload = createPayload.apply(nodeKey);
    final MessageData messageData = createMessageData.apply(payload);
    final Message message = new DefaultMessage(peerConnection, messageData);

    ibftGossip.send(message);
    verify(validatorMulticaster)
        .send(messageData, newArrayList(senderAddress, payload.getAuthor()));
  }

  @Test
  public void assertRebroadcastsProposalToAllExceptSignerAndSender() {
    assertRebroadcastToAllExceptSignerAndSender(
        TestHelpers::createSignedProposalPayload, ProposalMessageData::create);
  }

  @Test
  public void assertRebroadcastsRoundChangeToAllExceptSignerAndSender() {
    assertRebroadcastToAllExceptSignerAndSender(
        TestHelpers::createSignedRoundChangePayload, RoundChangeMessageData::create);
  }
}
