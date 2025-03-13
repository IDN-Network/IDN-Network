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
package org.idnecology.idn.ethereum.mainnet.parallelization;

import static org.idnecology.idn.ethereum.trie.diffbased.common.worldview.WorldStateConfig.createStatefulConfigWithTrie;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.mainnet.MainnetTransactionProcessor;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.ethereum.transaction.TransactionInvalidReason;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.NoOpBonsaiCachedWorldStorageManager;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.NoopBonsaiCachedMerkleTrieLoader;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.worldview.BonsaiWorldState;
import org.idnecology.idn.ethereum.trie.diffbased.common.trielog.NoOpTrieLogManager;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.accumulator.DiffBasedWorldStateUpdateAccumulator;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.blockhash.BlockHashLookup;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.util.Collections;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParallelizedConcurrentTransactionProcessorTest {

  @Mock private MainnetTransactionProcessor transactionProcessor;
  @Mock private BlockHeader chainHeadBlockHeader;
  @Mock private BlockHeader blockHeader;
  @Mock ProtocolContext protocolContext;
  @Mock private Transaction transaction;
  @Mock private PrivateMetadataUpdater privateMetadataUpdater;
  @Mock private TransactionCollisionDetector transactionCollisionDetector;

  private BonsaiWorldState worldState;

  private ParallelizedConcurrentTransactionProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new ParallelizedConcurrentTransactionProcessor(
            transactionProcessor, transactionCollisionDetector);
    final BonsaiWorldStateKeyValueStorage bonsaiWorldStateKeyValueStorage =
        new BonsaiWorldStateKeyValueStorage(
            new InMemoryKeyValueStorageProvider(),
            new NoOpMetricsSystem(),
            DataStorageConfiguration.DEFAULT_BONSAI_CONFIG);
    worldState =
        new BonsaiWorldState(
            bonsaiWorldStateKeyValueStorage,
            new NoopBonsaiCachedMerkleTrieLoader(),
            new NoOpBonsaiCachedWorldStorageManager(bonsaiWorldStateKeyValueStorage),
            new NoOpTrieLogManager(),
            EvmConfiguration.DEFAULT,
            createStatefulConfigWithTrie());

    when(chainHeadBlockHeader.getHash()).thenReturn(Hash.ZERO);
    when(chainHeadBlockHeader.getStateRoot()).thenReturn(Hash.EMPTY_TRIE_HASH);
    when(blockHeader.getParentHash()).thenReturn(Hash.ZERO);

    when(transaction.detachedCopy()).thenReturn(transaction);

    final MutableBlockchain blockchain = mock(MutableBlockchain.class);
    when(protocolContext.getBlockchain()).thenReturn(blockchain);
    when(blockchain.getChainHeadHeader()).thenReturn(chainHeadBlockHeader);
    final WorldStateArchive worldStateArchive = mock(WorldStateArchive.class);
    when(protocolContext.getWorldStateArchive()).thenReturn(worldStateArchive);
    when(worldStateArchive.getWorldState(any())).thenReturn(Optional.of(worldState));
    when(transactionCollisionDetector.hasCollision(any(), any(), any(), any())).thenReturn(false);
  }

  @Test
  void testRunTransaction() {
    Address miningBeneficiary = Address.fromHexString("0x1");
    Wei blobGasPrice = Wei.ZERO;

    Mockito.when(
            transactionProcessor.processTransaction(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                Collections.emptyList(), 0, 0, Bytes.EMPTY, ValidationResult.valid()));

    processor.runTransaction(
        protocolContext,
        blockHeader,
        0,
        transaction,
        miningBeneficiary,
        (__, ___) -> Hash.EMPTY,
        blobGasPrice,
        privateMetadataUpdater);

    verify(transactionProcessor, times(1))
        .processTransaction(
            any(DiffBasedWorldStateUpdateAccumulator.class),
            eq(blockHeader),
            eq(transaction),
            eq(miningBeneficiary),
            any(OperationTracer.class),
            any(BlockHashLookup.class),
            eq(true),
            eq(TransactionValidationParams.processingBlock()),
            eq(privateMetadataUpdater),
            eq(blobGasPrice));

    assertTrue(
        processor
            .applyParallelizedTransactionResult(
                worldState, miningBeneficiary, transaction, 0, Optional.empty(), Optional.empty())
            .isPresent(),
        "Expected the transaction context to be stored");
  }

  @Test
  void testRunTransactionWithFailure() {
    Address miningBeneficiary = Address.fromHexString("0x1");
    Wei blobGasPrice = Wei.ZERO;

    when(transactionProcessor.processTransaction(
            any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(
            TransactionProcessingResult.failed(
                0,
                0,
                ValidationResult.invalid(
                    TransactionInvalidReason.BLOB_GAS_PRICE_BELOW_CURRENT_BLOB_BASE_FEE),
                Optional.of(Bytes.EMPTY)));

    processor.runTransaction(
        protocolContext,
        blockHeader,
        0,
        transaction,
        miningBeneficiary,
        (__, ___) -> Hash.EMPTY,
        blobGasPrice,
        privateMetadataUpdater);

    Optional<TransactionProcessingResult> result =
        processor.applyParallelizedTransactionResult(
            worldState, miningBeneficiary, transaction, 0, Optional.empty(), Optional.empty());
    assertTrue(result.isEmpty(), "Expected the transaction result to indicate a failure");
  }

  @Test
  void testRunTransactionWithConflict() {

    Address miningBeneficiary = Address.fromHexString("0x1");
    Wei blobGasPrice = Wei.ZERO;

    Mockito.when(
            transactionProcessor.processTransaction(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                Collections.emptyList(), 0, 0, Bytes.EMPTY, ValidationResult.valid()));

    processor.runTransaction(
        protocolContext,
        blockHeader,
        0,
        transaction,
        miningBeneficiary,
        (__, ___) -> Hash.EMPTY,
        blobGasPrice,
        privateMetadataUpdater);

    verify(transactionProcessor, times(1))
        .processTransaction(
            any(DiffBasedWorldStateUpdateAccumulator.class),
            eq(blockHeader),
            eq(transaction),
            eq(miningBeneficiary),
            any(OperationTracer.class),
            any(BlockHashLookup.class),
            eq(true),
            eq(TransactionValidationParams.processingBlock()),
            eq(privateMetadataUpdater),
            eq(blobGasPrice));

    // simulate a conflict
    when(transactionCollisionDetector.hasCollision(any(), any(), any(), any())).thenReturn(true);

    Optional<TransactionProcessingResult> result =
        processor.applyParallelizedTransactionResult(
            worldState, miningBeneficiary, transaction, 0, Optional.empty(), Optional.empty());
    assertTrue(result.isEmpty(), "Expected no transaction result to be applied due to conflict");
  }
}
