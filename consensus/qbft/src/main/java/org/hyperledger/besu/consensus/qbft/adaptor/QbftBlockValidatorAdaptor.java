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

import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockValidator;
import org.idnecology.idn.ethereum.BlockProcessingResult;
import org.idnecology.idn.ethereum.BlockValidator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;

/** Adaptor class to allow a {@link BlockValidator} to be used as a {@link QbftBlockValidator}. */
public class QbftBlockValidatorAdaptor implements QbftBlockValidator {

  private final BlockValidator blockValidator;
  private final ProtocolContext protocolContext;

  /**
   * Constructs a new Qbft block validator
   *
   * @param blockValidator The Idn block validator
   * @param protocolContext The protocol context
   */
  public QbftBlockValidatorAdaptor(
      final BlockValidator blockValidator, final ProtocolContext protocolContext) {
    this.blockValidator = blockValidator;
    this.protocolContext = protocolContext;
  }

  @Override
  public ValidationResult validateBlock(final QbftBlock block) {
    final BlockProcessingResult blockProcessingResult =
        blockValidator.validateAndProcessBlock(
            protocolContext,
            BlockUtil.toIdnBlock(block),
            HeaderValidationMode.LIGHT,
            HeaderValidationMode.FULL,
            false);
    return new ValidationResult(
        blockProcessingResult.isSuccessful(), blockProcessingResult.errorMessage);
  }
}
