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
package org.idnecology.idn.ethereum.transaction;

import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public record TransactionSimulatorResult(
    Transaction transaction, TransactionProcessingResult result) {

  public boolean isSuccessful() {
    return result.isSuccessful();
  }

  public boolean isInvalid() {
    return result.isInvalid();
  }

  public long getGasEstimate() {
    return transaction.getGasLimit() - result.getGasRemaining();
  }

  public Bytes getOutput() {
    return result.getOutput();
  }

  public ValidationResult<TransactionInvalidReason> getValidationResult() {
    return result.getValidationResult();
  }

  public Optional<String> getInvalidReason() {
    return result.getInvalidReason();
  }
}
