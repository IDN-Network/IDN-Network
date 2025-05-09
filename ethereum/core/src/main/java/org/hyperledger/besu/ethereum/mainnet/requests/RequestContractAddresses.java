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
package org.idnecology.idn.ethereum.mainnet.requests;

import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.datatypes.Address;

public class RequestContractAddresses {
  private final Address withdrawalRequestContractAddress;
  private final Address depositContractAddress;
  private final Address consolidationRequestContractAddress;

  public static final Address DEFAULT_WITHDRAWAL_REQUEST_CONTRACT_ADDRESS =
      Address.fromHexString("0x00000961ef480eb55e80d19ad83579a64c007002");
  public static final Address DEFAULT_CONSOLIDATION_REQUEST_CONTRACT_ADDRESS =
      Address.fromHexString("0x0000bbddc7ce488642fb579f8b00f3a590007251");
  public static final Address DEFAULT_DEPOSIT_CONTRACT_ADDRESS =
      Address.fromHexString("0x00000000219ab540356cbb839cbe05303d7705fa");

  public RequestContractAddresses(
      final Address withdrawalRequestContractAddress,
      final Address depositContractAddress,
      final Address consolidationRequestContractAddress) {
    this.withdrawalRequestContractAddress = withdrawalRequestContractAddress;
    this.depositContractAddress = depositContractAddress;
    this.consolidationRequestContractAddress = consolidationRequestContractAddress;
  }

  public static RequestContractAddresses fromGenesis(
      final GenesisConfigOptions genesisConfigOptions) {
    return new RequestContractAddresses(
        genesisConfigOptions
            .getWithdrawalRequestContractAddress()
            .orElse(DEFAULT_WITHDRAWAL_REQUEST_CONTRACT_ADDRESS),
        genesisConfigOptions.getDepositContractAddress().orElse(DEFAULT_DEPOSIT_CONTRACT_ADDRESS),
        genesisConfigOptions
            .getConsolidationRequestContractAddress()
            .orElse(DEFAULT_CONSOLIDATION_REQUEST_CONTRACT_ADDRESS));
  }

  public Address getWithdrawalRequestContractAddress() {
    return withdrawalRequestContractAddress;
  }

  public Address getDepositContractAddress() {
    return depositContractAddress;
  }

  public Address getConsolidationRequestContractAddress() {
    return consolidationRequestContractAddress;
  }
}
