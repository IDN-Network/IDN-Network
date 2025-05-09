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

import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.Bytes;

/** The Stop operation. */
public class StopOperation extends AbstractFixedCostOperation {

  /** Opcode of STOP operation */
  public static final int OPCODE = 0x00;

  /** The Stop operation success result. */
  static final OperationResult stopSuccess = new OperationResult(0, null);

  /**
   * Instantiates a new Stop operation.
   *
   * @param gasCalculator the gas calculator
   */
  public StopOperation(final GasCalculator gasCalculator) {
    super(OPCODE, "STOP", 0, 0, gasCalculator, gasCalculator.getZeroTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs Stop operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    frame.setState(MessageFrame.State.CODE_SUCCESS);
    frame.setOutputData(Bytes.EMPTY);
    return stopSuccess;
  }
}
