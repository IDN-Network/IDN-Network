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
package org.idnecology.idn.components;

import static org.mockito.Mockito.mock;

import org.idnecology.idn.cli.IdnCommand;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.plugin.services.IdnConfiguration;
import org.idnecology.idn.services.IdnConfigurationImpl;
import org.idnecology.idn.services.IdnPluginContextImpl;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public class MockIdnCommandModule {

  @Provides
  IdnCommand provideIdnCommand() {
    return mock(IdnCommand.class);
  }

  @Provides
  @Singleton
  MetricsConfiguration provideMetricsConfiguration() {
    return MetricsConfiguration.builder().build();
  }

  @Provides
  @Named("idnCommandLogger")
  @Singleton
  Logger provideIdnCommandLogger() {
    return LoggerFactory.getLogger(MockIdnCommandModule.class);
  }

  /**
   * Creates a IdnPluginContextImpl, used for plugin service discovery.
   *
   * @return the IdnPluginContext
   */
  @Provides
  @Singleton
  public IdnPluginContextImpl provideIdnPluginContext() {
    IdnPluginContextImpl retval = new IdnPluginContextImpl();
    retval.addService(IdnConfiguration.class, new IdnConfigurationImpl());
    return retval;
  }
}
