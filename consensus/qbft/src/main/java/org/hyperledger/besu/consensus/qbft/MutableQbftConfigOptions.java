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
package org.idnecology.idn.consensus.qbft;

import org.idnecology.idn.config.QbftConfigOptions;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.MutableBftConfigOptions;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * A mutable {@link QbftConfigOptions} that is used for building config for transitions in the
 * {@link ForksSchedule}.
 */
public class MutableQbftConfigOptions extends MutableBftConfigOptions implements QbftConfigOptions {
  private Optional<String> validatorContractAddress;

  /**
   * Instantiates a new Mutable qbft config options.
   *
   * @param qbftConfigOptions the qbft config options
   */
  public MutableQbftConfigOptions(final QbftConfigOptions qbftConfigOptions) {
    super(qbftConfigOptions);
    this.validatorContractAddress =
        qbftConfigOptions.getValidatorContractAddress().map(String::toLowerCase);
  }

  @Override
  public Optional<String> getValidatorContractAddress() {
    return validatorContractAddress;
  }

  /**
   * Sets validator contract address.
   *
   * @param validatorContractAddress the validator contract address
   */
  public void setValidatorContractAddress(final Optional<String> validatorContractAddress) {
    this.validatorContractAddress = validatorContractAddress;
  }

  @Override
  public OptionalLong getStartBlock() {
    return OptionalLong.empty();
  }
}
