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
package org.idnecology.idn.evmtool;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.BlockchainStorage;
import org.idnecology.idn.ethereum.chain.DefaultBlockchain;
import org.idnecology.idn.ethereum.chain.GenesisState;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.worldview.ForestMutableWorldState;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.worldstate.WorldUpdater;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import org.apache.tuweni.bytes.Bytes32;

/**
 * This class is a Dagger module that provides dependencies related to the blockchain. It includes
 * the GenesisFileModule and DataStoreModule for providing the genesis block and data store
 * respectively. The class is annotated with {@code @Module} to indicate that it is a Dagger module.
 */
@SuppressWarnings("WeakerAccess")
@Module(includes = {GenesisFileModule.class, DataStoreModule.class})
public class BlockchainModule {

  /** Default constructor for the BlockchainModule class. */
  public BlockchainModule() {}

  @Singleton
  @Provides
  Blockchain provideBlockchain(
      @Named("GenesisBlock") final Block genesisBlock,
      final BlockchainStorage blockchainStorage,
      final MetricsSystem metricsSystem) {
    return DefaultBlockchain.createMutable(genesisBlock, blockchainStorage, metricsSystem, 0);
  }

  @Provides
  @Singleton
  MutableWorldState getMutableWorldState(
      @Named("StateRoot") final Bytes32 stateRoot,
      final WorldStateStorageCoordinator worldStateStorageCoordinator,
      final WorldStatePreimageStorage worldStatePreimageStorage,
      final GenesisState genesisState,
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final EvmConfiguration evmConfiguration) {
    if ("memory".equals(keyValueStorageName)) {
      final MutableWorldState mutableWorldState =
          new ForestMutableWorldState(
              worldStateStorageCoordinator.worldStateKeyValueStorage(),
              worldStatePreimageStorage,
              evmConfiguration);
      genesisState.writeStateTo(mutableWorldState);
      return mutableWorldState;
    } else {
      return new ForestMutableWorldState(
          stateRoot,
          worldStateStorageCoordinator.worldStateKeyValueStorage(),
          worldStatePreimageStorage,
          evmConfiguration);
    }
  }

  @Provides
  @Singleton
  WorldStateStorageCoordinator provideWorldStateStorage(
      @Named("worldState") final KeyValueStorage keyValueStorage) {
    return new WorldStateStorageCoordinator(new ForestWorldStateKeyValueStorage(keyValueStorage));
  }

  @Provides
  @Singleton
  WorldStatePreimageStorage provideWorldStatePreimageStorage(
      @Named("worldStatePreimage") final KeyValueStorage keyValueStorage) {
    return new WorldStatePreimageKeyValueStorage(keyValueStorage);
  }

  @Provides
  @Singleton
  WorldUpdater provideWorldUpdater(final MutableWorldState mutableWorldState) {
    return mutableWorldState.updater();
  }

  @Provides
  @Named("StateRoot")
  @Singleton
  Bytes32 provideStateRoot(final BlockParameter blockParameter, final Blockchain blockchain) {
    if (blockParameter.isEarliest()) {
      return blockchain.getBlockHeader(0).orElseThrow().getStateRoot();
    } else if (blockParameter.isLatest() || blockParameter.isPending()) {
      return blockchain.getChainHeadHeader().getStateRoot();
    } else if (blockParameter.isNumeric()) {
      return blockchain
          .getBlockHeader(blockParameter.getNumber().orElseThrow())
          .orElseThrow()
          .getStateRoot();
    } else {
      return Hash.EMPTY_TRIE_HASH;
    }
  }
}
