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
package org.idnecology.idn.consensus.clique;

import static org.idnecology.idn.ethereum.mainnet.AbstractGasLimitSpecification.DEFAULT_MAX_GAS_LIMIT;
import static org.idnecology.idn.ethereum.mainnet.AbstractGasLimitSpecification.DEFAULT_MIN_GAS_LIMIT;

import org.idnecology.idn.config.MergeConfiguration;
import org.idnecology.idn.consensus.clique.headervalidationrules.CliqueDifficultyValidationRule;
import org.idnecology.idn.consensus.clique.headervalidationrules.CliqueExtraDataValidationRule;
import org.idnecology.idn.consensus.clique.headervalidationrules.CliqueNoEmptyBlockValidationRule;
import org.idnecology.idn.consensus.clique.headervalidationrules.CoinbaseHeaderValidationRule;
import org.idnecology.idn.consensus.clique.headervalidationrules.SignerRateLimitValidationRule;
import org.idnecology.idn.consensus.clique.headervalidationrules.VoteValidationRule;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.mainnet.BlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.AncestryValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.AttachedComposedFromDetachedRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.BaseFeeMarketBlockHeaderGasPriceValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.ConstantFieldValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.GasLimitRangeAndDeltaValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.GasUsageValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.TimestampBoundedByFutureParameter;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.TimestampMoreRecentThanParent;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;

/** The Block header validation ruleset factory. */
public class BlockHeaderValidationRulesetFactory {
  /** Default constructor. */
  private BlockHeaderValidationRulesetFactory() {}

  /**
   * Creates a set of rules which when executed will determine if a given block header is valid with
   * respect to its parent (or chain).
   *
   * <p>Specifically the set of rules provided by this function are to be used for a Clique chain.
   *
   * @param secondsBetweenBlocks the minimum number of seconds which must elapse between blocks.
   * @param createEmptyBlocks whether clique should allow the creation of empty blocks.
   * @param epochManager an object which determines if a given block is an epoch block.
   * @param baseFeeMarket an {@link Optional} wrapping {@link BaseFeeMarket} class if appropriate.
   * @return the header validator.
   */
  public static BlockHeaderValidator.Builder cliqueBlockHeaderValidator(
      final long secondsBetweenBlocks,
      final boolean createEmptyBlocks,
      final EpochManager epochManager,
      final Optional<BaseFeeMarket> baseFeeMarket) {
    return cliqueBlockHeaderValidator(
        secondsBetweenBlocks,
        createEmptyBlocks,
        epochManager,
        baseFeeMarket,
        MergeConfiguration.isMergeEnabled());
  }

  /**
   * Clique block header validator. Visible for testing.
   *
   * @param secondsBetweenBlocks the seconds between blocks
   * @param createEmptyBlocks whether clique should allow the creation of empty blocks.
   * @param epochManager the epoch manager
   * @param baseFeeMarket the base fee market
   * @param isMergeEnabled the is merge enabled
   * @return the block header validator . builder
   */
  @VisibleForTesting
  public static BlockHeaderValidator.Builder cliqueBlockHeaderValidator(
      final long secondsBetweenBlocks,
      final boolean createEmptyBlocks,
      final EpochManager epochManager,
      final Optional<BaseFeeMarket> baseFeeMarket,
      final boolean isMergeEnabled) {

    final BlockHeaderValidator.Builder builder =
        new BlockHeaderValidator.Builder()
            .addRule(new AncestryValidationRule())
            .addRule(new TimestampBoundedByFutureParameter(10))
            .addRule(
                new GasLimitRangeAndDeltaValidationRule(
                    DEFAULT_MIN_GAS_LIMIT, DEFAULT_MAX_GAS_LIMIT, baseFeeMarket))
            .addRule(
                new ConstantFieldValidationRule<>(
                    "OmmersHash", BlockHeader::getOmmersHash, Hash.EMPTY_LIST_HASH))
            .addRule(new CliqueExtraDataValidationRule(epochManager))
            .addRule(new CliqueDifficultyValidationRule())
            .addRule(new SignerRateLimitValidationRule())
            .addRule(new CoinbaseHeaderValidationRule(epochManager))
            .addRule(new GasUsageValidationRule());

    if (baseFeeMarket.isPresent()) {
      builder.addRule(new BaseFeeMarketBlockHeaderGasPriceValidationRule(baseFeeMarket.get()));
    }

    if (!createEmptyBlocks) {
      builder.addRule(new CliqueNoEmptyBlockValidationRule());
    }

    var mixHashRule =
        new ConstantFieldValidationRule<>("MixHash", BlockHeader::getMixHash, Hash.ZERO);
    var voteValidationRule = new VoteValidationRule();
    var cliqueTimestampRule = new TimestampMoreRecentThanParent(secondsBetweenBlocks);

    if (isMergeEnabled) {
      builder
          .addRule(new AttachedComposedFromDetachedRule(mixHashRule))
          .addRule(new AttachedComposedFromDetachedRule(voteValidationRule))
          .addRule(new AttachedComposedFromDetachedRule(cliqueTimestampRule));
    } else {
      builder.addRule(mixHashRule).addRule(voteValidationRule).addRule(cliqueTimestampRule);
    }

    return builder;
  }
}
