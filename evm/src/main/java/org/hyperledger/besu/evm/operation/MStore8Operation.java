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

import org.apache.tuweni.bytes.Bytes;

/** The M store8 operation. */
public class MStore8Operation extends AbstractOperation {

  /**
   * Instantiates a new M store8 operation.
   *
   * @param gasCalculator the gas calculator
   */
  public MStore8Operation(final GasCalculator gasCalculator) {
    super(0x53, "MSTORE8", 2, 0, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final long location = clampedToLong(frame.popStackItem());
    final Bytes value = frame.popStackItem();
    final byte theByte = (value.size() > 0) ? value.get(value.size() - 1) : 0;

    final long cost = gasCalculator().mStore8OperationGasCost(frame, location);
    if (frame.getRemainingGas() < cost) {
      return new OperationResult(cost, ExceptionalHaltReason.INSUFFICIENT_GAS);
    }

    frame.writeMemory(location, theByte, true);
    return new OperationResult(cost, null);
  }
}
