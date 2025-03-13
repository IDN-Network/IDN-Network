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
package org.idnecology.idn.ethereum.mainnet;

import static org.idnecology.idn.ethereum.mainnet.MainnetProtocolSpecs.powHasher;

import org.idnecology.idn.config.PowAlgorithm;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.core.feemarket.CoinbaseFeePriceCalculator;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.evm.ClassicEVMs;
import org.idnecology.idn.evm.MainnetEVMs;
import org.idnecology.idn.evm.contractvalidation.MaxCodeSizeRule;
import org.idnecology.idn.evm.contractvalidation.PrefixCodeRule;
import org.idnecology.idn.evm.gascalculator.BerlinGasCalculator;
import org.idnecology.idn.evm.gascalculator.DieHardGasCalculator;
import org.idnecology.idn.evm.gascalculator.IstanbulGasCalculator;
import org.idnecology.idn.evm.gascalculator.LondonGasCalculator;
import org.idnecology.idn.evm.gascalculator.PetersburgGasCalculator;
import org.idnecology.idn.evm.gascalculator.ShanghaiGasCalculator;
import org.idnecology.idn.evm.gascalculator.SpuriousDragonGasCalculator;
import org.idnecology.idn.evm.gascalculator.TangerineWhistleGasCalculator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.processor.ContractCreationProcessor;
import org.idnecology.idn.evm.processor.MessageCallProcessor;
import org.idnecology.idn.evm.worldstate.WorldState;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public class ClassicProtocolSpecs {
  private static final Wei MAX_BLOCK_REWARD = Wei.fromEth(5);

  private ClassicProtocolSpecs() {
    // utility class
  }

  public static ProtocolSpecBuilder classicRecoveryInitDefinition(
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return MainnetProtocolSpecs.homesteadDefinition(
            evmConfiguration, isParallelTxProcessingEnabled, metricsSystem)
        .blockHeaderValidatorBuilder(
            feeMarket -> MainnetBlockHeaderValidator.createClassicValidator())
        .name("ClassicRecoveryInit");
  }

  public static ProtocolSpecBuilder tangerineWhistleDefinition(
      final Optional<BigInteger> chainId,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return MainnetProtocolSpecs.homesteadDefinition(
            evmConfiguration, isParallelTxProcessingEnabled, metricsSystem)
        .isReplayProtectionSupported(true)
        .gasCalculator(TangerineWhistleGasCalculator::new)
        .transactionValidatorFactoryBuilder(
            (evm, gasLimitCalculator, feeMarket) ->
                new TransactionValidatorFactory(
                    evm.getGasCalculator(), gasLimitCalculator, true, chainId))
        .name("ClassicTangerineWhistle");
  }

  public static ProtocolSpecBuilder dieHardDefinition(
      final Optional<BigInteger> chainId,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return tangerineWhistleDefinition(
            chainId, evmConfiguration, isParallelTxProcessingEnabled, metricsSystem)
        .gasCalculator(DieHardGasCalculator::new)
        .difficultyCalculator(ClassicDifficultyCalculators.DIFFICULTY_BOMB_PAUSED)
        .name("DieHard");
  }

  public static ProtocolSpecBuilder gothamDefinition(
      final Optional<BigInteger> chainId,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return dieHardDefinition(
            chainId, evmConfiguration, isParallelTxProcessingEnabled, metricsSystem)
        .blockReward(MAX_BLOCK_REWARD)
        .difficultyCalculator(ClassicDifficultyCalculators.DIFFICULTY_BOMB_DELAYED)
        .blockProcessorBuilder(
            (transactionProcessor,
                transactionReceiptFactory,
                blockReward,
                miningBeneficiaryCalculator,
                skipZeroBlockRewards,
                protocolSchedule) ->
                new ClassicBlockProcessor(
                    transactionProcessor,
                    transactionReceiptFactory,
                    blockReward,
                    miningBeneficiaryCalculator,
                    skipZeroBlockRewards,
                    ecip1017EraRounds,
                    protocolSchedule))
        .name("Gotham");
  }

  public static ProtocolSpecBuilder defuseDifficultyBombDefinition(
      final Optional<BigInteger> chainId,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return gothamDefinition(
            chainId,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .difficultyCalculator(ClassicDifficultyCalculators.DIFFICULTY_BOMB_REMOVED)
        .transactionValidatorFactoryBuilder(
            (evm, gasLimitCalculator, feeMarket) ->
                new TransactionValidatorFactory(
                    evm.getGasCalculator(), gasLimitCalculator, true, chainId))
        .name("DefuseDifficultyBomb");
  }

  public static ProtocolSpecBuilder atlantisDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return gothamDefinition(
            chainId,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .evmBuilder(MainnetEVMs::byzantium)
        .evmConfiguration(evmConfiguration)
        .gasCalculator(SpuriousDragonGasCalculator::new)
        .skipZeroBlockRewards(true)
        .messageCallProcessorBuilder(MessageCallProcessor::new)
        .precompileContractRegistryBuilder(MainnetPrecompiledContractRegistries::byzantium)
        .difficultyCalculator(ClassicDifficultyCalculators.EIP100)
        .transactionReceiptFactory(
            enableRevertReason
                ? ClassicProtocolSpecs::byzantiumTransactionReceiptFactoryWithReasonEnabled
                : ClassicProtocolSpecs::byzantiumTransactionReceiptFactory)
        .contractCreationProcessorBuilder(
            evm ->
                new ContractCreationProcessor(
                    evm, true, Collections.singletonList(MaxCodeSizeRule.from(evm)), 1))
        .transactionProcessorBuilder(
            (gasCalculator,
                feeMarket,
                transactionValidatorFactory,
                contractCreationProcessor,
                messageCallProcessor) ->
                MainnetTransactionProcessor.builder()
                    .gasCalculator(gasCalculator)
                    .transactionValidatorFactory(transactionValidatorFactory)
                    .contractCreationProcessor(contractCreationProcessor)
                    .messageCallProcessor(messageCallProcessor)
                    .clearEmptyAccounts(true)
                    .warmCoinbase(false)
                    .maxStackSize(evmConfiguration.evmStackSize())
                    .feeMarket(feeMarket)
                    .coinbaseFeePriceCalculator(CoinbaseFeePriceCalculator.frontier())
                    .build())
        .name("Atlantis");
  }

  public static ProtocolSpecBuilder aghartaDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return atlantisDefinition(
            chainId,
            enableRevertReason,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .evmBuilder(MainnetEVMs::constantinople)
        .gasCalculator(PetersburgGasCalculator::new)
        .evmBuilder(MainnetEVMs::constantinople)
        .precompileContractRegistryBuilder(MainnetPrecompiledContractRegistries::istanbul)
        .name("Agharta");
  }

  public static ProtocolSpecBuilder phoenixDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return aghartaDefinition(
            chainId,
            enableRevertReason,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .gasCalculator(IstanbulGasCalculator::new)
        .evmBuilder(
            (gasCalculator, evmConfig) ->
                MainnetEVMs.istanbul(
                    gasCalculator, chainId.orElse(BigInteger.ZERO), evmConfiguration))
        .precompileContractRegistryBuilder(MainnetPrecompiledContractRegistries::istanbul)
        .name("Phoenix");
  }

  public static ProtocolSpecBuilder thanosDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return phoenixDefinition(
            chainId,
            enableRevertReason,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .blockHeaderValidatorBuilder(
            feeMarket ->
                MainnetBlockHeaderValidator.createPgaBlockHeaderValidator(
                    new EpochCalculator.Ecip1099EpochCalculator(), powHasher(PowAlgorithm.ETHASH)))
        .ommerHeaderValidatorBuilder(
            feeMarket ->
                MainnetBlockHeaderValidator.createLegacyFeeMarketOmmerValidator(
                    new EpochCalculator.Ecip1099EpochCalculator(), powHasher(PowAlgorithm.ETHASH)))
        .name("Thanos");
  }

  private static TransactionReceipt byzantiumTransactionReceiptFactory(
      // ignored because it's always FRONTIER for byzantium
      final TransactionType __,
      final TransactionProcessingResult result,
      final WorldState worldState,
      final long gasUsed) {
    return new TransactionReceipt(
        result.isSuccessful() ? 1 : 0, gasUsed, result.getLogs(), Optional.empty());
  }

  private static TransactionReceipt byzantiumTransactionReceiptFactoryWithReasonEnabled(
      // ignored because it's always FRONTIER for byzantium
      final TransactionType __,
      final TransactionProcessingResult result,
      final WorldState worldState,
      final long gasUsed) {
    return new TransactionReceipt(
        result.isSuccessful() ? 1 : 0, gasUsed, result.getLogs(), result.getRevertReason());
  }

  public static ProtocolSpecBuilder magnetoDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return thanosDefinition(
            chainId,
            enableRevertReason,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .gasCalculator(BerlinGasCalculator::new)
        .transactionValidatorFactoryBuilder(
            (evm, gasLimitCalculator, feeMarket) ->
                new TransactionValidatorFactory(
                    evm.getGasCalculator(),
                    gasLimitCalculator,
                    true,
                    chainId,
                    Set.of(TransactionType.FRONTIER, TransactionType.ACCESS_LIST)))
        .transactionReceiptFactory(
            enableRevertReason
                ? MainnetProtocolSpecs::berlinTransactionReceiptFactoryWithReasonEnabled
                : MainnetProtocolSpecs::berlinTransactionReceiptFactory)
        .name("Magneto");
  }

  public static ProtocolSpecBuilder mystiqueDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return magnetoDefinition(
            chainId,
            enableRevertReason,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        .gasCalculator(LondonGasCalculator::new)
        .contractCreationProcessorBuilder(
            evm ->
                new ContractCreationProcessor(
                    evm, true, List.of(MaxCodeSizeRule.from(evm), PrefixCodeRule.of()), 1))
        .name("Mystique");
  }

  public static ProtocolSpecBuilder spiralDefinition(
      final Optional<BigInteger> chainId,
      final boolean enableRevertReason,
      final OptionalLong ecip1017EraRounds,
      final EvmConfiguration evmConfiguration,
      final boolean isParallelTxProcessingEnabled,
      final MetricsSystem metricsSystem) {
    return mystiqueDefinition(
            chainId,
            enableRevertReason,
            ecip1017EraRounds,
            evmConfiguration,
            isParallelTxProcessingEnabled,
            metricsSystem)
        // EIP-3860
        .gasCalculator(ShanghaiGasCalculator::new)
        // EIP-3855
        .evmBuilder(
            (gasCalculator, jdCacheConfig) ->
                ClassicEVMs.spiral(
                    gasCalculator, chainId.orElse(BigInteger.ZERO), evmConfiguration))
        // EIP-3651
        .transactionProcessorBuilder(
            (gasCalculator,
                feeMarket,
                transactionValidatorFactory,
                contractCreationProcessor,
                messageCallProcessor) ->
                MainnetTransactionProcessor.builder()
                    .gasCalculator(gasCalculator)
                    .transactionValidatorFactory(transactionValidatorFactory)
                    .contractCreationProcessor(contractCreationProcessor)
                    .messageCallProcessor(messageCallProcessor)
                    .clearEmptyAccounts(true)
                    .warmCoinbase(true)
                    .maxStackSize(evmConfiguration.evmStackSize())
                    .feeMarket(feeMarket)
                    .coinbaseFeePriceCalculator(CoinbaseFeePriceCalculator.frontier())
                    .build())
        .name("Spiral");
  }
}
