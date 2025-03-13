/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.mainnet;

import org.idnecology.idn.collections.trie.BytesTrieSet;
import org.idnecology.idn.datatypes.Address;

import java.util.Set;

public class CodeDelegationResult {
  private final Set<Address> accessedDelegatorAddresses = new BytesTrieSet<>(Address.SIZE);
  private long alreadyExistingDelegators = 0L;

  public void addAccessedDelegatorAddress(final Address address) {
    accessedDelegatorAddresses.add(address);
  }

  public void incrementAlreadyExistingDelegators() {
    alreadyExistingDelegators += 1;
  }

  public Set<Address> accessedDelegatorAddresses() {
    return accessedDelegatorAddresses;
  }

  public long alreadyExistingDelegators() {
    return alreadyExistingDelegators;
  }
}
