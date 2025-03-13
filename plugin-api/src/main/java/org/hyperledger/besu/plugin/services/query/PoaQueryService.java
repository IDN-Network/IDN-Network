/*
 * Copyright 2019 ConsenSys AG.
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
package org.idnecology.idn.plugin.services.query;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.plugin.data.BlockHeader;
import org.idnecology.idn.plugin.services.IdnService;

import java.util.Collection;

/** Provides methods to query the status of a Proof of Authority (PoA) network. */
public interface PoaQueryService extends IdnService {

  /**
   * Retrieves the validators specified in the latest block from the canonical chain.
   *
   * @return Addresses of all validators in the latest canonical block.
   */
  Collection<Address> getValidatorsForLatestBlock();

  /**
   * Retrieves the {@link Address} for the proposer of a block on the canonical chain.
   *
   * @param header The {@link BlockHeader} for which the proposer will be found.
   * @return The identity of the proposer for the given block.
   */
  Address getProposerOfBlock(final BlockHeader header);

  /**
   * Retrieves the signer {@link Address} of the local node. This is the address added to the {@link
   * BlockHeader} to identify that a specific node was a validator during block creation.
   *
   * @return The signer {@link Address} of the local node.
   */
  Address getLocalSignerAddress();
}
