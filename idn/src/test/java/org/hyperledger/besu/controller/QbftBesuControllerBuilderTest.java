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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.JsonQbftConfigOptions;
import org.idnecology.idn.config.TransitionsConfigOptions;
import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.consensus.common.bft.BftContext;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.qbft.MutableQbftConfigOptions;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.validator.ForkingValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class QbftIdnControllerBuilderTest extends AbstractBftIdnControllerBuilderTest {

  @Override
  public void setupBftGenesisConfig() throws JsonProcessingException {

    // qbft prepForBuild setup
    lenient()
        .when(genesisConfigOptions.getQbftConfigOptions())
        .thenReturn(new MutableQbftConfigOptions(JsonQbftConfigOptions.DEFAULT));

    final var jsonTransitions =
        (ObjectNode)
            objectMapper.readTree(
                """
                                {"qbft": [
                                  {
                                            "block": 2,
                                            "blockperiodseconds": 2
                                  }
                                ]}
                                """);

    lenient()
        .when(genesisConfigOptions.getTransitions())
        .thenReturn(new TransitionsConfigOptions(jsonTransitions));

    lenient()
        .when(genesisConfig.getExtraData())
        .thenReturn(
            QbftExtraDataCodec.createGenesisExtraDataString(List.of(Address.fromHexString("1"))));
  }

  @Override
  protected IdnControllerBuilder createBftControllerBuilder() {
    return new QbftIdnControllerBuilder();
  }

  @Override
  protected BlockHeaderFunctions getBlockHeaderFunctions() {
    return BftBlockHeaderFunctions.forOnchainBlock(new QbftExtraDataCodec());
  }

  @Test
  public void forkingValidatorProviderIsAvailableOnBftContext() {
    final IdnController idnController = bftIdnControllerBuilder.build();

    final ValidatorProvider validatorProvider =
        idnController
            .getProtocolContext()
            .getConsensusContext(BftContext.class)
            .getValidatorProvider();
    assertThat(validatorProvider).isInstanceOf(ForkingValidatorProvider.class);
  }

  @Test
  public void missingTransactionValidatorProviderThrowsError() {
    final ProtocolContext protocolContext = mock(ProtocolContext.class);
    final ProtocolSchedule protocolSchedule = mock(ProtocolSchedule.class);
    when(protocolContext.getBlockchain()).thenReturn(mock(MutableBlockchain.class));

    assertThatThrownBy(
            () ->
                bftIdnControllerBuilder.createAdditionalJsonRpcMethodFactory(
                    protocolContext, protocolSchedule, MiningConfiguration.newDefault()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("transactionValidatorProvider should have been initialised");
  }
}
