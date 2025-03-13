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

import static org.idnecology.idn.evm.internal.Words.clampedAdd;
import static org.idnecology.idn.evm.internal.Words.clampedToInt;
import static org.idnecology.idn.evm.internal.Words.clampedToLong;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.evm.Code;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;

/** The Create operation. */
public class CreateOperation extends AbstractCreateOperation {

  /**
   * Instantiates a new Create operation.
   *
   * @param gasCalculator the gas calculator
   */
  public CreateOperation(final GasCalculator gasCalculator) {
    super(0xF0, "CREATE", 3, 1, gasCalculator, 0);
  }

  @Override
  public long cost(final MessageFrame frame, final Supplier<Code> unused) {
    final int inputOffset = clampedToInt(frame.getStackItem(1));
    final int inputSize = clampedToInt(frame.getStackItem(2));
    return clampedAdd(
        clampedAdd(
            gasCalculator().txCreateCost(),
            gasCalculator().memoryExpansionGasCost(frame, inputOffset, inputSize)),
        gasCalculator().initcodeCost(inputSize));
  }

  @Override
  protected Address generateTargetContractAddress(final MessageFrame frame, final Code initcode) {
    final Account sender = frame.getWorldUpdater().get(frame.getRecipientAddress());
    // Decrement nonce by 1 to normalize the effect of transaction execution
    return Address.contractAddress(frame.getRecipientAddress(), sender.getNonce() - 1L);
  }

  @Override
  protected Code getInitCode(final MessageFrame frame, final EVM evm) {
    final long inputOffset = clampedToLong(frame.getStackItem(1));
    final long inputSize = clampedToLong(frame.getStackItem(2));
    final Bytes inputData = frame.readMemory(inputOffset, inputSize);
    // Never cache CREATEx initcode. The amount of reuse is very low, and caching mostly
    // addresses disk loading delay, and we already have the code.
    return evm.getCodeUncached(inputData);
  }
}
