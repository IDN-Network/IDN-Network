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
package org.idnecology.idn.ethereum.eth.manager.task;

import static java.util.Collections.emptyMap;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.PendingPeerRequest;
import org.idnecology.idn.ethereum.eth.messages.EthPV63;
import org.idnecology.idn.ethereum.eth.messages.NodeDataMessage;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetNodeDataFromPeerTask extends AbstractPeerRequestTask<Map<Hash, Bytes>> {

  private static final Logger LOG = LoggerFactory.getLogger(GetNodeDataFromPeerTask.class);

  private final Set<Hash> hashes;
  private final long pivotBlockNumber;

  private GetNodeDataFromPeerTask(
      final EthContext ethContext,
      final Collection<Hash> hashes,
      final long pivotBlockNumber,
      final MetricsSystem metricsSystem) {
    super(ethContext, EthPV63.GET_NODE_DATA, metricsSystem);
    this.hashes = new HashSet<>(hashes);
    this.pivotBlockNumber = pivotBlockNumber;
  }

  public static GetNodeDataFromPeerTask forHashes(
      final EthContext ethContext,
      final Collection<Hash> hashes,
      final long pivotBlockNumber,
      final MetricsSystem metricsSystem) {
    return new GetNodeDataFromPeerTask(ethContext, hashes, pivotBlockNumber, metricsSystem);
  }

  @Override
  protected PendingPeerRequest sendRequest() {
    return sendRequestToPeer(
        peer -> {
          LOG.atTrace()
              .setMessage("Requesting {} node data entries from peer {}")
              .addArgument(hashes::size)
              .addArgument(peer::getLoggableId)
              .log();
          return peer.getNodeData(hashes);
        },
        pivotBlockNumber);
  }

  @Override
  protected Optional<Map<Hash, Bytes>> processResponse(
      final boolean streamClosed, final MessageData message, final EthPeer peer) {
    if (streamClosed) {
      // We don't record this as a useless response because it's impossible to know if a peer has
      // the data we're requesting.
      return Optional.of(emptyMap());
    }
    final NodeDataMessage nodeDataMessage = NodeDataMessage.readFrom(message);
    final List<Bytes> nodeData = nodeDataMessage.nodeData();
    if (nodeData.size() > hashes.size()) {
      // Can't be the response to our request
      return Optional.empty();
    }
    return mapNodeDataByHash(nodeData);
  }

  private Optional<Map<Hash, Bytes>> mapNodeDataByHash(final List<Bytes> nodeData) {
    final Map<Hash, Bytes> nodeDataByHash = new HashMap<>();
    for (final Bytes data : nodeData) {
      final Hash hash = Hash.hash(data);
      if (!hashes.contains(hash)) {
        return Optional.empty();
      }
      nodeDataByHash.put(hash, data);
    }
    return Optional.of(nodeDataByHash);
  }
}
