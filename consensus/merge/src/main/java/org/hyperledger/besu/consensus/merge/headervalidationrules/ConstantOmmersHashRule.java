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
package org.idnecology.idn.consensus.merge.headervalidationrules;

import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.mainnet.AttachedBlockHeaderValidationRule;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Constant ommers hash rule. */
public class ConstantOmmersHashRule implements AttachedBlockHeaderValidationRule {

  private static final Hash mergeConstant = Hash.EMPTY_LIST_HASH;

  private static final Logger LOG = LoggerFactory.getLogger(ConstantOmmersHashRule.class);

  /** Default constructor. */
  public ConstantOmmersHashRule() {}

  @Override
  public boolean validate(
      final BlockHeader header, final BlockHeader parent, final ProtocolContext protocolContext) {
    Optional<Difficulty> totalDifficulty =
        protocolContext.getBlockchain().getTotalDifficultyByHash(header.getParentHash());
    if (totalDifficulty.isEmpty()) {
      LOG.warn("unable to get total difficulty, parent {} not found", header.getParentHash());
      return false;
    }
    if (totalDifficulty
        .get()
        .greaterOrEqualThan(
            protocolContext.getConsensusContext(MergeContext.class).getTerminalTotalDifficulty())) {
      return header.getOmmersHash().equals(mergeConstant);
    } else {
      return true;
    }
  }

  @Override
  public boolean includeInLightValidation() {
    return AttachedBlockHeaderValidationRule.super.includeInLightValidation();
  }
}
