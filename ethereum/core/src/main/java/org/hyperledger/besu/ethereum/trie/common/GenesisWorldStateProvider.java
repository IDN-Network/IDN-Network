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
package org.idnecology.idn.ethereum.trie.common;

import static org.idnecology.idn.ethereum.trie.diffbased.common.worldview.WorldStateConfig.createStatefulConfigWithTrie;

import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.idnecology.idn.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoader;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.NoOpBonsaiCachedWorldStorageManager;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.worldview.BonsaiWorldState;
import org.idnecology.idn.ethereum.trie.diffbased.common.trielog.NoOpTrieLogManager;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.worldview.ForestMutableWorldState;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;
import org.idnecology.idn.services.kvstore.SegmentedInMemoryKeyValueStorage;

import java.util.Objects;

public class GenesisWorldStateProvider {

  /**
   * Creates a Genesis world state based on the provided data storage format.
   *
   * @param dataStorageConfiguration the data storage configuration to use
   * @return a mutable world state for the Genesis block
   */
  public static MutableWorldState createGenesisWorldState(
      final DataStorageConfiguration dataStorageConfiguration) {
    if (Objects.requireNonNull(dataStorageConfiguration).getDataStorageFormat()
        == DataStorageFormat.BONSAI) {
      return createGenesisBonsaiWorldState();
    } else {
      return createGenesisForestWorldState();
    }
  }

  /**
   * Creates a Genesis world state using the Bonsai data storage format.
   *
   * @return a mutable world state for the Genesis block
   */
  private static MutableWorldState createGenesisBonsaiWorldState() {
    final BonsaiCachedMerkleTrieLoader bonsaiCachedMerkleTrieLoader =
        new BonsaiCachedMerkleTrieLoader(new NoOpMetricsSystem());
    final BonsaiWorldStateKeyValueStorage bonsaiWorldStateKeyValueStorage =
        new BonsaiWorldStateKeyValueStorage(
            new KeyValueStorageProvider(
                segmentIdentifiers -> new SegmentedInMemoryKeyValueStorage(),
                new InMemoryKeyValueStorage(),
                new NoOpMetricsSystem()),
            new NoOpMetricsSystem(),
            DataStorageConfiguration.DEFAULT_BONSAI_CONFIG);
    return new BonsaiWorldState(
        bonsaiWorldStateKeyValueStorage,
        bonsaiCachedMerkleTrieLoader,
        new NoOpBonsaiCachedWorldStorageManager(bonsaiWorldStateKeyValueStorage),
        new NoOpTrieLogManager(),
        EvmConfiguration.DEFAULT,
        createStatefulConfigWithTrie());
  }

  /**
   * Creates a Genesis world state using the Forest data storage format.
   *
   * @return a mutable world state for the Genesis block
   */
  private static MutableWorldState createGenesisForestWorldState() {
    final ForestWorldStateKeyValueStorage stateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
    final WorldStatePreimageKeyValueStorage preimageStorage =
        new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage());
    return new ForestMutableWorldState(stateStorage, preimageStorage, EvmConfiguration.DEFAULT);
  }
}
