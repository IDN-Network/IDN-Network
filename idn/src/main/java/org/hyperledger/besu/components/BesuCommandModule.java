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

import org.idnecology.idn.Idn;
import org.idnecology.idn.RunnerBuilder;
import org.idnecology.idn.chainexport.RlpBlockExporter;
import org.idnecology.idn.chainimport.Era1BlockImporter;
import org.idnecology.idn.chainimport.JsonBlockImporter;
import org.idnecology.idn.chainimport.RlpBlockImporter;
import org.idnecology.idn.cli.IdnCommand;
import org.idnecology.idn.cli.options.P2PDiscoveryOptions;
import org.idnecology.idn.cli.options.RPCOptions;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.ethereum.p2p.discovery.P2PDiscoveryConfiguration;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.services.IdnPluginContextImpl;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;

/**
 * A dagger module that know how to create the IdnCommand, which collects all configuration
 * settings.
 */
@Module
public class IdnCommandModule {
  /** Default constructor. */
  public IdnCommandModule() {}

  @Provides
  @Singleton
  IdnCommand provideIdnCommand(final @Named("idnCommandLogger") Logger commandLogger) {
    final IdnCommand idnCommand =
        new IdnCommand(
            RlpBlockImporter::new,
            JsonBlockImporter::new,
            Era1BlockImporter::new,
            RlpBlockExporter::new,
            new RunnerBuilder(),
            new IdnController.Builder(),
            new IdnPluginContextImpl(),
            System.getenv(),
            commandLogger);
    return idnCommand;
  }

  @Provides
  @Singleton
  MetricsConfiguration provideMetricsConfiguration(final IdnCommand provideFrom) {
    return provideFrom.metricsConfiguration();
  }

  @Provides
  @Singleton
  RPCOptions provideRPCOptions() {
    return RPCOptions.create();
  }

  @Provides
  @Singleton
  P2PDiscoveryConfiguration provideP2PDiscoveryConfiguration() {
    return new P2PDiscoveryOptions().toDomainObject();
  }

  @Provides
  @Named("idnCommandLogger")
  @Singleton
  Logger provideIdnCommandLogger() {
    return Idn.getFirstLogger();
  }

  @Provides
  @Singleton
  IdnPluginContextImpl provideIdnPluginContextImpl(final IdnCommand provideFrom) {
    return provideFrom.getIdnPluginContext();
  }
}
