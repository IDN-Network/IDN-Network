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

import org.idnecology.idn.consensus.qbft.core.types.QbftBlockValidator;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.BlockValidator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QbftBlockValidatorAdaptorTest {
  @Mock private BlockValidator blockValidator;
  @Mock private ProtocolContext protocolContext;
  @Mock private Block idnBlock;
  @Mock private QbftBlockAdaptor qbftBlock;

  @Test
  void validateSuccessfullyWhenIdnValidatorSuccessful() {
    when(qbftBlock.getIdnBlock()).thenReturn(idnBlock);
    when(blockValidator.validateAndProcessBlock(
            protocolContext,
            idnBlock,
            HeaderValidationMode.LIGHT,
            HeaderValidationMode.FULL,
            false))
        .thenReturn(new BlockProcessingResult(Optional.empty()));

    QbftBlockValidatorAdaptor qbftBlockValidator =
        new QbftBlockValidatorAdaptor(blockValidator, protocolContext);
    QbftBlockValidator.ValidationResult validationResult =
        qbftBlockValidator.validateBlock(qbftBlock);
    assertThat(validationResult.success()).isTrue();
    assertThat(validationResult.errorMessage()).isEmpty();
  }

  @Test
  void validateFailsWhenIdnValidatorFails() {
    when(qbftBlock.getIdnBlock()).thenReturn(idnBlock);
    when(blockValidator.validateAndProcessBlock(
            protocolContext,
            idnBlock,
            HeaderValidationMode.LIGHT,
            HeaderValidationMode.FULL,
            false))
        .thenReturn(new BlockProcessingResult("failed"));

    QbftBlockValidatorAdaptor qbftBlockValidator =
        new QbftBlockValidatorAdaptor(blockValidator, protocolContext);
    QbftBlockValidator.ValidationResult validationResult =
        qbftBlockValidator.validateBlock(qbftBlock);
    assertThat(validationResult.success()).isFalse();
    assertThat(validationResult.errorMessage()).contains("failed");
  }
}
