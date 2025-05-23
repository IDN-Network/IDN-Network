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
package org.idnecology.idn.consensus.qbft.core.statemachine;

import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.events.RoundExpiry;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Commit;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Prepare;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.Proposal;
import org.idnecology.idn.consensus.qbft.core.messagewrappers.RoundChange;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;

/** The type NoOp block height manager. */
public class NoOpBlockHeightManager implements BaseQbftBlockHeightManager {

  private final QbftBlockHeader parentHeader;

  /**
   * Instantiates a new NoOp block height manager.
   *
   * @param parentHeader the parent header
   */
  public NoOpBlockHeightManager(final QbftBlockHeader parentHeader) {
    this.parentHeader = parentHeader;
  }

  @Override
  public void handleBlockTimerExpiry(final ConsensusRoundIdentifier roundIdentifier) {}

  @Override
  public void roundExpired(final RoundExpiry expire) {}

  @Override
  public void handleProposalPayload(final Proposal proposal) {}

  @Override
  public void handlePreparePayload(final Prepare prepare) {}

  @Override
  public void handleCommitPayload(final Commit commit) {}

  @Override
  public void handleRoundChangePayload(final RoundChange roundChange) {}

  @Override
  public long getChainHeight() {
    return parentHeader.getNumber() + 1;
  }

  @Override
  public QbftBlockHeader getParentBlockHeader() {
    return parentHeader;
  }
}
