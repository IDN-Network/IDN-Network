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
package org.idnecology.idn.ethereum.components;

import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSpecs;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecBuilder;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.plugin.services.MetricsSystem;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/** Provides protocol specs for network forks. */
@Module
public class ProtocolSpecModule {

  /** Default constructor. */
  public ProtocolSpecModule() {}

  /**
   * Provides the protocol spec for the frontier network fork.
   *
   * @param evmConfiguration the EVM configuration
   * @param isParalleltxEnabled whether parallel tx processing is enabled
   * @param metricsSystem the metrics system
   * @return the protocol spec for the frontier network fork
   */
  @Provides
  @Named("frontier")
  public ProtocolSpecBuilder frontierProtocolSpec(
      final EvmConfiguration evmConfiguration,
      final boolean isParalleltxEnabled,
      final MetricsSystem metricsSystem) {
    return MainnetProtocolSpecs.frontierDefinition(
        evmConfiguration, isParalleltxEnabled, metricsSystem);
  }
}
