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
package org.idnecology.idn.ethereum.core;

import static org.idnecology.idn.config.JsonUtil.normalizeKeys;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.JsonGenesisConfigOptions;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ProtocolScheduleFixture {
  public static final ProtocolSchedule MAINNET =
      MainnetProtocolSchedule.fromConfig(
          getMainnetConfigOptions(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          MiningConfiguration.newDefault(),
          new BadBlockManager(),
          false,
          new NoOpMetricsSystem());

  private static GenesisConfigOptions getMainnetConfigOptions() {
    return getGenesisConfigOptions("/mainnet.json");
  }

  public static GenesisConfigOptions getGenesisConfigOptions(final String genesisConfig) {
    // this method avoids reading all the alloc accounts when all we want is the "config" section
    try (final JsonParser jsonParser =
        new JsonFactory().createParser(GenesisConfig.class.getResource(genesisConfig))) {

      while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
        if ("config".equals(jsonParser.currentName())) {
          jsonParser.nextToken();
          return JsonGenesisConfigOptions.fromJsonObject(
              normalizeKeys(new ObjectMapper().readTree(jsonParser)));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed open or parse mainnet genesis json", e);
    }
    throw new IllegalArgumentException("mainnet json file had no config section");
  }
}
