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
package org.idnecology.idn.ethereum.proof;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.trie.MerkleTrie;
import org.idnecology.idn.ethereum.trie.common.PmtStateTrieAccountValue;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.patricia.StoredMerklePatriciaTrie;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WorldStateProofProviderTest {

  private static final Address address =
      Address.fromHexString("0x1234567890123456789012345678901234567890");
  private final ForestWorldStateKeyValueStorage worldStateKeyValueStorage =
      new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());

  private WorldStateProofProvider worldStateProofProvider;

  @BeforeEach
  public void setup() {
    worldStateProofProvider =
        new WorldStateProofProvider(new WorldStateStorageCoordinator(worldStateKeyValueStorage));
  }

  @Test
  public void getProofWhenWorldStateNotAvailable() {
    Optional<WorldStateProof> accountProof =
        worldStateProofProvider.getAccountProof(Hash.EMPTY, address, new ArrayList<>());

    assertThat(accountProof).isEmpty();
  }

  @Test
  public void getProofWhenWorldStateAvailable() {
    final Hash addressHash = address.addressHash();
    final MerkleTrie<Bytes32, Bytes> worldStateTrie = emptyWorldStateTrie();
    final MerkleTrie<Bytes32, Bytes> storageTrie = emptyStorageTrie();

    final ForestWorldStateKeyValueStorage.Updater updater = worldStateKeyValueStorage.updater();

    // Add some storage values
    writeStorageValue(storageTrie, UInt256.ONE, UInt256.valueOf(2L));
    writeStorageValue(storageTrie, UInt256.valueOf(2L), UInt256.valueOf(4L));
    writeStorageValue(storageTrie, UInt256.valueOf(3L), UInt256.valueOf(6L));
    // Save to Storage
    storageTrie.commit((location, hash, value) -> updater.putAccountStorageTrieNode(hash, value));

    // Define account value
    final Hash codeHash = Hash.hash(Bytes.fromHexString("0x1122"));
    final PmtStateTrieAccountValue accountValue =
        new PmtStateTrieAccountValue(
            1L, Wei.of(2L), Hash.wrap(storageTrie.getRootHash()), codeHash);
    // Save to storage
    worldStateTrie.put(addressHash, RLP.encode(accountValue::writeTo));
    worldStateTrie.commit((location, hash, value) -> updater.putAccountStateTrieNode(hash, value));

    // Persist updates
    updater.commit();

    final List<UInt256> storageKeys =
        Arrays.asList(UInt256.ONE, UInt256.valueOf(3L), UInt256.valueOf(6L));
    final Optional<WorldStateProof> accountProof =
        worldStateProofProvider.getAccountProof(
            Hash.wrap(worldStateTrie.getRootHash()), address, storageKeys);

    assertThat(accountProof).isPresent();
    Assertions.assertThat(accountProof.get().getStateTrieAccountValue())
        .isEqualToComparingFieldByField(accountValue);
    assertThat(accountProof.get().getAccountProof().size()).isGreaterThanOrEqualTo(1);
    // Check storage fields
    assertThat(accountProof.get().getStorageKeys()).isEqualTo(storageKeys);
    // Check key 1
    UInt256 storageKey = UInt256.ONE;
    assertThat(accountProof.get().getStorageValue(storageKey)).isEqualTo(UInt256.valueOf(2L));
    assertThat(accountProof.get().getStorageProof(storageKey).size()).isGreaterThanOrEqualTo(1);
    // Check key 3
    storageKey = UInt256.valueOf(3L);
    assertThat(accountProof.get().getStorageValue(storageKey)).isEqualTo(UInt256.valueOf(6L));
    assertThat(accountProof.get().getStorageProof(storageKey).size()).isGreaterThanOrEqualTo(1);
    // Check key 6
    storageKey = UInt256.valueOf(6L);
    assertThat(accountProof.get().getStorageValue(storageKey)).isEqualTo(UInt256.ZERO);
    assertThat(accountProof.get().getStorageProof(storageKey).size()).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void getProofWhenStateTrieAccountUnavailable() {
    final MerkleTrie<Bytes32, Bytes> worldStateTrie = emptyWorldStateTrie();

    final Optional<WorldStateProof> accountProof =
        worldStateProofProvider.getAccountProof(
            Hash.wrap(worldStateTrie.getRootHash()), address, new ArrayList<>());

    assertThat(accountProof).isEmpty();
  }

  private void writeStorageValue(
      final MerkleTrie<Bytes32, Bytes> storageTrie, final UInt256 key, final UInt256 value) {
    storageTrie.put(storageKeyHash(key), encodeStorageValue(value));
  }

  private Bytes32 storageKeyHash(final UInt256 storageKey) {
    return Hash.hash(storageKey);
  }

  private Bytes encodeStorageValue(final UInt256 storageValue) {
    return RLP.encode(out -> out.writeBytes(storageValue.toMinimalBytes()));
  }

  private MerkleTrie<Bytes32, Bytes> emptyStorageTrie() {
    return new StoredMerklePatriciaTrie<>(
        (location, hash) -> worldStateKeyValueStorage.getAccountStateTrieNode(hash),
        b -> b,
        b -> b);
  }

  private MerkleTrie<Bytes32, Bytes> emptyWorldStateTrie() {
    return new StoredMerklePatriciaTrie<>(
        (location, hash) -> worldStateKeyValueStorage.getAccountStorageTrieNode(hash),
        b -> b,
        b -> b);
  }
}
