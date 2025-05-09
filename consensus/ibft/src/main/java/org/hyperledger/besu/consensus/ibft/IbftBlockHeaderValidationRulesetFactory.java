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
package org.idnecology.idn.consensus.ibft;

import static org.idnecology.idn.ethereum.mainnet.AbstractGasLimitSpecification.DEFAULT_MAX_GAS_LIMIT;
import static org.idnecology.idn.ethereum.mainnet.AbstractGasLimitSpecification.DEFAULT_MIN_GAS_LIMIT;

import org.idnecology.idn.consensus.common.bft.BftHelpers;
import org.idnecology.idn.consensus.common.bft.headervalidationrules.BftCoinbaseValidationRule;
import org.idnecology.idn.consensus.common.bft.headervalidationrules.BftCommitSealsValidationRule;
import org.idnecology.idn.consensus.common.bft.headervalidationrules.BftValidatorsValidationRule;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.mainnet.BlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.AncestryValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.ConstantFieldValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.GasLimitRangeAndDeltaValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.GasUsageValidationRule;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.TimestampBoundedByFutureParameter;
import org.idnecology.idn.ethereum.mainnet.headervalidationrules.TimestampMoreRecentThanParent;

import java.time.Duration;
import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt256;

/** The Ibft block header validation ruleset factory. */
public class IbftBlockHeaderValidationRulesetFactory {
  /** Default constructor. */
  private IbftBlockHeaderValidationRulesetFactory() {}

  /**
   * Produces a BlockHeaderValidator configured for assessing bft block headers which are to form
   * part of the BlockChain (i.e. not proposed blocks, which do not contain commit seals)
   *
   * @param minimumTimeBetweenBlocks the minimum time which must elapse between blocks.
   * @param baseFeeMarket an {@link Optional} wrapping {@link BaseFeeMarket} class if appropriate.
   * @return BlockHeaderValidator configured for assessing bft block headers
   */
  public static BlockHeaderValidator.Builder blockHeaderValidator(
      final Duration minimumTimeBetweenBlocks, final Optional<BaseFeeMarket> baseFeeMarket) {
    final BlockHeaderValidator.Builder ruleBuilder =
        new BlockHeaderValidator.Builder()
            .addRule(new AncestryValidationRule())
            .addRule(new GasUsageValidationRule())
            .addRule(
                new GasLimitRangeAndDeltaValidationRule(
                    DEFAULT_MIN_GAS_LIMIT, DEFAULT_MAX_GAS_LIMIT, baseFeeMarket))
            .addRule(new TimestampBoundedByFutureParameter(1))
            .addRule(
                new ConstantFieldValidationRule<>(
                    "MixHash", BlockHeader::getMixHash, BftHelpers.EXPECTED_MIX_HASH))
            .addRule(
                new ConstantFieldValidationRule<>(
                    "OmmersHash", BlockHeader::getOmmersHash, Hash.EMPTY_LIST_HASH))
            .addRule(
                new ConstantFieldValidationRule<>(
                    "Difficulty", BlockHeader::getDifficulty, UInt256.ONE))
            .addRule(new ConstantFieldValidationRule<>("Nonce", BlockHeader::getNonce, 0L))
            .addRule(new BftValidatorsValidationRule())
            .addRule(new BftCoinbaseValidationRule())
            .addRule(new BftCommitSealsValidationRule());

    // Currently the minimum acceptable time between blocks is 1 second. The timestamp of an
    // Ethereum header is stored as seconds since Unix epoch so blocks being produced more
    // frequently than once a second cannot pass this validator. For non-production scenarios
    // (e.g. for testing block production much more frequently than once a second) Idn has
    // an experimental 'xblockperiodmilliseconds' option for BFT chains. If this is enabled
    // we cannot apply the TimestampMoreRecentThanParent validation rule so we do not add it
    if (minimumTimeBetweenBlocks.compareTo(Duration.ofSeconds(1)) >= 0) {
      ruleBuilder.addRule(new TimestampMoreRecentThanParent(minimumTimeBetweenBlocks.getSeconds()));
    }
    return ruleBuilder;
  }
}
