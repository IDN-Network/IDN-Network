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
package org.idnecology.idn.consensus.clique.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.consensus.clique.CliqueBlockInterface;
import org.idnecology.idn.consensus.clique.CliqueContext;
import org.idnecology.idn.consensus.clique.CliqueExtraData;
import org.idnecology.idn.consensus.clique.CliqueHelpers;
import org.idnecology.idn.consensus.clique.CliqueProtocolSchedule;
import org.idnecology.idn.consensus.clique.TestHelpers;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.ValidatorVote;
import org.idnecology.idn.consensus.common.validator.VoteProvider;
import org.idnecology.idn.consensus.common.validator.VoteType;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.GenesisState;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.GasPricePendingTransactionsSorter;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;
import org.idnecology.idn.testutil.TestClock;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CliqueBlockCreatorTest {

  private final NodeKey proposerNodeKey = NodeKeyUtils.generate();
  private final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());
  private final KeyPair otherKeyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private final List<Address> validatorList = Lists.newArrayList();
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();
  private final CliqueBlockInterface blockInterface = new CliqueBlockInterface();
  private final EthScheduler ethScheduler = new DeterministicEthScheduler();
  private ProtocolSchedule protocolSchedule;
  private final WorldStateArchive stateArchive = createInMemoryWorldStateArchive();

  private MutableBlockchain blockchain;
  private ProtocolContext protocolContext;
  private EpochManager epochManager;
  private ValidatorProvider validatorProvider;
  private VoteProvider voteProvider;

  @BeforeEach
  void setup() {
    final Address otherAddress = Util.publicKeyToAddress(otherKeyPair.getPublicKey());
    validatorList.add(otherAddress);

    validatorProvider = mock(ValidatorProvider.class);
    voteProvider = mock(VoteProvider.class);
    when(validatorProvider.getVoteProviderAtHead()).thenReturn(Optional.of(voteProvider));
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);

    protocolSchedule =
        CliqueProtocolSchedule.create(
            GenesisConfig.DEFAULT.getConfigOptions(),
            new ForksSchedule<>(List.of()),
            proposerNodeKey,
            PrivacyParameters.DEFAULT,
            false,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    CliqueHelpers.setCliqueContext(cliqueContext);

    final Block genesis =
        GenesisState.fromConfig(GenesisConfig.mainnet(), protocolSchedule).getBlock();
    blockchain = createInMemoryBlockchain(genesis);
    protocolContext =
        new ProtocolContext(blockchain, stateArchive, cliqueContext, new BadBlockManager());
    epochManager = new EpochManager(10);

    // Add a block above the genesis
    final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();
    headerTestFixture.number(1).parentHash(genesis.getHeader().getHash());
    final Block emptyBlock =
        new Block(
            TestHelpers.createCliqueSignedBlockHeader(
                headerTestFixture, otherKeyPair, validatorList),
            new BlockBody(Lists.newArrayList(), Lists.newArrayList()));
    blockchain.appendBlock(emptyBlock, Lists.newArrayList());
  }

  @Test
  public void proposerAddressCanBeExtractFromAConstructedBlock() {

    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);

    final Address coinbase = AddressHelpers.ofValue(1);

    final MiningConfiguration miningConfiguration = createMiningConfiguration(extraData, coinbase);

    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            miningConfiguration,
            parent -> extraData,
            createTransactionPool(),
            protocolContext,
            protocolSchedule,
            proposerNodeKey,
            epochManager,
            ethScheduler);

    final Block createdBlock =
        blockCreator.createBlock(5L, blockchain.getChainHeadHeader()).getBlock();

    assertThat(CliqueHelpers.getProposerOfBlock(createdBlock.getHeader()))
        .isEqualTo(proposerAddress);
  }

  @Test
  public void insertsValidVoteIntoConstructedBlock() {
    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);
    final Address a1 = Address.fromHexString("5");
    final Address coinbase = AddressHelpers.ofValue(1);
    when(voteProvider.getVoteAfterBlock(any(), any()))
        .thenReturn(Optional.of(new ValidatorVote(VoteType.ADD, coinbase, a1)));

    final MiningConfiguration miningConfiguration = createMiningConfiguration(extraData, coinbase);

    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            miningConfiguration,
            parent -> extraData,
            createTransactionPool(),
            protocolContext,
            protocolSchedule,
            proposerNodeKey,
            epochManager,
            ethScheduler);

    final Block createdBlock =
        blockCreator.createBlock(0L, blockchain.getChainHeadHeader()).getBlock();
    assertThat(createdBlock.getHeader().getNonce()).isEqualTo(CliqueBlockInterface.ADD_NONCE);
    assertThat(createdBlock.getHeader().getCoinbase()).isEqualTo(a1);
  }

  @Test
  public void insertsNoVoteWhenAtEpoch() {
    // ensure that the next block is epoch
    epochManager = new EpochManager(1);

    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);
    final Address a1 = Address.fromHexString("5");
    final Address coinbase = AddressHelpers.ofValue(1);

    final VoteProvider mockVoteProvider = mock(VoteProvider.class);
    when(validatorProvider.getVoteProviderAtHead()).thenReturn(Optional.of(mockVoteProvider));
    when(mockVoteProvider.getVoteAfterBlock(any(), any()))
        .thenReturn(Optional.of(new ValidatorVote(VoteType.ADD, coinbase, a1)));

    final MiningConfiguration miningConfiguration = createMiningConfiguration(extraData, coinbase);

    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            miningConfiguration,
            parent -> extraData,
            createTransactionPool(),
            protocolContext,
            protocolSchedule,
            proposerNodeKey,
            epochManager,
            ethScheduler);

    final Block createdBlock =
        blockCreator.createBlock(0L, blockchain.getChainHeadHeader()).getBlock();
    assertThat(createdBlock.getHeader().getNonce()).isEqualTo(CliqueBlockInterface.DROP_NONCE);
    assertThat(createdBlock.getHeader().getCoinbase()).isEqualTo(Address.fromHexString("0"));
  }

  private TransactionPool createTransactionPool() {
    final TransactionPoolConfiguration conf =
        ImmutableTransactionPoolConfiguration.builder().txPoolMaxSize(5).build();
    final EthContext ethContext = mock(EthContext.class, RETURNS_DEEP_STUBS);
    when(ethContext.getEthPeers().subscribeConnect(any())).thenReturn(1L);
    final TransactionPool transactionPool =
        new TransactionPool(
            () ->
                new GasPricePendingTransactionsSorter(
                    conf,
                    TestClock.system(ZoneId.systemDefault()),
                    metricsSystem,
                    blockchain::getChainHeadHeader),
            protocolSchedule,
            protocolContext,
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(metricsSystem),
            conf,
            new BlobCache());
    transactionPool.setEnabled();
    return transactionPool;
  }

  private static MiningConfiguration createMiningConfiguration(
      final Bytes extraData, final Address coinbase) {
    final MiningConfiguration miningConfiguration =
        ImmutableMiningConfiguration.builder()
            .mutableInitValues(
                MutableInitValues.builder()
                    .extraData(extraData)
                    .targetGasLimit(10_000_000L)
                    .minTransactionGasPrice(Wei.ZERO)
                    .coinbase(coinbase)
                    .build())
            .build();
    return miningConfiguration;
  }
}
