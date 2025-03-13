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

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.code.EOFLayout;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.internal.OverflowException;
import org.idnecology.idn.evm.internal.UnderflowException;
import org.idnecology.idn.evm.internal.Words;

import org.apache.tuweni.bytes.Bytes;

/** The Ext code hash operation. */
public class ExtCodeHashOperation extends AbstractOperation {

  // // 0x9dbf3648db8210552e9c4f75c6a1c3057c0ca432043bd648be15fe7be05646f5
  static final Hash EOF_REPLACEMENT_HASH = Hash.hash(ExtCodeCopyOperation.EOF_REPLACEMENT_CODE);

  private final boolean enableEIP3540;

  /**
   * Instantiates a new Ext code hash operation.
   *
   * @param gasCalculator the gas calculator
   */
  public ExtCodeHashOperation(final GasCalculator gasCalculator) {
    this(gasCalculator, false);
  }

  /**
   * Instantiates a new Ext code copy operation.
   *
   * @param gasCalculator the gas calculator
   * @param enableEIP3540 enable EIP-3540 semantics (don't copy EOF)
   */
  public ExtCodeHashOperation(final GasCalculator gasCalculator, final boolean enableEIP3540) {
    super(0x3F, "EXTCODEHASH", 1, 1, gasCalculator);
    this.enableEIP3540 = enableEIP3540;
  }

  /**
   * Cost of Ext code hash operation.
   *
   * @param accountIsWarm the account is warm
   * @return the long
   */
  protected long cost(final boolean accountIsWarm) {
    return gasCalculator().extCodeHashOperationGasCost()
        + (accountIsWarm
            ? gasCalculator().getWarmStorageReadCost()
            : gasCalculator().getColdAccountAccessCost());
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    try {
      final Address address = Words.toAddress(frame.popStackItem());
      final boolean accountIsWarm =
          frame.warmUpAddress(address) || gasCalculator().isPrecompile(address);
      final long cost = cost(accountIsWarm);
      if (frame.getRemainingGas() < cost) {
        return new OperationResult(cost, ExceptionalHaltReason.INSUFFICIENT_GAS);
      }

      final Account account = frame.getWorldUpdater().get(address);

      if (account == null || account.isEmpty()) {
        frame.pushStackItem(Bytes.EMPTY);
      } else {
        final Bytes code = account.getCode();
        if (enableEIP3540
            && code.size() >= 2
            && code.get(0) == EOFLayout.EOF_PREFIX_BYTE
            && code.get(1) == 0) {
          frame.pushStackItem(EOF_REPLACEMENT_HASH);
        } else {
          frame.pushStackItem(account.getCodeHash());
        }
      }
      return new OperationResult(cost, null);

    } catch (final UnderflowException ufe) {
      return new OperationResult(cost(true), ExceptionalHaltReason.INSUFFICIENT_STACK_ITEMS);
    } catch (final OverflowException ofe) {
      return new OperationResult(cost(true), ExceptionalHaltReason.TOO_MANY_STACK_ITEMS);
    }
  }
}
