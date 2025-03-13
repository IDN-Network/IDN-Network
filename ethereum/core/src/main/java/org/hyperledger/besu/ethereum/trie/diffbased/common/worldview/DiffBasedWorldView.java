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
package org.idnecology.idn.ethereum.trie.diffbased.common.worldview;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.StorageSlotKey;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.trie.diffbased.common.storage.DiffBasedWorldStateKeyValueStorage;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.evm.worldstate.WorldView;

import java.util.Map;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public interface DiffBasedWorldView extends WorldView {

  Optional<Bytes> getCode(Address address, final Hash codeHash);

  UInt256 getStorageValue(Address address, UInt256 key);

  Optional<UInt256> getStorageValueByStorageSlotKey(Address address, StorageSlotKey storageSlotKey);

  UInt256 getPriorStorageValue(Address address, UInt256 key);

  /**
   * Retrieve all the storage values of an account.
   *
   * @param address the account to stream
   * @param rootHash the root hash of the account storage trie
   * @return A map that is a copy of the entries. The key is the hashed slot number, and the value
   *     is the Bytes representation of the storage value.
   */
  Map<Bytes32, Bytes> getAllAccountStorage(final Address address, final Hash rootHash);

  static Bytes encodeTrieValue(final Bytes bytes) {
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.writeBytes(bytes.trimLeadingZeros());
    return out.encoded();
  }

  boolean isModifyingHeadWorldState();

  DiffBasedWorldStateKeyValueStorage getWorldStateStorage();

  WorldUpdater updater();
}
