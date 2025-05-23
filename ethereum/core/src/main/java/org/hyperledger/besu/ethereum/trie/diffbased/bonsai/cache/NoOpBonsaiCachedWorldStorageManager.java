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
package org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.DiffBasedWorldState;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.WorldStateConfig;

import java.util.Optional;
import java.util.function.Function;

public class NoOpBonsaiCachedWorldStorageManager extends BonsaiCachedWorldStorageManager {

  public NoOpBonsaiCachedWorldStorageManager(
      final BonsaiWorldStateKeyValueStorage bonsaiWorldStateKeyValueStorage) {
    super(null, bonsaiWorldStateKeyValueStorage, WorldStateConfig.createStatefulConfigWithTrie());
  }

  @Override
  public synchronized void addCachedLayer(
      final BlockHeader blockHeader,
      final Hash worldStateRootHash,
      final DiffBasedWorldState forWorldState) {
    // no cache
  }

  @Override
  public boolean contains(final Hash blockHash) {
    return false;
  }

  @Override
  public Optional<DiffBasedWorldState> getWorldState(final Hash blockHash) {
    return Optional.empty();
  }

  @Override
  public Optional<DiffBasedWorldState> getNearestWorldState(final BlockHeader blockHeader) {
    return Optional.empty();
  }

  @Override
  public Optional<DiffBasedWorldState> getHeadWorldState(
      final Function<Hash, Optional<BlockHeader>> hashBlockHeaderFunction) {
    return Optional.empty();
  }

  @Override
  public void reset() {
    // world states are not re-used
  }
}
