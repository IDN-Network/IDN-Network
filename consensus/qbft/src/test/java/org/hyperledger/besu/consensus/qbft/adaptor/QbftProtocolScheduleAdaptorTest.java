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

import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockImporter;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockValidator;
import org.idnecology.idn.consensus.qbft.core.types.QbftProtocolSchedule;
import org.idnecology.idn.ethereum.BlockValidator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QbftProtocolScheduleAdaptorTest {
  @Mock private ProtocolSchedule idnProtocolSchedule;
  @Mock private ProtocolSpec idnProtocolSpec;
  @Mock private ProtocolContext idnProtocolContext;

  @Test
  void createsBlockValidatorUsingIdnBlockValidator() {
    final BlockHeader idnHeader = new BlockHeaderTestFixture().number(1).buildHeader();
    final QbftBlockHeader qbftHeader = new QbftBlockHeaderAdaptor(idnHeader);
    final BlockValidator idnBlockValidator = Mockito.mock(BlockValidator.class);

    when(idnProtocolSchedule.getByBlockHeader(idnHeader)).thenReturn(idnProtocolSpec);
    when(idnProtocolSpec.getBlockValidator()).thenReturn(idnBlockValidator);

    final QbftProtocolSchedule qbftProtocolSchedule =
        new QbftProtocolScheduleAdaptor(idnProtocolSchedule, idnProtocolContext);
    final QbftBlockValidator qbftBlockValidator =
        qbftProtocolSchedule.getBlockValidator(qbftHeader);
    assertThat(qbftBlockValidator)
        .hasFieldOrPropertyWithValue("blockValidator", idnBlockValidator);
  }

  @Test
  void createsBlockImporterUsingIdnBlockImporter() {
    final BlockHeader idnHeader = new BlockHeaderTestFixture().number(1).buildHeader();
    final QbftBlockHeader qbftHeader = new QbftBlockHeaderAdaptor(idnHeader);
    final BlockImporter idnBlockImporter = Mockito.mock(BlockImporter.class);

    when(idnProtocolSchedule.getByBlockHeader(idnHeader)).thenReturn(idnProtocolSpec);
    when(idnProtocolSpec.getBlockImporter()).thenReturn(idnBlockImporter);

    final QbftProtocolSchedule qbftProtocolSchedule =
        new QbftProtocolScheduleAdaptor(idnProtocolSchedule, idnProtocolContext);
    final QbftBlockImporter qbftBlockImporter = qbftProtocolSchedule.getBlockImporter(qbftHeader);
    assertThat(qbftBlockImporter).hasFieldOrPropertyWithValue("blockImporter", idnBlockImporter);
  }
}
