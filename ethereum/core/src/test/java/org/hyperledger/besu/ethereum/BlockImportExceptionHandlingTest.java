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
package org.idnecology.idn.ethereum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.trie.diffbased.common.worldview.WorldStateConfig.createStatefulConfigWithTrie;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.mainnet.AbstractBlockProcessor;
import org.idnecology.idn.ethereum.mainnet.BlockBodyValidator;
import org.idnecology.idn.ethereum.mainnet.BlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.BlockProcessor;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockProcessor;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.mainnet.blockhash.FrontierBlockHashProcessor;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.BonsaiWorldStateProvider;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.worldview.BonsaiWorldState;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.exception.StorageException;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BlockImportExceptionHandlingTest {

  private final MainnetTransactionProcessor transactionProcessor =
      mock(MainnetTransactionProcessor.class);
  private final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory =
      mock(AbstractBlockProcessor.TransactionReceiptFactory.class);

  private final ProtocolSchedule protocolSchedule = mock(ProtocolSchedule.class);
  private final BlockProcessor blockProcessor =
      new MainnetBlockProcessor(
          transactionProcessor,
          transactionReceiptFactory,
          Wei.ZERO,
          BlockHeader::getCoinbase,
          true,
          protocolSchedule);
  private final BlockHeaderValidator blockHeaderValidator = mock(BlockHeaderValidator.class);
  private final BlockBodyValidator blockBodyValidator = mock(BlockBodyValidator.class);
  private final ProtocolContext protocolContext = mock(ProtocolContext.class);
  private final ProtocolSpec protocolSpec = mock(ProtocolSpec.class);
  private final GasCalculator gasCalculator = mock(GasCalculator.class);
  private final FeeMarket feeMarket = mock(FeeMarket.class);
  protected final MutableBlockchain blockchain = mock(MutableBlockchain.class);
  private final StorageProvider storageProvider = new InMemoryKeyValueStorageProvider();

  private final WorldStateStorageCoordinator worldStateStorageCoordinator =
      new WorldStateStorageCoordinator(
          new BonsaiWorldStateKeyValueStorage(
              storageProvider,
              new NoOpMetricsSystem(),
              DataStorageConfiguration.DEFAULT_BONSAI_CONFIG));

  private final WorldStateArchive worldStateArchive =
      // contains a BonsaiWorldState which we need to spy on.
      // do we need to also test with a DefaultWorldStateArchive?
      spy(InMemoryKeyValueStorageProvider.createBonsaiInMemoryWorldStateArchive(blockchain));

  private final BonsaiWorldState persisted =
      spy(
          new BonsaiWorldState(
              (BonsaiWorldStateProvider) worldStateArchive,
              (BonsaiWorldStateKeyValueStorage)
                  worldStateStorageCoordinator.worldStateKeyValueStorage(),
              EvmConfiguration.DEFAULT,
              createStatefulConfigWithTrie()));

  private final BadBlockManager badBlockManager = new BadBlockManager();

  private MainnetBlockValidator mainnetBlockValidator;

  @BeforeEach
  public void setup() {
    when(protocolContext.getBlockchain()).thenReturn(blockchain);
    when(protocolContext.getWorldStateArchive()).thenReturn(worldStateArchive);
    when(protocolSchedule.getByBlockHeader(any())).thenReturn(protocolSpec);
    when(protocolSpec.getBlockHashProcessor()).thenReturn(new FrontierBlockHashProcessor());
    when(protocolSpec.getGasCalculator()).thenReturn(gasCalculator);
    when(protocolSpec.getFeeMarket()).thenReturn(feeMarket);
    mainnetBlockValidator =
        new MainnetBlockValidator(
            blockHeaderValidator, blockBodyValidator, blockProcessor, badBlockManager);
  }

  @Test
  void shouldNotBadBlockWhenInternalErrorDuringPersisting() {

    Mockito.doThrow(new StorageException("database problem")).when(persisted).persist(any());
    Mockito.doReturn(persisted).when(worldStateArchive).getWorldState();
    Mockito.doReturn(Optional.of(persisted)).when(worldStateArchive).getWorldState(any());

    Block goodBlock =
        new BlockDataGenerator()
            .block(
                BlockDataGenerator.BlockOptions.create()
                    .setBlockNumber(0)
                    .hasTransactions(false)
                    .setBlockHeaderFunctions(new MainnetBlockHeaderFunctions()));

    when(blockchain.getBlockHeader(any(Hash.class)))
        .thenReturn(Optional.of(new BlockHeaderTestFixture().buildHeader()));
    when(blockHeaderValidator.validateHeader(
            any(BlockHeader.class),
            any(BlockHeader.class),
            eq(protocolContext),
            eq(HeaderValidationMode.DETACHED_ONLY)))
        .thenReturn(true);

    when(blockBodyValidator.validateBody(
            eq(protocolContext),
            eq(goodBlock),
            any(),
            any(),
            eq(HeaderValidationMode.DETACHED_ONLY),
            any()))
        .thenReturn(true);
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
    mainnetBlockValidator.validateAndProcessBlock(
        protocolContext,
        goodBlock,
        HeaderValidationMode.DETACHED_ONLY,
        HeaderValidationMode.DETACHED_ONLY);
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
  }

  @Test
  void shouldNotBadBlockWhenInternalErrorOnBlockLookup() {

    Block goodBlock =
        new BlockDataGenerator()
            .block(
                BlockDataGenerator.BlockOptions.create()
                    .setBlockNumber(0)
                    .hasTransactions(false)
                    .setBlockHeaderFunctions(new MainnetBlockHeaderFunctions()));

    when(blockchain.getBlockHeader(any(Hash.class)))
        .thenThrow(new StorageException("database problem"));
    when(blockHeaderValidator.validateHeader(
            any(BlockHeader.class),
            any(BlockHeader.class),
            eq(protocolContext),
            eq(HeaderValidationMode.DETACHED_ONLY)))
        .thenReturn(true);

    when(blockBodyValidator.validateBody(
            eq(protocolContext),
            eq(goodBlock),
            any(),
            any(),
            eq(HeaderValidationMode.DETACHED_ONLY),
            any()))
        .thenReturn(true);
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
    mainnetBlockValidator.validateAndProcessBlock(
        protocolContext,
        goodBlock,
        HeaderValidationMode.DETACHED_ONLY,
        HeaderValidationMode.DETACHED_ONLY);
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
  }

  @Test
  void shouldNotBadBlockWhenInternalErrorDuringValidateHeader() {

    Block goodBlock =
        new BlockDataGenerator()
            .block(
                BlockDataGenerator.BlockOptions.create()
                    .setBlockNumber(0)
                    .hasTransactions(false)
                    .setBlockHeaderFunctions(new MainnetBlockHeaderFunctions()));

    when(blockchain.getBlockHeader(any(Hash.class)))
        .thenReturn(Optional.of(new BlockHeaderTestFixture().buildHeader()));
    when(blockHeaderValidator.validateHeader(
            any(BlockHeader.class),
            any(BlockHeader.class),
            eq(protocolContext),
            eq(HeaderValidationMode.DETACHED_ONLY)))
        .thenThrow(new StorageException("database problem"));

    assertThat(badBlockManager.getBadBlocks()).isEmpty();
    mainnetBlockValidator.validateAndProcessBlock(
        protocolContext,
        goodBlock,
        HeaderValidationMode.DETACHED_ONLY,
        HeaderValidationMode.DETACHED_ONLY);
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
  }

  @Test
  void shouldNotBadBlockWhenInternalErrorDuringValidateBody() {
    Mockito.doNothing().when(persisted).persist(any());
    Mockito.doReturn(persisted).when(worldStateArchive).getWorldState();
    Mockito.doReturn(Optional.of(persisted)).when(worldStateArchive).getWorldState(any());

    Block goodBlock =
        new BlockDataGenerator()
            .block(
                BlockDataGenerator.BlockOptions.create()
                    .setBlockNumber(0)
                    .hasTransactions(false)
                    .setBlockHeaderFunctions(new MainnetBlockHeaderFunctions()));

    when(blockchain.getBlockHeader(any(Hash.class)))
        .thenReturn(Optional.of(new BlockHeaderTestFixture().buildHeader()));
    when(blockHeaderValidator.validateHeader(
            any(BlockHeader.class),
            any(BlockHeader.class),
            eq(protocolContext),
            eq(HeaderValidationMode.DETACHED_ONLY)))
        .thenReturn(true);

    when(blockBodyValidator.validateBody(
            eq(protocolContext),
            eq(goodBlock),
            any(),
            any(),
            eq(HeaderValidationMode.DETACHED_ONLY),
            any()))
        .thenThrow(new StorageException("database problem"));
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
    mainnetBlockValidator.validateAndProcessBlock(
        protocolContext,
        goodBlock,
        HeaderValidationMode.DETACHED_ONLY,
        HeaderValidationMode.DETACHED_ONLY);
    assertThat(badBlockManager.getBadBlocks()).isEmpty();
  }
}
