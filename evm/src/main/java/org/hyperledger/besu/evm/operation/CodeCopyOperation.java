/*
 * Copyright contributors to Idn ecology Idn.
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

import org.idnecology.idn.evm.Code;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

/** The Code copy operation. */
public class CodeCopyOperation extends AbstractOperation {

  /**
   * Instantiates a new Code copy operation.
   *
   * @param gasCalculator the gas calculator
   */
  public CodeCopyOperation(final GasCalculator gasCalculator) {
    super(0x39, "CODECOPY", 3, 0, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final long memOffset = clampedToLong(frame.popStackItem());
    final long sourceOffset = clampedToLong(frame.popStackItem());
    final long numBytes = clampedToLong(frame.popStackItem());

    final long cost = gasCalculator().dataCopyOperationGasCost(frame, memOffset, numBytes);
    if (frame.getRemainingGas() < cost) {
      return new OperationResult(cost, ExceptionalHaltReason.INSUFFICIENT_GAS);
    }

    final Code code = frame.getCode();

    frame.writeMemory(memOffset, sourceOffset, numBytes, code.getBytes(), true);

    return new OperationResult(cost, null);
  }
}
