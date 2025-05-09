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
package org.idnecology.idn.ethereum.eth.sync.fullsync;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.chain.Blockchain;

public class FlexibleBlockHashTerminalCondition implements SyncTerminationCondition {
  private Hash blockHash;
  private final Blockchain blockchain;

  public FlexibleBlockHashTerminalCondition(final Hash blockHash, final Blockchain blockchain) {
    this.blockHash = blockHash;
    this.blockchain = blockchain;
  }

  public synchronized void setBlockHash(final Hash blockHash) {
    this.blockHash = blockHash;
  }

  @Override
  public synchronized boolean getAsBoolean() {
    return blockchain.contains(blockHash);
  }
}
