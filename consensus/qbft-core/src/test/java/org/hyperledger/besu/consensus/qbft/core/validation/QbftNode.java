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
package org.idnecology.idn.consensus.qbft.core.validation;

import org.idnecology.idn.consensus.qbft.core.payload.MessageFactory;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.Util;

public class QbftNode {

  final NodeKey nodeKey;
  final MessageFactory messageFactory;

  private QbftNode(final NodeKey nodeKey, final MessageFactory messageFactory) {
    this.nodeKey = nodeKey;
    this.messageFactory = messageFactory;
  }

  public Address getAddress() {
    return Util.publicKeyToAddress(nodeKey.getPublicKey());
  }

  public MessageFactory getMessageFactory() {
    return messageFactory;
  }

  public NodeKey getNodeKey() {
    return nodeKey;
  }

  public static QbftNode create(final QbftBlockCodec qbftBlockCodec) {
    final NodeKey nodeKey = NodeKeyUtils.generate();
    final MessageFactory factory = new MessageFactory(nodeKey, qbftBlockCodec);

    return new QbftNode(nodeKey, factory);
  }
}
