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
package org.idnecology.idn.evm.contractvalidation;

import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Prefix code rule. */
public class PrefixCodeRule implements ContractValidationRule {

  private static final Logger LOG = LoggerFactory.getLogger(PrefixCodeRule.class);

  private static final byte FORMAT_RESERVED = (byte) 0xEF;

  /** Default constructor. */
  public PrefixCodeRule() {
    // This constructor exists because of JavaDoc linting rules.
  }

  @Override
  // As per https://eips.ethereum.org/EIPS/eip-3541
  public Optional<ExceptionalHaltReason> validate(
      final Bytes contractCode, final MessageFrame frame, final EVM evm) {
    if (!contractCode.isEmpty() && contractCode.get(0) == FORMAT_RESERVED) {
      LOG.trace("Contract creation error: code cannot start with {}", FORMAT_RESERVED);
      return Optional.of(ExceptionalHaltReason.INVALID_CODE);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Instantiate ContractValidationRule of prefix code rule
   *
   * @return the contract validation rule
   */
  public static ContractValidationRule of() {
    return new PrefixCodeRule();
  }
}
