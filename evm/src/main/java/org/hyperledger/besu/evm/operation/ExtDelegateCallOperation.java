/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.evm.operation;

import static org.idnecology.idn.evm.internal.Words.clampedToLong;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

/** The Delegate call operation. */
public class ExtDelegateCallOperation extends AbstractExtCallOperation {

  /** The constant OPCODE. */
  public static final int OPCODE = 0xF9;

  /**
   * Instantiates a new Delegate call operation.
   *
   * @param gasCalculator the gas calculator
   */
  public ExtDelegateCallOperation(final GasCalculator gasCalculator) {
    super(OPCODE, "EXTDELEGATECALL", 3, 1, gasCalculator);
  }

  @Override
  protected Wei value(final MessageFrame frame) {
    return Wei.ZERO;
  }

  @Override
  protected Wei apparentValue(final MessageFrame frame) {
    return frame.getApparentValue();
  }

  @Override
  protected long inputDataOffset(final MessageFrame frame) {
    return clampedToLong(frame.getStackItem(STACK_INPUT_OFFSET));
  }

  @Override
  protected long inputDataLength(final MessageFrame frame) {
    return clampedToLong(frame.getStackItem(STACK_INPUT_LENGTH));
  }

  @Override
  protected Address address(final MessageFrame frame) {
    return frame.getRecipientAddress();
  }

  @Override
  protected Address sender(final MessageFrame frame) {
    return frame.getSenderAddress();
  }

  @Override
  protected boolean isDelegate() {
    return true;
  }
}
