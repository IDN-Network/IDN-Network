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
package org.idnecology.idn.consensus.ibft.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.consensus.common.bft.BftContextBuilder.setupContextWithBftExtraDataEncoder;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.BftConfigOptions;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.consensus.common.ForkSpec;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.BaseBftProtocolScheduleBuilder;
import org.idnecology.idn.consensus.common.bft.BftBlockHashing;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftBlockCreator;
import org.idnecology.idn.consensus.ibft.IbftBlockHeaderValidationRulesetFactory;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.GasPricePendingTransactionsSorter;
import org.idnecology.idn.ethereum.mainnet.BlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;
import org.idnecology.idn.testutil.TestClock;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class BftBlockCreatorTest {
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Test
  public void createdBlockPassesValidationRulesAndHasAppropriateHashAndMixHash() {
    // Construct a parent block.
    final BlockHeaderTestFixture blockHeaderBuilder = new BlockHeaderTestFixture();
    blockHeaderBuilder.gasLimit(5000); // required to pass validation rule checks.
    final BlockHeader parentHeader = blockHeaderBuilder.buildHeader();
    final Optional<BlockHeader> optionalHeader = Optional.of(parentHeader);

    // Construct a blockchain and world state
    final MutableBlockchain blockchain = mock(MutableBlockchain.class);
    when(blockchain.getChainHeadHash()).thenReturn(parentHeader.getHash());
    when(blockchain.getBlockHeader(any())).thenReturn(optionalHeader);
    final BlockHeader blockHeader = mock(BlockHeader.class);
    when(blockHeader.getBaseFee()).thenReturn(Optional.empty());
    when(blockchain.getChainHeadHeader()).thenReturn(blockHeader);

    final List<Address> initialValidatorList = Lists.newArrayList();
    for (int i = 0; i < 4; i++) {
      initialValidatorList.add(AddressHelpers.ofValue(i));
    }

    final IbftExtraDataCodec bftExtraDataEncoder = new IbftExtraDataCodec();

    final BaseBftProtocolScheduleBuilder bftProtocolSchedule =
        new BaseBftProtocolScheduleBuilder() {
          @Override
          public BlockHeaderValidator.Builder createBlockHeaderRuleset(
              final BftConfigOptions config, final FeeMarket feeMarket) {
            return IbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(
                Duration.ofSeconds(5), Optional.empty());
          }
        };
    final GenesisConfigOptions configOptions =
        GenesisConfig.fromConfig("{\"config\": {\"spuriousDragonBlock\":0}}").getConfigOptions();
    final ForksSchedule<BftConfigOptions> forksSchedule =
        new ForksSchedule<>(List.of(new ForkSpec<>(0, configOptions.getBftConfigOptions())));
    final ProtocolSchedule protocolSchedule =
        bftProtocolSchedule.createProtocolSchedule(
            configOptions,
            forksSchedule,
            PrivacyParameters.DEFAULT,
            false,
            bftExtraDataEncoder,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());
    final ProtocolContext protContext =
        new ProtocolContext(
            blockchain,
            createInMemoryWorldStateArchive(),
            setupContextWithBftExtraDataEncoder(initialValidatorList, bftExtraDataEncoder),
            new BadBlockManager());

    final TransactionPoolConfiguration poolConf =
        ImmutableTransactionPoolConfiguration.builder().txPoolMaxSize(1).build();

    final GasPricePendingTransactionsSorter pendingTransactions =
        new GasPricePendingTransactionsSorter(
            poolConf,
            TestClock.system(ZoneId.systemDefault()),
            metricsSystem,
            blockchain::getChainHeadHeader);

    final EthContext ethContext = mock(EthContext.class, RETURNS_DEEP_STUBS);
    when(ethContext.getEthPeers().subscribeConnect(any())).thenReturn(1L);

    final TransactionPool transactionPool =
        new TransactionPool(
            () -> pendingTransactions,
            protocolSchedule,
            protContext,
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(metricsSystem),
            poolConf,
            new BlobCache());

    transactionPool.setEnabled();

    final MiningConfiguration miningConfiguration =
        ImmutableMiningConfiguration.builder()
            .mutableInitValues(
                MutableInitValues.builder()
                    .extraData(
                        bftExtraDataEncoder.encode(
                            new BftExtraData(
                                Bytes.wrap(new byte[32]),
                                Collections.emptyList(),
                                Optional.empty(),
                                0,
                                initialValidatorList)))
                    .minTransactionGasPrice(Wei.ZERO)
                    .coinbase(AddressHelpers.ofValue(1))
                    .build())
            .build();

    final BftBlockCreator blockCreator =
        new BftBlockCreator(
            miningConfiguration,
            forksSchedule,
            initialValidatorList.get(0),
            parent ->
                bftExtraDataEncoder.encode(
                    new BftExtraData(
                        Bytes.wrap(new byte[32]),
                        Collections.emptyList(),
                        Optional.empty(),
                        0,
                        initialValidatorList)),
            transactionPool,
            protContext,
            protocolSchedule,
            bftExtraDataEncoder,
            new DeterministicEthScheduler());

    final int secondsBetweenBlocks = 1;
    final Block block =
        blockCreator.createBlock(parentHeader.getTimestamp() + 1, parentHeader).getBlock();

    final BlockHeaderValidator rules =
        IbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(
                Duration.ofSeconds(secondsBetweenBlocks), Optional.empty())
            .build();

    // NOTE: The header will not contain commit seals, so can only do light validation on header.
    final boolean validationResult =
        rules.validateHeader(
            block.getHeader(), parentHeader, protContext, HeaderValidationMode.LIGHT);

    assertThat(validationResult).isTrue();

    final BlockHeader header = block.getHeader();
    final BftExtraData extraData = bftExtraDataEncoder.decode(header);
    assertThat(block.getHash())
        .isEqualTo(
            new BftBlockHashing(bftExtraDataEncoder)
                .calculateDataHashForCommittedSeal(header, extraData));
  }
}
