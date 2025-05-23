/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.ethereum.p2p.discovery.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.p2p.discovery.DiscoveryPeer;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.DaggerPacketPackage;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.Packet;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.PacketData;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.PacketPackage;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.findneighbors.FindNeighborsPacketData;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.findneighbors.FindNeighborsPacketDataFactory;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.neighbors.NeighborsPacketData;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.pong.PongPacketData;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.validation.ExpiryValidator;
import org.idnecology.idn.ethereum.p2p.discovery.internal.packet.validation.TargetValidator;
import org.idnecology.idn.ethereum.p2p.peers.Peer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;

class MockPacketDataFactory {
  private static final PacketPackage PACKET_PACKAGE = DaggerPacketPackage.create();

  static Packet mockNeighborsPacket(final DiscoveryPeer from, final DiscoveryPeer... neighbors) {
    final Packet packet = mock(Packet.class);

    final NeighborsPacketData packetData =
        PACKET_PACKAGE.neighborsPacketDataFactory().create(Arrays.asList(neighbors));

    when(packet.getPacketData(any())).thenReturn(Optional.of(packetData));
    final Bytes id = from.getId();
    when(packet.getNodeId()).thenReturn(id);
    when(packet.getType()).thenReturn(PacketType.NEIGHBORS);
    when(packet.getHash()).thenReturn(Bytes32.ZERO);

    return packet;
  }

  static Packet mockPongPacket(final DiscoveryPeer from, final Bytes pingHash) {
    final Packet packet = mock(Packet.class);

    final PongPacketData pongPacketData =
        PACKET_PACKAGE.pongPacketDataFactory().create(from.getEndpoint(), pingHash, UInt64.ONE);
    when(packet.getPacketData(any())).thenReturn(Optional.of(pongPacketData));
    final Bytes id = from.getId();
    when(packet.getNodeId()).thenReturn(id);
    when(packet.getType()).thenReturn(PacketType.PONG);
    when(packet.getHash()).thenReturn(Bytes32.ZERO);

    return packet;
  }

  static Packet mockFindNeighborsPacket(final Peer from) {
    return mockFindNeighborsPacket(from, PacketData.defaultExpiration());
  }

  static Packet mockFindNeighborsPacket(final Peer from, final long exparationMs) {
    final Packet packet = mock(Packet.class);
    final Bytes target =
        Bytes.fromHexString(
            "0x0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f40");

    Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(exparationMs - 1), ZoneId.of("UTC"));
    FindNeighborsPacketDataFactory findNeighborsPacketDataFactory =
        new FindNeighborsPacketDataFactory(
            new TargetValidator(), new ExpiryValidator(fixedClock), fixedClock);
    final FindNeighborsPacketData packetData =
        findNeighborsPacketDataFactory.create(target, exparationMs);
    when(packet.getPacketData(any())).thenReturn(Optional.of(packetData));
    final Bytes id = from.getId();
    when(packet.getNodeId()).thenReturn(id);
    when(packet.getType()).thenReturn(PacketType.FIND_NEIGHBORS);
    when(packet.getHash()).thenReturn(Bytes32.ZERO);

    return packet;
  }
}
