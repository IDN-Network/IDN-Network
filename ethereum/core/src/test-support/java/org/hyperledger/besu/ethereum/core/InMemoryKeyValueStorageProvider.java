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
package org.idnecology.idn.ethereum.core;

import static org.idnecology.idn.ethereum.core.WorldStateHealerHelper.throwingWorldStateHealerSupplier;

import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.DefaultBlockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.chain.VariablesStorage;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateKeyValueStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.idnecology.idn.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.BonsaiWorldStateProvider;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoader;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.ForestWorldStateArchive;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.worldview.ForestMutableWorldState;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;
import org.idnecology.idn.services.kvstore.SegmentedInMemoryKeyValueStorage;

import java.util.Optional;

public class InMemoryKeyValueStorageProvider extends KeyValueStorageProvider {

  public InMemoryKeyValueStorageProvider() {
    super(
        segmentIdentifiers -> new SegmentedInMemoryKeyValueStorage(),
        new InMemoryKeyValueStorage(),
        new NoOpMetricsSystem());
  }

  public static MutableBlockchain createInMemoryBlockchain(final Block genesisBlock) {
    return createInMemoryBlockchain(genesisBlock, createInMemoryVariablesStorage());
  }

  public static MutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock, final VariablesStorage variablesStorage) {
    return createInMemoryBlockchain(
        genesisBlock, new MainnetBlockHeaderFunctions(), variablesStorage);
  }

  public static MutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock, final BlockHeaderFunctions blockHeaderFunctions) {
    return createInMemoryBlockchain(
        genesisBlock, blockHeaderFunctions, createInMemoryVariablesStorage());
  }

  public static MutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock,
      final BlockHeaderFunctions blockHeaderFunctions,
      final VariablesStorage variablesStorage) {
    final InMemoryKeyValueStorage keyValueStorage = new InMemoryKeyValueStorage();
    return DefaultBlockchain.createMutable(
        genesisBlock,
        new KeyValueStoragePrefixedKeyBlockchainStorage(
            keyValueStorage, variablesStorage, blockHeaderFunctions, false),
        new NoOpMetricsSystem(),
        0);
  }

  public static ForestWorldStateArchive createInMemoryWorldStateArchive() {
    return new ForestWorldStateArchive(
        new WorldStateStorageCoordinator(
            new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage())),
        new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage()),
        EvmConfiguration.DEFAULT);
  }

  public static BonsaiWorldStateProvider createBonsaiInMemoryWorldStateArchive(
      final Blockchain blockchain) {
    return createBonsaiInMemoryWorldStateArchive(blockchain, EvmConfiguration.DEFAULT);
  }

  public static BonsaiWorldStateProvider createBonsaiInMemoryWorldStateArchive(
      final Blockchain blockchain, final EvmConfiguration evmConfiguration) {
    final InMemoryKeyValueStorageProvider inMemoryKeyValueStorageProvider =
        new InMemoryKeyValueStorageProvider();
    final BonsaiCachedMerkleTrieLoader bonsaiCachedMerkleTrieLoader =
        new BonsaiCachedMerkleTrieLoader(new NoOpMetricsSystem());
    return new BonsaiWorldStateProvider(
        (BonsaiWorldStateKeyValueStorage)
            inMemoryKeyValueStorageProvider.createWorldStateStorage(
                DataStorageConfiguration.DEFAULT_BONSAI_CONFIG),
        blockchain,
        Optional.empty(),
        bonsaiCachedMerkleTrieLoader,
        null,
        evmConfiguration,
        throwingWorldStateHealerSupplier());
  }

  public static MutableWorldState createInMemoryWorldState() {
    final InMemoryKeyValueStorageProvider provider = new InMemoryKeyValueStorageProvider();
    return new ForestMutableWorldState(
        provider.createWorldStateStorage(DataStorageConfiguration.DEFAULT_FOREST_CONFIG),
        provider.createWorldStatePreimageStorage(),
        EvmConfiguration.DEFAULT);
  }

  public static PrivateStateStorage createInMemoryPrivateStateStorage() {
    return new PrivateStateKeyValueStorage(new InMemoryKeyValueStorage());
  }

  public static VariablesStorage createInMemoryVariablesStorage() {
    return new VariablesKeyValueStorage(new InMemoryKeyValueStorage());
  }
}
