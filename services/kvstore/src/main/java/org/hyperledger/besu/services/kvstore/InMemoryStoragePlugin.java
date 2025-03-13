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
package org.idnecology.idn.services.kvstore;

import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.ServiceManager;
import org.idnecology.idn.plugin.services.IdnConfiguration;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.StorageService;
import org.idnecology.idn.plugin.services.exception.StorageException;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;
import org.idnecology.idn.plugin.services.storage.KeyValueStorageFactory;
import org.idnecology.idn.plugin.services.storage.SegmentIdentifier;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The In memory storage plugin. */
public class InMemoryStoragePlugin implements IdnPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(InMemoryStoragePlugin.class);
  private ServiceManager context;
  private InMemoryKeyValueStorageFactory factory;
  private InMemoryKeyValueStorageFactory privacyFactory;

  /** Default constructor */
  public InMemoryStoragePlugin() {}

  @Override
  public void register(final ServiceManager context) {
    LOG.debug("Registering plugin");
    this.context = context;

    createFactoriesAndRegisterWithStorageService();

    LOG.debug("Plugin registered.");
  }

  @Override
  public void start() {
    LOG.debug("Starting plugin.");
    if (factory == null) {
      createFactoriesAndRegisterWithStorageService();
    }
  }

  @Override
  public void stop() {
    LOG.debug("Stopping plugin.");

    if (factory != null) {
      factory.close();
      factory = null;
    }

    if (privacyFactory != null) {
      privacyFactory.close();
      privacyFactory = null;
    }
  }

  private void createAndRegister(final StorageService service) {

    factory = new InMemoryKeyValueStorageFactory("memory");
    privacyFactory = new InMemoryKeyValueStorageFactory("memory-privacy");

    service.registerKeyValueStorage(factory);
    service.registerKeyValueStorage(privacyFactory);
  }

  private void createFactoriesAndRegisterWithStorageService() {
    context
        .getService(StorageService.class)
        .ifPresentOrElse(
            this::createAndRegister,
            () -> LOG.error("Failed to register KeyValueFactory due to missing StorageService."));
  }

  /** The Memory key value storage factory. */
  public static class InMemoryKeyValueStorageFactory implements KeyValueStorageFactory {

    private final String name;
    private final Map<List<SegmentIdentifier>, SegmentedInMemoryKeyValueStorage> storageMap =
        new HashMap<>();

    /**
     * Instantiates a new Memory key value storage factory.
     *
     * @param name the name
     */
    public InMemoryKeyValueStorageFactory(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public KeyValueStorage create(
        final SegmentIdentifier segment,
        final IdnConfiguration configuration,
        final MetricsSystem metricsSystem)
        throws StorageException {
      var kvStorage =
          storageMap.computeIfAbsent(
              List.of(segment), seg -> new SegmentedInMemoryKeyValueStorage(seg));
      return new SegmentedKeyValueStorageAdapter(segment, kvStorage);
    }

    @Override
    public SegmentedKeyValueStorage create(
        final List<SegmentIdentifier> segments,
        final IdnConfiguration configuration,
        final MetricsSystem metricsSystem)
        throws StorageException {
      var kvStorage =
          storageMap.computeIfAbsent(segments, __ -> new SegmentedInMemoryKeyValueStorage());
      return kvStorage;
    }

    @Override
    public boolean isSegmentIsolationSupported() {
      return true;
    }

    @Override
    public boolean isSnapshotIsolationSupported() {
      return true;
    }

    @Override
    public void close() {
      storageMap.clear();
    }
  }
}
