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
import static org.idnecology.idn.evm.internal.Words.clampedToLong;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.code.EOFLayout;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.internal.Words;

import org.apache.tuweni.bytes.Bytes;

/** The Ext code copy operation. */
public class ExtCodeCopyOperation extends AbstractOperation {

  /** This is the "code" legacy contracts see when copying code from an EOF contract. */
  public static final Bytes EOF_REPLACEMENT_CODE = Bytes.fromHexString("0xef00");

  private final boolean enableEIP3540;

  /**
   * Instantiates a new Ext code copy operation.
   *
   * @param gasCalculator the gas calculator
   */
  public ExtCodeCopyOperation(final GasCalculator gasCalculator) {
    this(gasCalculator, false);
  }

  /**
   * Instantiates a new Ext code copy operation.
   *
   * @param gasCalculator the gas calculator
   * @param enableEIP3540 enable EIP-3540 semantics (don't copy EOF)
   */
  public ExtCodeCopyOperation(final GasCalculator gasCalculator, final boolean enableEIP3540) {
    super(0x3C, "EXTCODECOPY", 4, 0, gasCalculator);
    this.enableEIP3540 = enableEIP3540;
  }

  /**
   * Cost of Ext Code Copy operation.
   *
   * @param frame the frame
   * @param memOffset the mem offset
   * @param length the length
   * @param accountIsWarm the account is warm
   * @return the long
   */
  protected long cost(
      final MessageFrame frame,
      final long memOffset,
      final long length,
      final boolean accountIsWarm) {
    return clampedAdd(
        gasCalculator().extCodeCopyOperationGasCost(frame, memOffset, length),
        accountIsWarm
            ? gasCalculator().getWarmStorageReadCost()
            : gasCalculator().getColdAccountAccessCost());
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final Address address = Words.toAddress(frame.popStackItem());
    final long memOffset = clampedToLong(frame.popStackItem());
    final long sourceOffset = clampedToLong(frame.popStackItem());
    final long numBytes = clampedToLong(frame.popStackItem());

    final boolean accountIsWarm =
        frame.warmUpAddress(address) || gasCalculator().isPrecompile(address);
    final long cost = cost(frame, memOffset, numBytes, accountIsWarm);

    if (frame.getRemainingGas() < cost) {
      return new OperationResult(cost, ExceptionalHaltReason.INSUFFICIENT_GAS);
    }

    final Account account = frame.getWorldUpdater().get(address);
    final Bytes code = account != null ? account.getCode() : Bytes.EMPTY;

    if (enableEIP3540
        && code.size() >= 2
        && code.get(0) == EOFLayout.EOF_PREFIX_BYTE
        && code.get(1) == 0) {
      frame.writeMemory(memOffset, sourceOffset, numBytes, EOF_REPLACEMENT_CODE);
    } else {
      frame.writeMemory(memOffset, sourceOffset, numBytes, code);
    }

    return new OperationResult(cost, null);
  }
}
