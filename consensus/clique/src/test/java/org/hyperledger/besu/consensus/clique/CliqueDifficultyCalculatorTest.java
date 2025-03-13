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
package org.idnecology.idn.consensus.clique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Util;

import java.math.BigInteger;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CliqueDifficultyCalculatorTest {

  private final KeyPair proposerKeyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private Address localAddr;

  private final List<Address> validatorList = Lists.newArrayList();
  private BlockHeaderTestFixture blockHeaderBuilder;

  @BeforeEach
  public void setup() {
    localAddr = Util.publicKeyToAddress(proposerKeyPair.getPublicKey());

    validatorList.add(localAddr);
    validatorList.add(AddressHelpers.calculateAddressWithRespectTo(localAddr, 1));

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    CliqueHelpers.setCliqueContext(new CliqueContext(validatorProvider, null, null));
    blockHeaderBuilder = new BlockHeaderTestFixture();
  }

  @Test
  public void inTurnValidatorProducesDifficultyOfTwo() {
    final CliqueDifficultyCalculator calculator = new CliqueDifficultyCalculator(localAddr);

    final BlockHeader parentHeader = blockHeaderBuilder.number(1).buildHeader();

    assertThat(calculator.nextDifficulty(0, parentHeader)).isEqualTo(BigInteger.valueOf(2));
  }

  @Test
  public void outTurnValidatorProducesDifficultyOfOne() {
    final CliqueDifficultyCalculator calculator = new CliqueDifficultyCalculator(localAddr);

    final BlockHeader parentHeader = blockHeaderBuilder.number(2).buildHeader();

    assertThat(calculator.nextDifficulty(0, parentHeader)).isEqualTo(BigInteger.valueOf(1));
  }
}
