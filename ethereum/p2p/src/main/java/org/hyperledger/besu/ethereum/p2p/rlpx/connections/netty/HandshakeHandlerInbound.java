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
package org.idnecology.idn.ethereum.p2p.rlpx.connections.netty;

import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.p2p.discovery.internal.PeerTable;
import org.idnecology.idn.ethereum.p2p.peers.LocalNode;
import org.idnecology.idn.ethereum.p2p.rlpx.connections.PeerConnection;
import org.idnecology.idn.ethereum.p2p.rlpx.connections.PeerConnectionEventDispatcher;
import org.idnecology.idn.ethereum.p2p.rlpx.framing.FramerProvider;
import org.idnecology.idn.ethereum.p2p.rlpx.handshake.Handshaker;
import org.idnecology.idn.ethereum.p2p.rlpx.handshake.HandshakerProvider;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.SubProtocol;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;

final class HandshakeHandlerInbound extends AbstractHandshakeHandler {

  public HandshakeHandlerInbound(
      final NodeKey nodeKey,
      final List<SubProtocol> subProtocols,
      final LocalNode localNode,
      final CompletableFuture<PeerConnection> connectionFuture,
      final PeerConnectionEventDispatcher connectionEventDispatcher,
      final MetricsSystem metricsSystem,
      final HandshakerProvider handshakerProvider,
      final FramerProvider framerProvider,
      final PeerTable peerTable) {
    super(
        subProtocols,
        localNode,
        Optional.empty(),
        connectionFuture,
        connectionEventDispatcher,
        metricsSystem,
        handshakerProvider,
        framerProvider,
        true,
        peerTable);
    handshaker.prepareResponder(nodeKey);
  }

  @Override
  protected Optional<ByteBuf> nextHandshakeMessage(final ByteBuf msg) {
    final Optional<ByteBuf> nextMsg;
    if (handshaker.getStatus() == Handshaker.HandshakeStatus.IN_PROGRESS) {
      nextMsg = handshaker.handleMessage(msg);
    } else {
      nextMsg = Optional.empty();
    }
    return nextMsg;
  }
}
