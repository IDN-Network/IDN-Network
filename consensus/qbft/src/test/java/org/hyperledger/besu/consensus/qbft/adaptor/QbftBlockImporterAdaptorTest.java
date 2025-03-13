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
import static org.idnecology.idn.ethereum.mainnet.BlockImportResult.BlockImportStatus.ALREADY_IMPORTED;
import static org.idnecology.idn.ethereum.mainnet.BlockImportResult.BlockImportStatus.IMPORTED;
import static org.idnecology.idn.ethereum.mainnet.BlockImportResult.BlockImportStatus.NOT_IMPORTED;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.mainnet.BlockImportResult;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QbftBlockImporterAdaptorTest {
  @Mock private BlockImporter blockImporter;
  @Mock private ProtocolContext protocolContext;
  private final Block idnBlock = new BlockDataGenerator().block();
  private final QbftBlock block = new QbftBlockAdaptor(idnBlock);

  @Test
  void importsBlockSuccessfullyWhenIdnBlockImports() {
    when(blockImporter.importBlock(protocolContext, idnBlock, HeaderValidationMode.FULL))
        .thenReturn(new BlockImportResult(IMPORTED));

    QbftBlockImporterAdaptor qbftBlockImporter =
        new QbftBlockImporterAdaptor(blockImporter, protocolContext);
    assertThat(qbftBlockImporter.importBlock(block)).isEqualTo(true);
  }

  @Test
  void importsBlockSuccessfullyWhenIdnBlockAlreadyImported() {
    when(blockImporter.importBlock(protocolContext, idnBlock, HeaderValidationMode.FULL))
        .thenReturn(new BlockImportResult(ALREADY_IMPORTED));

    QbftBlockImporterAdaptor qbftBlockImporter =
        new QbftBlockImporterAdaptor(blockImporter, protocolContext);
    assertThat(qbftBlockImporter.importBlock(block)).isEqualTo(true);
  }

  @Test
  void importsBlockFailsWhenIdnBlockNotImported() {
    when(blockImporter.importBlock(protocolContext, idnBlock, HeaderValidationMode.FULL))
        .thenReturn(new BlockImportResult(NOT_IMPORTED));

    QbftBlockImporterAdaptor qbftBlockImporter =
        new QbftBlockImporterAdaptor(blockImporter, protocolContext);
    assertThat(qbftBlockImporter.importBlock(block)).isEqualTo(false);
  }
}
