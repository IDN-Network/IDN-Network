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
package org.idnecology.idn.consensus.merge.headervalidationrules;

import static org.idnecology.idn.consensus.merge.TransitionUtils.isTerminalProofOfWorkBlock;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The No difficulty rule. */
public class NoDifficultyRule extends MergeConsensusRule {

  private static final Logger LOG = LoggerFactory.getLogger(NoDifficultyRule.class);

  /** Default Constructor. */
  public NoDifficultyRule() {}

  @Override
  public boolean validate(
      final BlockHeader header, final BlockHeader parent, final ProtocolContext protocolContext) {
    Optional<Difficulty> totalDifficulty =
        protocolContext.getBlockchain().getTotalDifficultyByHash(header.getParentHash());
    if (totalDifficulty.isEmpty()) {
      LOG.warn("unable to get total difficulty, parent {} not found", header.getParentHash());
      return false;
    }
    if (super.shouldUsePostMergeRules(header, protocolContext)
        && !isTerminalProofOfWorkBlock(header, protocolContext)) {
      return (header.getDifficulty() == null || header.getDifficulty().equals(Difficulty.ZERO));
    } else {
      return true;
    }
  }
}
