/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.eth.sync.snapsync;

import static org.idnecology.idn.ethereum.trie.RangeManager.MAX_RANGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.TrieGenerator;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.SnapDataRequest;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.StorageRangeDataRequest;
import org.idnecology.idn.ethereum.eth.sync.snapsync.request.heal.StorageTrieNodeHealingRequest;
import org.idnecology.idn.ethereum.proof.WorldStateProofProvider;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.trie.MerkleTrie;
import org.idnecology.idn.ethereum.trie.RangeStorageEntriesCollector;
import org.idnecology.idn.ethereum.trie.TrieIterator;
import org.idnecology.idn.ethereum.trie.common.PmtStateTrieAccountValue;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.patricia.StoredMerklePatriciaTrie;
import org.idnecology.idn.ethereum.trie.patricia.StoredNodeFactory;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import kotlin.collections.ArrayDeque;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AccountHealingTrackingTest {

  private final List<Address> accounts = List.of(Address.fromHexString("0xdeadbeef"));
  private final BonsaiWorldStateKeyValueStorage worldStateKeyValueStorage =
      new BonsaiWorldStateKeyValueStorage(
          new InMemoryKeyValueStorageProvider(),
          new NoOpMetricsSystem(),
          DataStorageConfiguration.DEFAULT_BONSAI_CONFIG);

  private WorldStateStorageCoordinator worldStateStorageCoordinator;
  private WorldStateProofProvider worldStateProofProvider;
  private MerkleTrie<Bytes, Bytes> accountStateTrie;

  @Mock SnapWorldDownloadState snapWorldDownloadState;

  @BeforeEach
  public void setup() {
    worldStateStorageCoordinator = new WorldStateStorageCoordinator(worldStateKeyValueStorage);
    accountStateTrie =
        TrieGenerator.generateTrie(
            worldStateStorageCoordinator,
            accounts.stream().map(Address::addressHash).collect(Collectors.toList()));
    worldStateProofProvider = new WorldStateProofProvider(worldStateStorageCoordinator);
  }

  @Test
  void shouldMarkAccountForHealingWhenStorageProofIsReceived() {
    final Hash accountHash = Hash.hash(accounts.get(0));
    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(
            RLP.input(accountStateTrie.get(accountHash).orElseThrow()));

    final StoredMerklePatriciaTrie<Bytes, Bytes> storageTrie =
        new StoredMerklePatriciaTrie<>(
            new StoredNodeFactory<>(
                (location, hash) ->
                    worldStateKeyValueStorage.getAccountStorageTrieNode(
                        accountHash, location, hash),
                Function.identity(),
                Function.identity()),
            stateTrieAccountValue.getStorageRoot());

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(Hash.ZERO, MAX_RANGE, 10, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes32, Bytes> slots =
        (TreeMap<Bytes32, Bytes>)
            storageTrie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, Hash.ZERO));

    final List<Bytes> proofs =
        worldStateProofProvider.getStorageProofRelatedNodes(
            Hash.wrap(storageTrie.getRootHash()), accountHash, Hash.ZERO);
    proofs.addAll(
        worldStateProofProvider.getStorageProofRelatedNodes(
            Hash.wrap(storageTrie.getRootHash()), accountHash, slots.lastKey()));

    final StorageRangeDataRequest storageRangeDataRequest =
        SnapDataRequest.createStorageRangeDataRequest(
            Hash.wrap(accountStateTrie.getRootHash()),
            accountHash,
            storageTrie.getRootHash(),
            Hash.ZERO,
            MAX_RANGE);
    storageRangeDataRequest.addResponse(
        snapWorldDownloadState, worldStateProofProvider, slots, new ArrayDeque<>(proofs));
    storageRangeDataRequest.getChildRequests(
        snapWorldDownloadState, worldStateStorageCoordinator, null);

    verify(snapWorldDownloadState).addAccountToHealingList(any(Bytes.class));
  }

  @Test
  void shouldNotMarkAccountForHealingWhenAllStorageIsReceivedWithoutProof() {
    final Hash accountHash = Hash.hash(accounts.get(0));
    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(
            RLP.input(accountStateTrie.get(accountHash).orElseThrow()));

    final StoredMerklePatriciaTrie<Bytes, Bytes> storageTrie =
        new StoredMerklePatriciaTrie<>(
            new StoredNodeFactory<>(
                (location, hash) ->
                    worldStateKeyValueStorage.getAccountStorageTrieNode(
                        accountHash, location, hash),
                Function.identity(),
                Function.identity()),
            stateTrieAccountValue.getStorageRoot());

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(Hash.ZERO, MAX_RANGE, 10, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes32, Bytes> slots =
        (TreeMap<Bytes32, Bytes>)
            storageTrie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, Hash.ZERO));

    final StorageRangeDataRequest storageRangeDataRequest =
        SnapDataRequest.createStorageRangeDataRequest(
            Hash.wrap(accountStateTrie.getRootHash()),
            accountHash,
            storageTrie.getRootHash(),
            Hash.ZERO,
            MAX_RANGE);
    storageRangeDataRequest.addResponse(
        snapWorldDownloadState, worldStateProofProvider, slots, new ArrayDeque<>());
    storageRangeDataRequest.getChildRequests(
        snapWorldDownloadState, worldStateStorageCoordinator, null);

    verify(snapWorldDownloadState, never()).addAccountToHealingList(any(Bytes.class));
  }

  @Test
  void shouldMarkAccountForHealingOnInvalidStorageProof() {
    final Hash accountHash = Hash.hash(accounts.get(0));
    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(
            RLP.input(accountStateTrie.get(accountHash).orElseThrow()));

    final List<Bytes> proofs =
        List.of(
            worldStateKeyValueStorage
                .getAccountStorageTrieNode(
                    accountHash, Bytes.EMPTY, stateTrieAccountValue.getStorageRoot())
                .get());

    final StorageRangeDataRequest storageRangeDataRequest =
        SnapDataRequest.createStorageRangeDataRequest(
            Hash.wrap(accountStateTrie.getRootHash()),
            accountHash,
            stateTrieAccountValue.getStorageRoot(),
            Hash.ZERO,
            MAX_RANGE);
    storageRangeDataRequest.addResponse(
        snapWorldDownloadState, worldStateProofProvider, new TreeMap<>(), new ArrayDeque<>(proofs));

    verify(snapWorldDownloadState).addAccountToHealingList(any(Bytes.class));
  }

  @Test
  void shouldMarkAccountForHealingOnInvalidStorageWithoutProof() {
    final Hash accountHash = Hash.hash(accounts.get(0));
    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(
            RLP.input(accountStateTrie.get(accountHash).orElseThrow()));

    final StoredMerklePatriciaTrie<Bytes, Bytes> storageTrie =
        new StoredMerklePatriciaTrie<>(
            new StoredNodeFactory<>(
                (location, hash) ->
                    worldStateKeyValueStorage.getAccountStorageTrieNode(
                        accountHash, location, hash),
                Function.identity(),
                Function.identity()),
            stateTrieAccountValue.getStorageRoot());

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(Hash.ZERO, MAX_RANGE, 1, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes32, Bytes> slots =
        (TreeMap<Bytes32, Bytes>)
            storageTrie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, Hash.ZERO));

    final StorageRangeDataRequest storageRangeDataRequest =
        SnapDataRequest.createStorageRangeDataRequest(
            Hash.wrap(accountStateTrie.getRootHash()),
            accountHash,
            storageTrie.getRootHash(),
            Hash.ZERO,
            MAX_RANGE);
    storageRangeDataRequest.addResponse(
        snapWorldDownloadState, worldStateProofProvider, slots, new ArrayDeque<>());

    verify(snapWorldDownloadState).addAccountToHealingList(any(Bytes.class));
  }

  @Test
  void shouldMarkAccountForHealingOnPartialStorageRange() {
    final Hash accountHash = Hash.hash(accounts.get(0));
    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(
            RLP.input(accountStateTrie.get(accountHash).orElseThrow()));

    final StoredMerklePatriciaTrie<Bytes, Bytes> storageTrie =
        new StoredMerklePatriciaTrie<>(
            new StoredNodeFactory<>(
                (location, hash) ->
                    worldStateKeyValueStorage.getAccountStorageTrieNode(
                        accountHash, location, hash),
                Function.identity(),
                Function.identity()),
            stateTrieAccountValue.getStorageRoot());

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(Hash.ZERO, MAX_RANGE, 1, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes32, Bytes> slots =
        (TreeMap<Bytes32, Bytes>)
            storageTrie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, Hash.ZERO));

    final List<Bytes> proofs =
        worldStateProofProvider.getStorageProofRelatedNodes(
            Hash.wrap(storageTrie.getRootHash()), accountHash, Hash.ZERO);
    proofs.addAll(
        worldStateProofProvider.getStorageProofRelatedNodes(
            Hash.wrap(storageTrie.getRootHash()), accountHash, slots.lastKey()));

    final StorageRangeDataRequest storageRangeDataRequest =
        SnapDataRequest.createStorageRangeDataRequest(
            Hash.wrap(accountStateTrie.getRootHash()),
            accountHash,
            storageTrie.getRootHash(),
            Hash.ZERO,
            MAX_RANGE);
    storageRangeDataRequest.addResponse(
        snapWorldDownloadState, worldStateProofProvider, slots, new ArrayDeque<>(proofs));
    verify(snapWorldDownloadState, never()).addAccountToHealingList(any(Bytes.class));

    storageRangeDataRequest.getChildRequests(
        snapWorldDownloadState, worldStateStorageCoordinator, null);

    verify(snapWorldDownloadState).addAccountToHealingList(any(Bytes.class));
  }

  @Test
  void shouldNotMarkAccountForHealingOnValidStorageTrieNodeDetection() {
    final Hash accountHash = Hash.hash(accounts.get(0));
    final PmtStateTrieAccountValue stateTrieAccountValue =
        PmtStateTrieAccountValue.readFrom(
            RLP.input(accountStateTrie.get(accountHash).orElseThrow()));
    final StorageTrieNodeHealingRequest storageTrieNodeHealingRequest =
        SnapDataRequest.createStorageTrieNodeDataRequest(
            stateTrieAccountValue.getStorageRoot(),
            accountHash,
            Hash.wrap(accountStateTrie.getRootHash()),
            Bytes.EMPTY);
    storageTrieNodeHealingRequest.getExistingData(worldStateStorageCoordinator);

    verify(snapWorldDownloadState, never()).addAccountToHealingList(any(Bytes.class));
  }
}
