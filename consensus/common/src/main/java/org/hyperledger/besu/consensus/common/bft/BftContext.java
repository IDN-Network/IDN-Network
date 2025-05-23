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
package org.idnecology.idn.consensus.common.bft;

import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.PoaContext;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.ethereum.ConsensusContext;

/** Holds the BFT specific mutable state. */
public class BftContext implements PoaContext {

  private final ValidatorProvider validatorProvider;
  private final EpochManager epochManager;
  private final BftBlockInterface blockInterface;

  /**
   * Instantiates a new Bft context.
   *
   * @param validatorProvider the validator provider
   * @param epochManager the epoch manager
   * @param blockInterface the block interface
   */
  public BftContext(
      final ValidatorProvider validatorProvider,
      final EpochManager epochManager,
      final BftBlockInterface blockInterface) {
    this.validatorProvider = validatorProvider;
    this.epochManager = epochManager;
    this.blockInterface = blockInterface;
  }

  /**
   * Gets validator provider.
   *
   * @return the validator provider
   */
  public ValidatorProvider getValidatorProvider() {
    return validatorProvider;
  }

  /**
   * Gets epoch manager.
   *
   * @return the epoch manager
   */
  public EpochManager getEpochManager() {
    return epochManager;
  }

  @Override
  public BftBlockInterface getBlockInterface() {
    return blockInterface;
  }

  @Override
  public <C extends ConsensusContext> C as(final Class<C> klass) {
    return klass.cast(this);
  }
}
