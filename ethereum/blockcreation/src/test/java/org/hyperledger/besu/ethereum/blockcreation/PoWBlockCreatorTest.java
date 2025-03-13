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
package org.idnecology.idn.ethereum.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.blockcreation.BlockCreator.BlockCreationResult;
import org.idnecology.idn.ethereum.blockcreation.txselection.TransactionSelectionResults;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ExecutionContextTestFixture;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.difficulty.fixed.FixedDifficultyCalculators;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.BaseFeePendingTransactionsSorter;
import org.idnecology.idn.ethereum.mainnet.EpochCalculator;
import org.idnecology.idn.ethereum.mainnet.PoWHasher;
import org.idnecology.idn.ethereum.mainnet.PoWSolver;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.ethereum.mainnet.ValidationTestUtils;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.testutil.TestClock;
import org.idnecology.idn.util.Subscribers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class PoWBlockCreatorTest extends AbstractBlockCreatorTest {

  private final Address BLOCK_1_COINBASE =
      Address.fromHexString("0x05a56e2d52c817161883f50c441c3228cfe54d9f");

  private static final long BLOCK_1_TIMESTAMP = Long.parseUnsignedLong("55ba4224", 16);

  private static final long BLOCK_1_NONCE = Long.parseLong("539bd4979fef1ec4", 16);
  private static final long FIXED_DIFFICULTY_NONCE = 26;

  private static final Bytes BLOCK_1_EXTRA_DATA =
      Bytes.fromHexString("0x476574682f76312e302e302f6c696e75782f676f312e342e32");
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Test
  void createMainnetBlock1() throws IOException {
    final var genesisConfig = GenesisConfig.mainnet();

    final MiningConfiguration miningConfiguration = createMiningParameters(BLOCK_1_NONCE);

    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder(genesisConfig)
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfig.getConfigOptions(),
                        Optional.of(BigInteger.valueOf(42)),
                        ProtocolSpecAdapters.create(0, Function.identity()),
                        PrivacyParameters.DEFAULT,
                        false,
                        EvmConfiguration.DEFAULT,
                        MiningConfiguration.MINING_DISABLED,
                        new BadBlockManager(),
                        false,
                        new NoOpMetricsSystem())
                    .createProtocolSchedule())
            .build();

    final PoWSolver solver =
        new PoWSolver(
            miningConfiguration,
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final TransactionPool transactionPool = createTransactionPool(executionContextTestFixture);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            miningConfiguration,
            parent -> BLOCK_1_EXTRA_DATA,
            transactionPool,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            solver,
            ethScheduler);

    // A Hashrate should not exist in the block creator prior to creating a block
    assertThat(blockCreator.getHashesPerSecond()).isNotPresent();

    final BlockCreationResult blockResult =
        blockCreator.createBlock(
            BLOCK_1_TIMESTAMP, executionContextTestFixture.getBlockchain().getChainHeadHeader());
    final Block actualBlock = blockResult.getBlock();
    final Block expectedBlock = ValidationTestUtils.readBlock(1);

    assertThat(actualBlock).isEqualTo(expectedBlock);
    assertThat(blockCreator.getHashesPerSecond()).isPresent();
    assertThat(blockResult.getTransactionSelectionResults())
        .isEqualTo(new TransactionSelectionResults());
  }

  @Test
  void createMainnetBlock1_fixedDifficulty1() {
    final var genesisConfig =
        GenesisConfig.fromResource("/block-creation-fixed-difficulty-genesis.json");

    final MiningConfiguration miningConfiguration = createMiningParameters(FIXED_DIFFICULTY_NONCE);

    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder(genesisConfig)
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfig.getConfigOptions(),
                        Optional.of(BigInteger.valueOf(42)),
                        ProtocolSpecAdapters.create(
                            0,
                            specBuilder ->
                                specBuilder.difficultyCalculator(
                                    FixedDifficultyCalculators.calculator(
                                        genesisConfig.getConfigOptions()))),
                        PrivacyParameters.DEFAULT,
                        false,
                        EvmConfiguration.DEFAULT,
                        MiningConfiguration.MINING_DISABLED,
                        new BadBlockManager(),
                        false,
                        new NoOpMetricsSystem())
                    .createProtocolSchedule())
            .build();

    final PoWSolver solver =
        new PoWSolver(
            miningConfiguration,
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final TransactionPool transactionPool = createTransactionPool(executionContextTestFixture);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            miningConfiguration,
            parent -> BLOCK_1_EXTRA_DATA,
            transactionPool,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            solver,
            ethScheduler);

    assertThat(
            blockCreator.createBlock(
                BLOCK_1_TIMESTAMP,
                executionContextTestFixture.getBlockchain().getChainHeadHeader()))
        .isNotNull();
    // If we weren't setting difficulty to 2^256-1 a difficulty of 1 would have caused a
    // IllegalArgumentException at the previous line, as 2^256 is 33 bytes.
  }

  @Test
  void rewardBeneficiary_zeroReward_skipZeroRewardsFalse() {
    final var genesisConfig =
        GenesisConfig.fromResource("/block-creation-fixed-difficulty-genesis.json");

    final MiningConfiguration miningConfiguration = createMiningParameters(FIXED_DIFFICULTY_NONCE);

    ProtocolSchedule protocolSchedule =
        new ProtocolScheduleBuilder(
                genesisConfig.getConfigOptions(),
                Optional.of(BigInteger.valueOf(42)),
                ProtocolSpecAdapters.create(
                    0,
                    specBuilder ->
                        specBuilder.difficultyCalculator(
                            FixedDifficultyCalculators.calculator(
                                genesisConfig.getConfigOptions()))),
                PrivacyParameters.DEFAULT,
                false,
                EvmConfiguration.DEFAULT,
                MiningConfiguration.MINING_DISABLED,
                new BadBlockManager(),
                false,
                new NoOpMetricsSystem())
            .createProtocolSchedule();
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder(genesisConfig)
            .protocolSchedule(protocolSchedule)
            .build();

    final PoWSolver solver =
        new PoWSolver(
            miningConfiguration,
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final TransactionPool transactionPool = createTransactionPool(executionContextTestFixture);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            miningConfiguration,
            parent -> BLOCK_1_EXTRA_DATA,
            transactionPool,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            solver,
            ethScheduler);

    final MutableWorldState mutableWorldState =
        executionContextTestFixture.getStateArchive().getWorldState();
    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNull();

    final ProcessableBlockHeader header =
        BlockHeaderBuilder.create()
            .parentHash(Hash.ZERO)
            .coinbase(BLOCK_1_COINBASE)
            .difficulty(Difficulty.ONE)
            .number(1)
            .gasLimit(1)
            .timestamp(1)
            .buildProcessableBlockHeader();

    blockCreator.rewardBeneficiary(
        mutableWorldState,
        header,
        Collections.emptyList(),
        BLOCK_1_COINBASE,
        Wei.ZERO,
        false,
        protocolSchedule.getByBlockHeader(header));

    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNotNull();
    assertThat(mutableWorldState.get(BLOCK_1_COINBASE).getBalance()).isEqualTo(Wei.ZERO);
  }

  @Test
  void rewardBeneficiary_zeroReward_skipZeroRewardsTrue() {
    final var genesisConfig =
        GenesisConfig.fromResource("/block-creation-fixed-difficulty-genesis.json");

    final MiningConfiguration miningConfiguration = createMiningParameters(FIXED_DIFFICULTY_NONCE);

    ProtocolSchedule protocolSchedule =
        new ProtocolScheduleBuilder(
                genesisConfig.getConfigOptions(),
                Optional.of(BigInteger.valueOf(42)),
                ProtocolSpecAdapters.create(
                    0,
                    specBuilder ->
                        specBuilder.difficultyCalculator(
                            FixedDifficultyCalculators.calculator(
                                genesisConfig.getConfigOptions()))),
                PrivacyParameters.DEFAULT,
                false,
                EvmConfiguration.DEFAULT,
                MiningConfiguration.MINING_DISABLED,
                new BadBlockManager(),
                false,
                new NoOpMetricsSystem())
            .createProtocolSchedule();
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder(genesisConfig)
            .protocolSchedule(protocolSchedule)
            .build();

    final PoWSolver solver =
        new PoWSolver(
            miningConfiguration,
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final TransactionPool transactionPool = createTransactionPool(executionContextTestFixture);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            miningConfiguration,
            parent -> BLOCK_1_EXTRA_DATA,
            transactionPool,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            solver,
            ethScheduler);

    final MutableWorldState mutableWorldState =
        executionContextTestFixture.getStateArchive().getWorldState();
    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNull();

    final ProcessableBlockHeader header =
        BlockHeaderBuilder.create()
            .parentHash(Hash.ZERO)
            .coinbase(BLOCK_1_COINBASE)
            .difficulty(Difficulty.ONE)
            .number(1)
            .gasLimit(1)
            .timestamp(1)
            .buildProcessableBlockHeader();

    blockCreator.rewardBeneficiary(
        mutableWorldState,
        header,
        Collections.emptyList(),
        BLOCK_1_COINBASE,
        Wei.ZERO,
        true,
        protocolSchedule.getByBlockHeader(header));

    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNull();
  }

  private TransactionPool createTransactionPool(
      final ExecutionContextTestFixture executionContextTestFixture) {
    final TransactionPoolConfiguration poolConf =
        ImmutableTransactionPoolConfiguration.builder().txPoolMaxSize(1).build();

    final BaseFeePendingTransactionsSorter pendingTransactions =
        new BaseFeePendingTransactionsSorter(
            poolConf,
            TestClock.fixed(),
            metricsSystem,
            executionContextTestFixture.getProtocolContext().getBlockchain()::getChainHeadHeader);

    final EthContext ethContext = mock(EthContext.class, RETURNS_DEEP_STUBS);
    when(ethContext.getEthPeers().subscribeConnect(any())).thenReturn(1L);

    final TransactionPool transactionPool =
        new TransactionPool(
            () -> pendingTransactions,
            executionContextTestFixture.getProtocolSchedule(),
            executionContextTestFixture.getProtocolContext(),
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(metricsSystem),
            poolConf,
            new BlobCache());
    transactionPool.setEnabled();

    return transactionPool;
  }

  private MiningConfiguration createMiningParameters(final long nonce) {
    return ImmutableMiningConfiguration.builder()
        .mutableInitValues(
            MutableInitValues.builder()
                .nonceGenerator(Lists.newArrayList(nonce))
                //                .nonceGenerator(new IncrementingNonceGenerator(0))
                .extraData(BLOCK_1_EXTRA_DATA)
                .minTransactionGasPrice(Wei.ONE)
                .coinbase(BLOCK_1_COINBASE)
                .build())
        .build();
  }
}
