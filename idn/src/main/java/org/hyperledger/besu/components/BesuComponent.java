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
package org.idnecology.idn.components;

import org.idnecology.idn.cli.IdnCommand;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.BlobCacheModule;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoader;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoaderModule;
import org.idnecology.idn.metrics.MetricsSystemModule;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.services.IdnPluginContextImpl;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import org.slf4j.Logger;

/** An application context that knows how to provide dependencies based on Dagger setup. */
@Singleton
@Component(
    modules = {
      IdnCommandModule.class,
      MetricsSystemModule.class,
      BonsaiCachedMerkleTrieLoaderModule.class,
      IdnPluginContextModule.class,
      BlobCacheModule.class
    })
public interface IdnComponent {

  /**
   * the configured and parsed representation of the user issued command to run Idn
   *
   * @return IdnCommand
   */
  IdnCommand getIdnCommand();

  /**
   * a cached trie node loader
   *
   * @return CachedMerkleTrieLoader
   */
  BonsaiCachedMerkleTrieLoader getCachedMerkleTrieLoader();

  /**
   * a metrics system that is observable by a Prometheus or OTEL metrics collection subsystem
   *
   * @return ObservableMetricsSystem
   */
  MetricsSystem getMetricsSystem();

  /**
   * a Logger specifically configured to provide configuration feedback to users.
   *
   * @return Logger
   */
  @Named("idnCommandLogger")
  Logger getIdnCommandLogger();

  /**
   * Idn plugin context for doing plugin service discovery.
   *
   * @return IdnPluginContextImpl
   */
  IdnPluginContextImpl getIdnPluginContext();

  /**
   * Cache to store blobs in for re-use after reorgs.
   *
   * @return BlobCache
   */
  BlobCache getBlobCache();
}
