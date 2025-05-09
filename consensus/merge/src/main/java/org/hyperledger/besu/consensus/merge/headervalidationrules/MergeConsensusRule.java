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

import static org.idnecology.idn.consensus.merge.TransitionUtils.isTerminalProofOfWorkBlock;

import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.mainnet.AttachedBlockHeaderValidationRule;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Merge consensus rule. */
public abstract class MergeConsensusRule implements AttachedBlockHeaderValidationRule {
  private static final Logger LOG = LoggerFactory.getLogger(MergeConsensusRule.class);

  /** Default Constructor. */
  public MergeConsensusRule() {}

  /**
   * Should use post merge rules.
   *
   * @param header the header
   * @param context the context
   * @return the boolean
   */
  // TODO: post-merge cleanup
  protected boolean shouldUsePostMergeRules(
      final BlockHeader header, final ProtocolContext context) {
    if (context.getConsensusContext(MergeContext.class).getFinalized().isPresent()) {
      // if anything has been finalized, we are now using PoS block validation rules forevermore
      return true;
    }

    Optional<Difficulty> parentChainTotalDifficulty =
        context.getBlockchain().getTotalDifficultyByHash(header.getParentHash());
    Difficulty configuredTotalTerminalDifficulty =
        context.getConsensusContext(MergeContext.class).getTerminalTotalDifficulty();

    if (parentChainTotalDifficulty.isEmpty()) {
      LOG.warn("unable to get total difficulty, parent {} not found", header.getParentHash());
      return false;
    }

    if (isTerminalProofOfWorkBlock(header, context)) {
      // ttd block looks like proof of stake, but is the last proof of work
      return false;
    }

    if (parentChainTotalDifficulty
        .get()
        .add(header.getDifficulty() == null ? Difficulty.ZERO : header.getDifficulty())
        .greaterOrEqualThan(configuredTotalTerminalDifficulty)) {
      return true;
    } else { // still PoWing
      return false;
    }
  }
}
