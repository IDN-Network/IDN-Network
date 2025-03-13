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
package org.idnecology.idn.consensus.qbft.blockcreation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.JsonQbftConfigOptions;
import org.idnecology.idn.config.QbftConfigOptions;
import org.idnecology.idn.consensus.common.ForkSpec;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.qbft.MutableQbftConfigOptions;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QbftBlockCreatorFactoryTest {
  private final QbftExtraDataCodec extraDataCodec = new QbftExtraDataCodec();
  private QbftBlockCreatorFactory qbftBlockCreatorFactory;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setUp() {
    final MiningConfiguration miningParams =
        ImmutableMiningConfiguration.builder()
            .mutableInitValues(
                MutableInitValues.builder()
                    .extraData(Bytes.wrap("Qbft tests".getBytes(UTF_8)))
                    .build())
            .build();

    final MutableQbftConfigOptions qbftConfigOptions =
        new MutableQbftConfigOptions(JsonQbftConfigOptions.DEFAULT);
    qbftConfigOptions.setValidatorContractAddress(Optional.of("1"));
    final ForkSpec<QbftConfigOptions> spec = new ForkSpec<>(0, qbftConfigOptions);
    final ForksSchedule<QbftConfigOptions> forksSchedule = mock(ForksSchedule.class);
    when(forksSchedule.getFork(anyLong())).thenReturn(spec);

    qbftBlockCreatorFactory =
        new QbftBlockCreatorFactory(
            mock(TransactionPool.class),
            mock(ProtocolContext.class),
            mock(ProtocolSchedule.class),
            forksSchedule,
            miningParams,
            mock(Address.class),
            extraDataCodec,
            new DeterministicEthScheduler());
  }

  @Test
  public void contractValidatorModeCreatesExtraDataWithoutValidatorsAndVote() {
    final BlockHeader parentHeader = mock(BlockHeader.class);
    when(parentHeader.getNumber()).thenReturn(1L);

    final Bytes encodedExtraData = qbftBlockCreatorFactory.createExtraData(3, parentHeader);
    final BftExtraData bftExtraData = extraDataCodec.decodeRaw(encodedExtraData);

    assertThat(bftExtraData.getValidators()).isEmpty();
    assertThat(bftExtraData.getVote()).isEmpty();
    assertThat(bftExtraData.getRound()).isEqualTo(3);
  }
}
