/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.consensus.qbft.adaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QbftValidatorProviderAdaptorTest {
  @Mock private ValidatorProvider validatorProvider;

  @Test
  void returnsValidatorsAtHead() {
    List<Address> validatorsAtHead =
        List.of(Address.fromHexString("0x095e7baea6a6c7c4c2dfeb977efac326af552d87"));
    when(validatorProvider.getValidatorsAtHead()).thenReturn(validatorsAtHead);

    QbftValidatorProviderAdaptor qbftValidatorProviderAdaptor =
        new QbftValidatorProviderAdaptor(validatorProvider);
    assertThat(qbftValidatorProviderAdaptor.getValidatorsAtHead()).isEqualTo(validatorsAtHead);
  }

  @Test
  void returnsValidatorsAfterBlock() {
    BlockHeader header = new BlockHeaderTestFixture().buildHeader();
    QbftBlockHeader qbftBlockHeader = new QbftBlockHeaderAdaptor(header);
    List<Address> validatorsAfterBlock =
        List.of(Address.fromHexString("0x095e7baea6a6c7c4c2dfeb977efac326af552d87"));
    when(validatorProvider.getValidatorsAfterBlock(header)).thenReturn(validatorsAfterBlock);

    QbftValidatorProviderAdaptor qbftValidatorProviderAdaptor =
        new QbftValidatorProviderAdaptor(validatorProvider);
    assertThat(qbftValidatorProviderAdaptor.getValidatorsAfterBlock(qbftBlockHeader))
        .isEqualTo(validatorsAfterBlock);
  }

  @Test
  void returnsValidatorsForBlock() {
    BlockHeader header = new BlockHeaderTestFixture().buildHeader();
    QbftBlockHeader qbftBlockHeader = new QbftBlockHeaderAdaptor(header);
    List<Address> validatorsForBlock =
        List.of(Address.fromHexString("0x095e7baea6a6c7c4c2dfeb977efac326af552d87"));
    when(validatorProvider.getValidatorsForBlock(header)).thenReturn(validatorsForBlock);

    QbftValidatorProviderAdaptor qbftValidatorProviderAdaptor =
        new QbftValidatorProviderAdaptor(validatorProvider);
    assertThat(qbftValidatorProviderAdaptor.getValidatorsForBlock(qbftBlockHeader))
        .isEqualTo(validatorsForBlock);
  }
}
