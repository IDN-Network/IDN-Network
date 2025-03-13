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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.config.CheckpointConfigOptions;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.ImmutableCliqueConfigOptions;
import org.idnecology.idn.config.TransitionsConfigOptions;
import org.idnecology.idn.consensus.clique.CliqueBlockHeaderFunctions;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.log.LogsBloomFilter;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Range;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CliqueIdnControllerBuilderTest {

  private IdnControllerBuilder cliqueIdnControllerBuilder;

  @Mock private GenesisConfig genesisConfig;
  @Mock private GenesisConfigOptions genesisConfigOptions;
  @Mock private SynchronizerConfiguration synchronizerConfiguration;
  @Mock private EthProtocolConfiguration ethProtocolConfiguration;
  @Mock private CheckpointConfigOptions checkpointConfigOptions;
  @Mock private PrivacyParameters privacyParameters;
  @Mock private Clock clock;
  @Mock private StorageProvider storageProvider;
  @Mock private WorldStatePreimageStorage worldStatePreimageStorage;
  private static final BigInteger networkId = BigInteger.ONE;
  private static final NodeKey nodeKey = NodeKeyUtils.generate();
  private final TransactionPoolConfiguration poolConfiguration =
      TransactionPoolConfiguration.DEFAULT;
  private final ObservableMetricsSystem observableMetricsSystem = new NoOpMetricsSystem();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final MiningConfiguration miningConfiguration = MiningConfiguration.newDefault();

  @TempDir Path tempDir;

  @BeforeEach
  public void setup() throws JsonProcessingException {
    // Clique Idn controller setup
    final ForestWorldStateKeyValueStorage worldStateKeyValueStorage =
        mock(ForestWorldStateKeyValueStorage.class);
    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateKeyValueStorage);

    lenient().when(genesisConfig.getParentHash()).thenReturn(Hash.ZERO.toHexString());
    lenient().when(genesisConfig.getDifficulty()).thenReturn(Bytes.of(0).toHexString());
    when(genesisConfig.getExtraData())
        .thenReturn(
            "0x0000000000000000000000000000000000000000000000000000000000000000b9b81ee349c3807e46bc71aa2632203c5b4620340000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
    lenient().when(genesisConfig.getMixHash()).thenReturn(Hash.ZERO.toHexString());
    lenient().when(genesisConfig.getNonce()).thenReturn(Long.toHexString(1));
    lenient().when(genesisConfig.getConfigOptions()).thenReturn(genesisConfigOptions);
    lenient().when(genesisConfigOptions.getCheckpointOptions()).thenReturn(checkpointConfigOptions);
    lenient()
        .when(storageProvider.createBlockchainStorage(any(), any(), any()))
        .thenReturn(
            new KeyValueStoragePrefixedKeyBlockchainStorage(
                new InMemoryKeyValueStorage(),
                new VariablesKeyValueStorage(new InMemoryKeyValueStorage()),
                new MainnetBlockHeaderFunctions(),
                false));
    lenient()
        .when(
            storageProvider.createWorldStateStorageCoordinator(
                DataStorageConfiguration.DEFAULT_FOREST_CONFIG))
        .thenReturn(worldStateStorageCoordinator);
    lenient().when(worldStateKeyValueStorage.isWorldStateAvailable(any())).thenReturn(true);
    lenient()
        .when(worldStateKeyValueStorage.updater())
        .thenReturn(mock(ForestWorldStateKeyValueStorage.Updater.class));
    lenient()
        .when(worldStatePreimageStorage.updater())
        .thenReturn(mock(WorldStatePreimageStorage.Updater.class));
    lenient()
        .when(storageProvider.createWorldStatePreimageStorage())
        .thenReturn(worldStatePreimageStorage);
    lenient().when(synchronizerConfiguration.getDownloaderParallelism()).thenReturn(1);
    lenient().when(synchronizerConfiguration.getTransactionsParallelism()).thenReturn(1);
    lenient().when(synchronizerConfiguration.getComputationParallelism()).thenReturn(1);

    lenient()
        .when(synchronizerConfiguration.getBlockPropagationRange())
        .thenReturn(Range.closed(1L, 2L));

    // clique prepForBuild setup
    lenient()
        .when(genesisConfigOptions.getCliqueConfigOptions())
        .thenReturn(
            ImmutableCliqueConfigOptions.builder()
                .epochLength(30)
                .createEmptyBlocks(true)
                .blockPeriodSeconds(1)
                .build());

    final var jsonTransitions =
        (ObjectNode)
            objectMapper.readTree(
                """
                    {"clique": [
                      {
                                "block": 2,
                                "blockperiodseconds": 2
                      }
                    ]}
                    """);

    lenient()
        .when(genesisConfigOptions.getTransitions())
        .thenReturn(new TransitionsConfigOptions(jsonTransitions));

    cliqueIdnControllerBuilder =
        new CliqueIdnControllerBuilder()
            .genesisConfig(genesisConfig)
            .synchronizerConfiguration(synchronizerConfiguration)
            .ethProtocolConfiguration(ethProtocolConfiguration)
            .networkId(networkId)
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
            .idnComponent(mock(IdnComponent.class))
            .networkConfiguration(NetworkingConfiguration.create())
            .apiConfiguration(ImmutableApiConfiguration.builder().build());
  }

  @Test
  public void miningParametersBlockPeriodSecondsIsUpdatedOnTransition() {
    final var idnController = cliqueIdnControllerBuilder.build();
    final var protocolContext = idnController.getProtocolContext();

    final BlockHeader header1 =
        new BlockHeader(
            protocolContext.getBlockchain().getChainHeadHash(),
            Hash.EMPTY_TRIE_HASH,
            Address.ZERO,
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY_TRIE_HASH,
            LogsBloomFilter.builder().build(),
            Difficulty.ONE,
            1,
            0,
            0,
            0,
            Bytes.EMPTY,
            Wei.ZERO,
            Hash.EMPTY,
            0,
            null,
            null,
            null,
            null,
            null,
            new CliqueBlockHeaderFunctions());
    final Block block1 = new Block(header1, BlockBody.empty());

    protocolContext.getBlockchain().appendBlock(block1, List.of());

    assertThat(miningConfiguration.getBlockPeriodSeconds()).isNotEmpty().hasValue(2);
    assertThat(miningConfiguration.getBlockTxsSelectionMaxTime()).isEqualTo(2000 * 75 / 100);
  }
}
