/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.trie.diffbased.common.storage.flat;

import static org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.CODE_STORAGE;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorageTransaction;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class AccountHashCodeStorageStrategy implements CodeStorageStrategy {
  @Override
  public Optional<Bytes> getFlatCode(
      final Hash codeHash, final Hash accountHash, final SegmentedKeyValueStorage storage) {
    return storage
        .get(CODE_STORAGE, accountHash.toArrayUnsafe())
        .map(Bytes::wrap)
        .filter(b -> Hash.hash(b).equals(codeHash));
  }

  @Override
  public void putFlatCode(
      final SegmentedKeyValueStorageTransaction transaction,
      final Hash accountHash,
      final Hash codeHash,
      final Bytes code) {
    transaction.put(CODE_STORAGE, accountHash.toArrayUnsafe(), code.toArrayUnsafe());
  }

  @Override
  public void removeFlatCode(
      final SegmentedKeyValueStorageTransaction transaction,
      final Hash accountHash,
      final Hash codeHash) {
    transaction.remove(CODE_STORAGE, accountHash.toArrayUnsafe());
  }
}
