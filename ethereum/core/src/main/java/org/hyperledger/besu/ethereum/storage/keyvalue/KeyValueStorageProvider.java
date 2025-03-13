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
package org.idnecology.idn.ethereum.storage.keyvalue;

import org.idnecology.idn.ethereum.chain.BlockchainStorage;
import org.idnecology.idn.ethereum.chain.VariablesStorage;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;
import org.idnecology.idn.plugin.services.storage.SegmentIdentifier;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;
import org.idnecology.idn.services.kvstore.SegmentedKeyValueStorageAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyValueStorageProvider implements StorageProvider {
  private static final Logger LOG = LoggerFactory.getLogger(StorageProvider.class);

  protected final Function<List<SegmentIdentifier>, SegmentedKeyValueStorage>
      segmentedStorageCreator;
  private final KeyValueStorage worldStatePreimageStorage;
  protected final Map<List<SegmentIdentifier>, SegmentedKeyValueStorage> storageInstances =
      new HashMap<>();
  private final ObservableMetricsSystem metricsSystem;

  public KeyValueStorageProvider(
      final Function<List<SegmentIdentifier>, SegmentedKeyValueStorage> segmentedStorageCreator,
      final KeyValueStorage worldStatePreimageStorage,
      final ObservableMetricsSystem metricsSystem) {
    this.segmentedStorageCreator = segmentedStorageCreator;
    this.worldStatePreimageStorage = worldStatePreimageStorage;
    this.metricsSystem = metricsSystem;
  }

  @Override
  public VariablesStorage createVariablesStorage() {
    return new VariablesKeyValueStorage(
        getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.VARIABLES));
  }

  @Override
  public BlockchainStorage createBlockchainStorage(
      final ProtocolSchedule protocolSchedule,
      final VariablesStorage variablesStorage,
      final DataStorageConfiguration dataStorageConfiguration) {
    return new KeyValueStoragePrefixedKeyBlockchainStorage(
        getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.BLOCKCHAIN),
        variablesStorage,
        ScheduleBasedBlockHeaderFunctions.create(protocolSchedule),
        dataStorageConfiguration.getReceiptCompactionEnabled());
  }

  @Override
  public WorldStateKeyValueStorage createWorldStateStorage(
      final DataStorageConfiguration dataStorageConfiguration) {
    if (dataStorageConfiguration.getDataStorageFormat().equals(DataStorageFormat.BONSAI)) {
      return new BonsaiWorldStateKeyValueStorage(this, metricsSystem, dataStorageConfiguration);
    } else {
      return new ForestWorldStateKeyValueStorage(
          getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE));
    }
  }

  @Override
  public WorldStateStorageCoordinator createWorldStateStorageCoordinator(
      final DataStorageConfiguration dataStorageFormatConfiguration) {
    return new WorldStateStorageCoordinator(
        createWorldStateStorage(dataStorageFormatConfiguration));
  }

  @Override
  public WorldStatePreimageStorage createWorldStatePreimageStorage() {
    return new WorldStatePreimageKeyValueStorage(worldStatePreimageStorage);
  }

  @Override
  public KeyValueStorage getStorageBySegmentIdentifier(final SegmentIdentifier segment) {
    return new SegmentedKeyValueStorageAdapter(
        segment, storageInstances.computeIfAbsent(List.of(segment), segmentedStorageCreator));
  }

  @Override
  public SegmentedKeyValueStorage getStorageBySegmentIdentifiers(
      final List<SegmentIdentifier> segments) {
    return segmentedStorageCreator.apply(segments);
  }

  @Override
  public void close() throws IOException {
    storageInstances.entrySet().stream()
        .filter(storage -> storage instanceof AutoCloseable)
        .forEach(
            storage -> {
              try {
                storage.getValue().close();
              } catch (final IOException e) {
                LOG.atWarn()
                    .setMessage("Failed to close storage instance {}")
                    .addArgument(
                        storage.getKey().stream()
                            .map(SegmentIdentifier::getName)
                            .collect(Collectors.joining(",")))
                    .setCause(e)
                    .log();
              }
            });
  }
}
