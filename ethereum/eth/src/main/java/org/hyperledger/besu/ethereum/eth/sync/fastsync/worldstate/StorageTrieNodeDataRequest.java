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
package org.idnecology.idn.ethereum.eth.sync.fastsync.worldstate;

import static org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator.applyForStrategy;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.rlp.RLPOutput;
import org.idnecology.idn.ethereum.trie.CompactEncoding;
import org.idnecology.idn.ethereum.worldstate.WorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;

class StorageTrieNodeDataRequest extends TrieNodeDataRequest {

  final Optional<Hash> accountHash;

  StorageTrieNodeDataRequest(
      final Hash hash, final Optional<Hash> accountHash, final Optional<Bytes> location) {
    super(RequestType.STORAGE_TRIE_NODE, hash, location);
    this.accountHash = accountHash;
  }

  @Override
  protected void doPersist(final WorldStateKeyValueStorage.Updater updater) {
    applyForStrategy(
        updater,
        onBonsai -> {
          onBonsai.putAccountStorageTrieNode(
              accountHash.orElse(Hash.EMPTY),
              getLocation().orElse(Bytes.EMPTY),
              getHash(),
              getData());
        },
        onForest -> {
          onForest.putAccountStorageTrieNode(getHash(), getData());
        });
  }

  @Override
  public Optional<Bytes> getExistingData(
      final WorldStateStorageCoordinator worldStateStorageCoordinator) {
    return getAccountHash()
        .flatMap(
            accountHash ->
                getLocation()
                    .flatMap(
                        location ->
                            worldStateStorageCoordinator
                                .getAccountStorageTrieNode(accountHash, location, getHash())
                                .filter(data -> Hash.hash(data).equals(getHash()))));
  }

  @Override
  protected NodeDataRequest createChildNodeDataRequest(
      final Hash childHash, final Optional<Bytes> location) {
    return NodeDataRequest.createStorageDataRequest(childHash, getAccountHash(), location);
  }

  @Override
  protected Stream<NodeDataRequest> getRequestsFromTrieNodeValue(
      final WorldStateStorageCoordinator worldStateStorageCoordinator,
      final Optional<Bytes> location,
      final Bytes path,
      final Bytes value) {

    worldStateStorageCoordinator.applyWhenFlatModeEnabled(
        worldStateKeyValueStorage -> {
          worldStateKeyValueStorage
              .updater()
              .putStorageValueBySlotHash(
                  accountHash.get(),
                  getSlotHash(location, path),
                  Bytes32.leftPad(RLP.decodeValue(value)))
              .commit();
        });

    return Stream.empty();
  }

  public Optional<Hash> getAccountHash() {
    return accountHash;
  }

  @Override
  protected void writeTo(final RLPOutput out) {
    out.startList();
    out.writeByte(getRequestType().getValue());
    out.writeBytes(getHash());
    getAccountHash().ifPresent(out::writeBytes);
    getLocation().ifPresent(out::writeBytes);
    out.endList();
  }

  private Hash getSlotHash(final Optional<Bytes> location, final Bytes path) {
    return Hash.wrap(
        Bytes32.wrap(
            CompactEncoding.pathToBytes(Bytes.concatenate(location.orElse(Bytes.EMPTY), path))));
  }
}
