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
package org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.flat;

import static org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE;
import static org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.StorageSlotKey;
import org.idnecology.idn.ethereum.trie.NodeLoader;
import org.idnecology.idn.ethereum.trie.diffbased.common.storage.flat.CodeStorageStrategy;
import org.idnecology.idn.ethereum.trie.patricia.StoredMerklePatriciaTrie;
import org.idnecology.idn.ethereum.trie.patricia.StoredNodeFactory;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;

/**
 * This class represents a strategy for reading data from a partial flat database. It extends the
 * FlatDbReaderStrategy and provides additional functionality for reading data from a merkle trie.
 * If data is missing in the flat database, this strategy falls back to the merkle trie to retrieve
 * the data. It adds a fallback mechanism for the `getAccount` and `getStorageValueByStorageSlotKey`
 * methods, which checks if the data is present in the flat database, and if not, queries the merkle
 * trie
 */
public class BonsaiPartialFlatDbStrategy extends BonsaiFlatDbStrategy {

  protected final Counter getAccountMerkleTrieCounter;
  protected final Counter getAccountMissingMerkleTrieCounter;

  protected final Counter getStorageValueMerkleTrieCounter;
  protected final Counter getStorageValueMissingMerkleTrieCounter;

  public BonsaiPartialFlatDbStrategy(
      final MetricsSystem metricsSystem, final CodeStorageStrategy codeStorageStrategy) {
    super(metricsSystem, codeStorageStrategy);
    getAccountMerkleTrieCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "get_account_merkle_trie",
            "Number of accounts not found in the flat database, but found in the merkle trie");

    getAccountMissingMerkleTrieCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "get_account_missing_merkle_trie",
            "Number of accounts not found (either in the flat database or the merkle trie)");

    getStorageValueMerkleTrieCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "get_storagevalue_merkle_trie",
            "Number of storage slots not found in the flat database, but found in the merkle trie");

    getStorageValueMissingMerkleTrieCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "get_storagevalue_missing_merkle_trie",
            "Number of storage slots not found (either in the flat database or in the merkle trie)");
  }

  @Override
  public Optional<Bytes> getFlatAccount(
      final Supplier<Optional<Bytes>> worldStateRootHashSupplier,
      final NodeLoader nodeLoader,
      final Hash accountHash,
      final SegmentedKeyValueStorage storage) {
    getAccountCounter.inc();
    Optional<Bytes> response =
        storage.get(ACCOUNT_INFO_STATE, accountHash.toArrayUnsafe()).map(Bytes::wrap);
    if (response.isEmpty()) {
      // after a snapsync/fastsync we only have the trie branches.
      final Optional<Bytes> worldStateRootHash = worldStateRootHashSupplier.get();
      if (worldStateRootHash.isPresent()) {
        response =
            new StoredMerklePatriciaTrie<>(
                    new StoredNodeFactory<>(nodeLoader, Function.identity(), Function.identity()),
                    Bytes32.wrap(worldStateRootHash.get()))
                .get(accountHash);
        if (response.isEmpty()) {
          getAccountMissingMerkleTrieCounter.inc();
        } else {
          getAccountMerkleTrieCounter.inc();
        }
      }
    } else {
      getAccountFoundInFlatDatabaseCounter.inc();
    }

    return response;
  }

  @Override
  public Optional<Bytes> getFlatStorageValueByStorageSlotKey(
      final Supplier<Optional<Bytes>> worldStateRootHashSupplier,
      final Supplier<Optional<Hash>> storageRootSupplier,
      final NodeLoader nodeLoader,
      final Hash accountHash,
      final StorageSlotKey storageSlotKey,
      final SegmentedKeyValueStorage storage) {
    getStorageValueCounter.inc();
    Optional<Bytes> response =
        storage
            .get(
                ACCOUNT_STORAGE_STORAGE,
                Bytes.concatenate(accountHash, storageSlotKey.getSlotHash()).toArrayUnsafe())
            .map(Bytes::wrap);
    if (response.isEmpty()) {
      final Optional<Hash> storageRoot = storageRootSupplier.get();
      final Optional<Bytes> worldStateRootHash = worldStateRootHashSupplier.get();
      if (storageRoot.isPresent() && worldStateRootHash.isPresent()) {
        response =
            new StoredMerklePatriciaTrie<>(
                    new StoredNodeFactory<>(nodeLoader, Function.identity(), Function.identity()),
                    storageRoot.get())
                .get(storageSlotKey.getSlotHash())
                .map(bytes -> Bytes32.leftPad(RLP.decodeValue(bytes)));
        if (response.isEmpty()) getStorageValueMissingMerkleTrieCounter.inc();
        else getStorageValueMerkleTrieCounter.inc();
      }
    } else {
      getStorageValueFlatDatabaseCounter.inc();
    }
    return response;
  }
}
