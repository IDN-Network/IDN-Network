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
package org.idnecology.idn.enclave.types;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The Find privacy group request. */
public class FindPrivacyGroupRequest {

  private final List<String> addresses;

  /**
   * Instantiates a new Find privacy group request.
   *
   * @param addresses the addresses
   */
  @JsonCreator
  public FindPrivacyGroupRequest(@JsonProperty("addresses") final List<String> addresses) {
    this.addresses = addresses;
  }

  /**
   * Addresses list.
   *
   * @return the list
   */
  @JsonProperty("addresses")
  public List<String> addresses() {
    return addresses;
  }
}
