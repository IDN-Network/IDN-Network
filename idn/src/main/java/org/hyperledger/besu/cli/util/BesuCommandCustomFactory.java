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
package org.idnecology.idn.cli.util;

import org.idnecology.idn.plugin.services.PluginVersionsProvider;

import picocli.CommandLine;

/**
 * Custom PicoCli IFactory to handle version provider construction with plugin versions. Based on
 * same logic as PicoCLI DefaultFactory.
 */
public class IdnCommandCustomFactory implements CommandLine.IFactory {
  private final PluginVersionsProvider pluginVersionsProvider;
  private final CommandLine.IFactory defaultFactory = CommandLine.defaultFactory();

  /**
   * IdnCommandCustomFactory Constructor
   *
   * @param pluginVersionsProvider instance of PluginVersionsProvider
   */
  public IdnCommandCustomFactory(final PluginVersionsProvider pluginVersionsProvider) {
    this.pluginVersionsProvider = pluginVersionsProvider;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(final Class<T> cls) throws Exception {
    if (CommandLine.IVersionProvider.class.isAssignableFrom(cls)) {
      return (T) new VersionProvider(pluginVersionsProvider);
    }

    return defaultFactory.create(cls);
  }
}
