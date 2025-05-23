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

import org.idnecology.idn.config.BftConfigOptions;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.BaseBftProtocolScheduleBuilder;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.BftProtocolSchedule;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.BlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.time.Duration;
import java.util.Optional;

/** Defines the protocol behaviours for a blockchain using a BFT consensus mechanism. */
public class IbftProtocolScheduleBuilder extends BaseBftProtocolScheduleBuilder {
  /** Default constructor. */
  protected IbftProtocolScheduleBuilder() {}

  /**
   * Create protocol schedule.
   *
   * @param config the config
   * @param forksSchedule the forks schedule
   * @param privacyParameters the privacy parameters
   * @param isRevertReasonEnabled the is revert reason enabled
   * @param bftExtraDataCodec the bft extra data codec
   * @param evmConfiguration the evm configuration
   * @param miningConfiguration the mining parameters
   * @param badBlockManager the cache to use to keep invalid blocks
   * @param isParallelTxProcessingEnabled indicates whether parallel transaction is enabled
   * @param metricsSystem A metricSystem instance to be able to expose metrics in the underlying
   *     calls
   * @return the protocol schedule
   */
  public static BftProtocolSchedule create(
      final GenesisConfigOptions config,
      final ForksSchedule<BftConfigOptions> forksSchedule,
      final PrivacyParameters privacyParameters,
      final boolean isRevertReasonEnabled,
      final BftExtraDataCodec bftExtraDataCodec,
      final EvmConfiguration evmConfiguration,
      final MiningConfiguration miningConfiguration,
      final BadBlockManager badBlockManager,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return new IbftProtocolScheduleBuilder()
        .createProtocolSchedule(
            config,
            forksSchedule,
            privacyParameters,
            isRevertReasonEnabled,
            bftExtraDataCodec,
            evmConfiguration,
            miningConfiguration,
            badBlockManager,
            isParallelTxProcessingEnabled,
            metricsSystem);
  }

  /**
   * Create protocol schedule.
   *
   * @param config the config
   * @param forksSchedule the forks schedule
   * @param bftExtraDataCodec the bft extra data codec
   * @param evmConfiguration the evm configuration
   * @param miningConfiguration the mining parameters
   * @param badBlockManager the cache to use to keep invalid blocks
   * @param isParallelTxProcessingEnabled indicates whether parallel transaction is enabled.
   * @param metricsSystem A metricSystem instance to be able to expose metrics in the underlying
   *     calls
   * @return the protocol schedule
   */
  public static BftProtocolSchedule create(
      final GenesisConfigOptions config,
      final ForksSchedule<BftConfigOptions> forksSchedule,
      final BftExtraDataCodec bftExtraDataCodec,
      final EvmConfiguration evmConfiguration,
      final MiningConfiguration miningConfiguration,
      final BadBlockManager badBlockManager,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return create(
        config,
        forksSchedule,
        PrivacyParameters.DEFAULT,
        false,
        bftExtraDataCodec,
        evmConfiguration,
        miningConfiguration,
        badBlockManager,
        isParallelTxProcessingEnabled,
        metricsSystem);
  }

  @Override
  protected BlockHeaderValidator.Builder createBlockHeaderRuleset(
      final BftConfigOptions config, final FeeMarket feeMarket) {
    final Optional<BaseFeeMarket> baseFeeMarket =
        Optional.of(feeMarket).filter(FeeMarket::implementsBaseFee).map(BaseFeeMarket.class::cast);

    return IbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(
        config.getBlockPeriodMilliseconds() > 0
            ? Duration.ofMillis(config.getBlockPeriodMilliseconds())
            : Duration.ofSeconds(config.getBlockPeriodSeconds()),
        baseFeeMarket);
  }
}
