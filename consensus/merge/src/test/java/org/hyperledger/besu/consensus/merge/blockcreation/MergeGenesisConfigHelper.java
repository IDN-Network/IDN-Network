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
package org.idnecology.idn.consensus.merge.blockcreation;

import org.idnecology.idn.config.GenesisAccount;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.consensus.merge.MergeProtocolSchedule;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

public interface MergeGenesisConfigHelper {

  default GenesisConfig getPosGenesisConfig() {
    try {
      final URI uri = MergeGenesisConfigHelper.class.getResource("/posAtGenesis.json").toURI();
      return GenesisConfig.fromSource(uri.toURL());
    } catch (final URISyntaxException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  default GenesisConfig getPowGenesisConfig() {
    try {
      final URI uri = MergeGenesisConfigHelper.class.getResource("/powAtGenesis.json").toURI();
      return GenesisConfig.fromSource(uri.toURL());
    } catch (final URISyntaxException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  default Stream<Address> genesisAllocations(final GenesisConfig configFile) {
    return configFile.streamAllocations().map(GenesisAccount::address);
  }

  default ProtocolSchedule getMergeProtocolSchedule() {
    return MergeProtocolSchedule.create(
        getPosGenesisConfig().getConfigOptions(),
        false,
        MiningConfiguration.MINING_DISABLED,
        new BadBlockManager(),
        false,
        new NoOpMetricsSystem());
  }
}
