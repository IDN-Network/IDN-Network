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
package org.idnecology.idn.consensus.common;

import org.idnecology.idn.ethereum.ConsensusContext;

/** The Migrating context. */
public class MigratingConsensusContext implements ConsensusContext {

  private final ForksSchedule<ConsensusContext> consensusContextSchedule;

  /**
   * Instantiates a new Migrating context.
   *
   * @param consensusContextSchedule the consensus context schedule
   */
  public MigratingConsensusContext(final ForksSchedule<ConsensusContext> consensusContextSchedule) {
    this.consensusContextSchedule = consensusContextSchedule;
  }

  /**
   * Gets consensus context schedule.
   *
   * @return the consensus context schedule
   */
  public ForksSchedule<ConsensusContext> getConsensusContextSchedule() {
    return consensusContextSchedule;
  }

  @Override
  public <C extends ConsensusContext> C as(final Class<C> klass) {
    return klass.cast(this);
  }
}
