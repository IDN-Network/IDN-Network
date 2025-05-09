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
package org.idnecology.idn.consensus.qbft.core.types;

import org.idnecology.idn.datatypes.Address;

import java.util.Collection;

/** The interface Validator provider. */
public interface QbftValidatorProvider {

  /**
   * Gets validators at head.
   *
   * @return the validators at head
   */
  Collection<Address> getValidatorsAtHead();

  /**
   * Gets validators after block.
   *
   * @param header the header
   * @return the validators after block
   */
  Collection<Address> getValidatorsAfterBlock(QbftBlockHeader header);

  /**
   * Gets validators for block.
   *
   * @param header the header
   * @return the validators for block
   */
  Collection<Address> getValidatorsForBlock(QbftBlockHeader header);
}
