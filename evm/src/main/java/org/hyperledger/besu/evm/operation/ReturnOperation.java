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
package org.idnecology.idn.evm.operation;

import static org.idnecology.idn.evm.internal.Words.clampedToLong;

import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

/** The Return operation. */
public class ReturnOperation extends AbstractOperation {

  /** RETURN opcode number */
  public static final int OPCODE = 0xF3;

  /**
   * Instantiates a new Return operation.
   *
   * @param gasCalculator the gas calculator
   */
  public ReturnOperation(final GasCalculator gasCalculator) {
    super(OPCODE, "RETURN", 2, 0, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final long from = clampedToLong(frame.popStackItem());
    final long length = clampedToLong(frame.popStackItem());

    final long cost = gasCalculator().memoryExpansionGasCost(frame, from, length);
    if (frame.getRemainingGas() < cost) {
      return new OperationResult(cost, ExceptionalHaltReason.INSUFFICIENT_GAS);
    }

    frame.setOutputData(frame.readMemory(from, length));
    frame.setState(MessageFrame.State.CODE_SUCCESS);
    return new OperationResult(cost, null);
  }
}
