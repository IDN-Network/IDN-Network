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
package org.idnecology.idn.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.config.CheckpointConfigOptions;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.GasLimitCalculator;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.GenesisState;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalLong;

import com.google.common.collect.Range;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MergeIdnControllerBuilderTest {

  private MergeIdnControllerBuilder idnControllerBuilder;
  private static final NodeKey nodeKey = NodeKeyUtils.generate();

  @Mock GenesisConfig genesisConfig;
  @Mock GenesisConfigOptions genesisConfigOptions;
  @Mock SynchronizerConfiguration synchronizerConfiguration;
  @Mock EthProtocolConfiguration ethProtocolConfiguration;
  @Mock CheckpointConfigOptions checkpointConfigOptions;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MiningConfiguration miningConfiguration;

  @Mock PrivacyParameters privacyParameters;
  @Mock Clock clock;
  @Mock StorageProvider storageProvider;
  @Mock GasLimitCalculator gasLimitCalculator;
  @Mock WorldStatePreimageStorage worldStatePreimageStorage;

  BigInteger networkId = BigInteger.ONE;
  private final BlockHeaderTestFixture headerGenerator = new BlockHeaderTestFixture();
  private final BaseFeeMarket feeMarket = FeeMarket.london(0, Optional.of(Wei.of(42)));
  private final TransactionPoolConfiguration poolConfiguration =
      TransactionPoolConfiguration.DEFAULT;
  private final ObservableMetricsSystem observableMetricsSystem = new NoOpMetricsSystem();

  @TempDir Path tempDir;

  @BeforeEach
  public void setup() {

    final ForestWorldStateKeyValueStorage worldStateKeyValueStorage =
        mock(ForestWorldStateKeyValueStorage.class);
    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateKeyValueStorage);

    lenient().when(genesisConfig.getParentHash()).thenReturn(Hash.ZERO.toHexString());
    lenient().when(genesisConfig.getDifficulty()).thenReturn(Bytes.of(0).toHexString());
    lenient().when(genesisConfig.getExtraData()).thenReturn(Bytes.EMPTY.toHexString());
    lenient().when(genesisConfig.getMixHash()).thenReturn(Hash.ZERO.toHexString());
    lenient().when(genesisConfig.getNonce()).thenReturn(Long.toHexString(1));
    lenient().when(genesisConfig.getConfigOptions()).thenReturn(genesisConfigOptions);
    lenient().when(genesisConfigOptions.getCheckpointOptions()).thenReturn(checkpointConfigOptions);
    when(genesisConfigOptions.getTerminalTotalDifficulty())
        .thenReturn((Optional.of(UInt256.valueOf(100L))));
    when(genesisConfigOptions.getThanosBlockNumber()).thenReturn(OptionalLong.empty());
    when(genesisConfigOptions.getTerminalBlockHash()).thenReturn(Optional.of(Hash.ZERO));
    lenient().when(genesisConfigOptions.getTerminalBlockNumber()).thenReturn(OptionalLong.of(1L));
    lenient()
        .when(storageProvider.createBlockchainStorage(any(), any(), any()))
        .thenReturn(
            new KeyValueStoragePrefixedKeyBlockchainStorage(
                new InMemoryKeyValueStorage(),
                new VariablesKeyValueStorage(new InMemoryKeyValueStorage()),
                new MainnetBlockHeaderFunctions(),
                false));
    lenient()
        .when(storageProvider.getStorageBySegmentIdentifier(any()))
        .thenReturn(new InMemoryKeyValueStorage());
    lenient().when(synchronizerConfiguration.getDownloaderParallelism()).thenReturn(1);
    lenient().when(synchronizerConfiguration.getTransactionsParallelism()).thenReturn(1);
    lenient().when(synchronizerConfiguration.getComputationParallelism()).thenReturn(1);

    lenient()
        .when(synchronizerConfiguration.getBlockPropagationRange())
        .thenReturn(Range.closed(1L, 2L));

    lenient()
        .when(
            storageProvider.createWorldStateStorageCoordinator(
                DataStorageConfiguration.DEFAULT_FOREST_CONFIG))
        .thenReturn(worldStateStorageCoordinator);
    lenient()
        .when(storageProvider.createWorldStatePreimageStorage())
        .thenReturn(worldStatePreimageStorage);

    lenient().when(worldStateKeyValueStorage.isWorldStateAvailable(any())).thenReturn(true);
    lenient()
        .when(worldStatePreimageStorage.updater())
        .thenReturn(mock(WorldStatePreimageStorage.Updater.class));
    lenient()
        .when(worldStateKeyValueStorage.updater())
        .thenReturn(mock(ForestWorldStateKeyValueStorage.Updater.class));
    lenient().when(miningConfiguration.getTargetGasLimit()).thenReturn(OptionalLong.empty());

    idnControllerBuilder = visitWithMockConfigs(new MergeIdnControllerBuilder());
  }

  MergeIdnControllerBuilder visitWithMockConfigs(final MergeIdnControllerBuilder builder) {
    return (MergeIdnControllerBuilder)
        builder
            .genesisConfig(genesisConfig)
            .synchronizerConfiguration(synchronizerConfiguration)
            .ethProtocolConfiguration(ethProtocolConfiguration)
            .miningParameters(miningConfiguration)
            .metricsSystem(observableMetricsSystem)
            .privacyParameters(privacyParameters)
            .dataDirectory(tempDir)
            .clock(clock)
            .transactionPoolConfiguration(poolConfiguration)
            .dataStorageConfiguration(DataStorageConfiguration.DEFAULT_FOREST_CONFIG)
            .nodeKey(nodeKey)
            .storageProvider(storageProvider)
            .evmConfiguration(EvmConfiguration.DEFAULT)
            .networkConfiguration(NetworkingConfiguration.create())
            .idnComponent(mock(IdnComponent.class))
            .networkId(networkId)
            .apiConfiguration(ImmutableApiConfiguration.builder().build());
  }

  @Test
  public void assertTerminalTotalDifficultyInMergeContext() {
    when(genesisConfigOptions.getTerminalTotalDifficulty())
        .thenReturn(Optional.of(UInt256.valueOf(1500L)));

    final Difficulty terminalTotalDifficulty =
        visitWithMockConfigs(new MergeIdnControllerBuilder())
            .build()
            .getProtocolContext()
            .getConsensusContext(MergeContext.class)
            .getTerminalTotalDifficulty();

    assertThat(terminalTotalDifficulty).isEqualTo(Difficulty.of(1500L));
  }

  @Test
  public void assertConfiguredBlock() {
    final Blockchain mockChain = mock(Blockchain.class);
    when(mockChain.getBlockHeader(anyLong())).thenReturn(Optional.of(mock(BlockHeader.class)));
    final MergeContext mergeContext =
        idnControllerBuilder.createConsensusContext(
            mockChain,
            mock(WorldStateArchive.class),
            this.idnControllerBuilder.createProtocolSchedule());
    assertThat(mergeContext).isNotNull();
    assertThat(mergeContext.getTerminalPoWBlock()).isPresent();
  }

  @Test
  public void assertBuiltContextMonitorsTTD() {
    final GenesisState genesisState =
        GenesisState.fromConfig(genesisConfig, this.idnControllerBuilder.createProtocolSchedule());
    final MutableBlockchain blockchain = createInMemoryBlockchain(genesisState.getBlock());
    final MergeContext mergeContext =
        spy(
            idnControllerBuilder.createConsensusContext(
                blockchain,
                mock(WorldStateArchive.class),
                this.idnControllerBuilder.createProtocolSchedule()));
    assertThat(mergeContext).isNotNull();
    final Difficulty over = Difficulty.of(10000L);
    final Difficulty under = Difficulty.of(10L);

    final BlockHeader parent =
        headerGenerator
            .difficulty(under)
            .parentHash(genesisState.getBlock().getHash())
            .number(genesisState.getBlock().getHeader().getNumber() + 1)
            .gasLimit(genesisState.getBlock().getHeader().getGasLimit())
            .stateRoot(genesisState.getBlock().getHeader().getStateRoot())
            .buildHeader();
    blockchain.appendBlock(new Block(parent, BlockBody.empty()), Collections.emptyList());

    final BlockHeader terminal =
        headerGenerator
            .difficulty(over)
            .parentHash(parent.getHash())
            .number(parent.getNumber() + 1)
            .gasLimit(parent.getGasLimit())
            .stateRoot(parent.getStateRoot())
            .buildHeader();

    blockchain.appendBlock(new Block(terminal, BlockBody.empty()), Collections.emptyList());
    assertThat(mergeContext.isPostMerge()).isTrue();
  }

  @Test
  public void assertNoFinalizedBlockWhenNotStored() {
    final Blockchain mockChain = mock(Blockchain.class);
    when(mockChain.getFinalized()).thenReturn(Optional.empty());
    final MergeContext mergeContext =
        idnControllerBuilder.createConsensusContext(
            mockChain,
            mock(WorldStateArchive.class),
            this.idnControllerBuilder.createProtocolSchedule());
    assertThat(mergeContext).isNotNull();
    assertThat(mergeContext.getFinalized()).isEmpty();
  }

  @Test
  public void assertFinalizedBlockIsPresentWhenStored() {
    final BlockHeader finalizedHeader = finalizedBlockHeader();

    final Blockchain mockChain = mock(Blockchain.class);
    when(mockChain.getFinalized()).thenReturn(Optional.of(finalizedHeader.getHash()));
    when(mockChain.getBlockHeader(finalizedHeader.getHash()))
        .thenReturn(Optional.of(finalizedHeader));
    final MergeContext mergeContext =
        idnControllerBuilder.createConsensusContext(
            mockChain,
            mock(WorldStateArchive.class),
            this.idnControllerBuilder.createProtocolSchedule());
    assertThat(mergeContext).isNotNull();
    assertThat(mergeContext.getFinalized().get()).isEqualTo(finalizedHeader);
  }

  private BlockHeader finalizedBlockHeader() {
    final long blockNumber = 42;
    final Hash magicHash = Hash.wrap(Bytes32.leftPad(Bytes.ofUnsignedInt(42)));

    return headerGenerator
        .difficulty(Difficulty.MAX_VALUE)
        .parentHash(magicHash)
        .number(blockNumber)
        .baseFeePerGas(feeMarket.computeBaseFee(blockNumber, Wei.of(0x3b9aca00), 0, 15000000l))
        .gasLimit(30000000l)
        .stateRoot(magicHash)
        .buildHeader();
  }
}
