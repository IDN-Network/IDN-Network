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

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.frame.ExceptionalHaltReason;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.internal.OverflowException;
import org.idnecology.idn.evm.internal.UnderflowException;
import org.idnecology.idn.evm.internal.Words;

import org.apache.tuweni.bytes.Bytes;

/** The Balance operation. */
public class BalanceOperation extends AbstractOperation {

  /**
   * Instantiates a new Balance operation.
   *
   * @param gasCalculator the gas calculator
   */
  public BalanceOperation(final GasCalculator gasCalculator) {
    super(0x31, "BALANCE", 1, 1, gasCalculator);
  }

  /**
   * Gets Balance operation Gas Cost plus warm storage read cost or cold account access cost.
   *
   * @param accountIsWarm true to add warm storage read cost, false to add cold account access cost
   * @return the long
   */
  protected long cost(final boolean accountIsWarm) {
    return gasCalculator().getBalanceOperationGasCost()
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
      } else {
        final Account account = frame.getWorldUpdater().get(address);
        frame.pushStackItem(account == null ? Bytes.EMPTY : account.getBalance());
        return new OperationResult(cost, null);
      }
    } catch (final UnderflowException ufe) {
      return new OperationResult(cost(true), ExceptionalHaltReason.INSUFFICIENT_STACK_ITEMS);
    } catch (final OverflowException ofe) {
      return new OperationResult(cost(true), ExceptionalHaltReason.TOO_MANY_STACK_ITEMS);
    }
  }
}
