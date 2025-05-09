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
package org.idnecology.idn.controller;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.JsonBftConfigOptions;
import org.idnecology.idn.config.TransitionsConfigOptions;
import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.consensus.common.bft.MutableBftConfigOptions;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IbftIdnControllerBuilderTest extends AbstractBftIdnControllerBuilderTest {

  @Override
  public void setupBftGenesisConfig() throws JsonProcessingException {

    // Ibft prepForBuild setup
    lenient()
        .when(genesisConfigOptions.getBftConfigOptions())
        .thenReturn(new MutableBftConfigOptions(JsonBftConfigOptions.DEFAULT));

    final var jsonTransitions =
        (ObjectNode)
            objectMapper.readTree(
                """
                                {"ibft2": [
                                  {
                                            "block": 2,
                                            "blockperiodseconds": 2
                                  }
                                ]}
                                """);

    lenient()
        .when(genesisConfigOptions.getTransitions())
        .thenReturn(new TransitionsConfigOptions(jsonTransitions));

    when(genesisConfig.getExtraData())
        .thenReturn(
            "0xf83ea00000000000000000000000000000000000000000000000000000000000000000d594c2ab482b506de561668e07f04547232a72897daf808400000000c0");
  }

  @Override
  protected IdnControllerBuilder createBftControllerBuilder() {
    return new IbftIdnControllerBuilder();
  }

  @Override
  protected BlockHeaderFunctions getBlockHeaderFunctions() {
    return BftBlockHeaderFunctions.forOnchainBlock(new IbftExtraDataCodec());
  }
}
