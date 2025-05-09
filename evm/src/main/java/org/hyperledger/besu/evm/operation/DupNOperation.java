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

import org.idnecology.idn.evm.Code;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

/** The Dup operation. */
public class DupNOperation extends AbstractFixedCostOperation {

  /** DUPN Opcode 0xe6 */
  public static final int OPCODE = 0xe6;

  /** The Dup success operation result. */
  static final OperationResult dupSuccess = new OperationResult(3, null);

  /**
   * Instantiates a new Dup operation.
   *
   * @param gasCalculator the gas calculator
   */
  public DupNOperation(final GasCalculator gasCalculator) {
    super(OPCODE, "DUPN", 0, 1, gasCalculator, gasCalculator.getVeryLowTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    Code code = frame.getCode();
    if (code.getEofVersion() == 0) {
      return InvalidOperation.INVALID_RESULT;
    }
    int pc = frame.getPC();

    int depth = code.readU8(pc + 1);
    frame.pushStackItem(frame.getStackItem(depth));
    frame.setPC(pc + 1);

    return dupSuccess;
  }
}
