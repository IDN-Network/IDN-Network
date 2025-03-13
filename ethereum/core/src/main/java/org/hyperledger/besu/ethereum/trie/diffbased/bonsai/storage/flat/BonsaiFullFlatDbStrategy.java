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
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;

public class BonsaiFullFlatDbStrategy extends BonsaiFlatDbStrategy {

  protected final Counter getAccountNotFoundInFlatDatabaseCounter;

  protected final Counter getStorageValueNotFoundInFlatDatabaseCounter;

  public BonsaiFullFlatDbStrategy(
      final MetricsSystem metricsSystem, final CodeStorageStrategy codeStorageStrategy) {
    super(metricsSystem, codeStorageStrategy);

    getAccountNotFoundInFlatDatabaseCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "get_account_missing_flat_database",
            "Number of accounts not found in the flat database");

    getStorageValueNotFoundInFlatDatabaseCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.BLOCKCHAIN,
            "get_storagevalue_missing_flat_database",
            "Number of storage slots not found in the flat database");
  }

  @Override
  public Optional<Bytes> getFlatAccount(
      final Supplier<Optional<Bytes>> worldStateRootHashSupplier,
      final NodeLoader nodeLoader,
      final Hash accountHash,
      final SegmentedKeyValueStorage storage) {
    getAccountCounter.inc();
    final Optional<Bytes> accountFound =
        storage.get(ACCOUNT_INFO_STATE, accountHash.toArrayUnsafe()).map(Bytes::wrap);
    if (accountFound.isPresent()) {
      getAccountFoundInFlatDatabaseCounter.inc();
    } else {
      getAccountNotFoundInFlatDatabaseCounter.inc();
    }
    return accountFound;
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
    final Optional<Bytes> storageFound =
        storage
            .get(
                ACCOUNT_STORAGE_STORAGE,
                Bytes.concatenate(accountHash, storageSlotKey.getSlotHash()).toArrayUnsafe())
            .map(Bytes::wrap);
    if (storageFound.isPresent()) {
      getStorageValueFlatDatabaseCounter.inc();
    } else {
      getStorageValueNotFoundInFlatDatabaseCounter.inc();
    }

    return storageFound;
  }

  @Override
  public void resetOnResync(final SegmentedKeyValueStorage storage) {
    // NOOP
    // not need to reset anything in full mode
  }
}
