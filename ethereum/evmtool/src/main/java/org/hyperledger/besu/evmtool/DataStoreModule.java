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

import org.idnecology.idn.ethereum.chain.BlockchainStorage;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.idnecology.idn.plugin.services.IdnConfiguration;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;
import org.idnecology.idn.plugin.services.storage.SegmentIdentifier;
import org.idnecology.idn.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.idnecology.idn.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.idnecology.idn.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;
import org.idnecology.idn.services.kvstore.LimitedInMemoryKeyValueStorage;

import java.util.List;
import java.util.function.Supplier;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Suppliers;
import dagger.Module;
import dagger.Provides;

/**
 * This class is a Dagger module that provides dependencies related to the data store. It includes
 * the GenesisFileModule for providing the genesis block. The class is annotated with
 * {@code @Module} to indicate that it is a Dagger module. It provides various key-value storages
 * such as variables, blockchain, world state, world state preimage, and pruning. The type of
 * key-value storage (e.g., rocksdb, memory) can be specified. The class also provides a
 * BlockchainStorage which is a prefixed key blockchain storage.
 */
@SuppressWarnings({"CloseableProvides"})
@Module(includes = GenesisFileModule.class)
public class DataStoreModule {

  private final Supplier<RocksDBKeyValueStorageFactory> rocksDBFactory =
      Suppliers.memoize(
          () ->
              new RocksDBKeyValueStorageFactory(
                  RocksDBCLIOptions.create()::toDomainObject,
                  List.of(KeyValueSegmentIdentifier.values()),
                  RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS));

  /** Default constructor for the DataStoreModule class. */
  public DataStoreModule() {}

  @Provides
  @Singleton
  @Named("variables")
  KeyValueStorage provideVariablesKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final IdnConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.VARIABLES);
  }

  @Provides
  @Singleton
  @Named("blockchain")
  KeyValueStorage provideBlockchainKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final IdnConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.BLOCKCHAIN);
  }

  @Provides
  @Singleton
  @Named("worldState")
  KeyValueStorage provideWorldStateKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final IdnConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.WORLD_STATE);
  }

  @Provides
  @Singleton
  @Named("worldStatePreimage")
  @SuppressWarnings("UnusedVariable")
  KeyValueStorage provideWorldStatePreimageKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final IdnConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return new LimitedInMemoryKeyValueStorage(5000);
  }

  @Provides
  @Singleton
  @Named("pruning")
  KeyValueStorage providePruningKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final IdnConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.PRUNING_STATE);
  }

  private KeyValueStorage constructKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final IdnConfiguration commonConfiguration,
      final MetricsSystem metricsSystem,
      final SegmentIdentifier segment) {

    switch (keyValueStorageName) {
      case "rocksdb":
        return rocksDBFactory.get().create(segment, commonConfiguration, metricsSystem);
      default:
        System.err.println("Unknown key, continuing as though 'memory' was specified");
      // fall through
      case "memory":
        return new InMemoryKeyValueStorage();
    }
  }

  @Provides
  @Singleton
  static BlockchainStorage provideBlockchainStorage(
      @Named("blockchain") final KeyValueStorage keyValueStorage,
      @Named("variables") final KeyValueStorage variablesKeyValueStorage,
      final BlockHeaderFunctions blockHashFunction) {
    return new KeyValueStoragePrefixedKeyBlockchainStorage(
        keyValueStorage,
        new VariablesKeyValueStorage(variablesKeyValueStorage),
        blockHashFunction,
        false);
  }
}
