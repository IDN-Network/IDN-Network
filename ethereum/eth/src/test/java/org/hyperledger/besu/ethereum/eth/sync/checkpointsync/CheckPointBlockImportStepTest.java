/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.eth.sync.checkpointsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.chain.DefaultBlockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.BlockWithReceipts;
import org.idnecology.idn.ethereum.eth.sync.fastsync.checkpoint.Checkpoint;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CheckPointBlockImportStepTest {

  private final CheckpointSource checkPointSource = mock(CheckpointSource.class);
  private final Checkpoint checkpoint = mock(Checkpoint.class);
  private MutableBlockchain blockchain;
  private CheckpointBlockImportStep checkPointHeaderImportStep;
  private KeyValueStoragePrefixedKeyBlockchainStorage blockchainStorage;

  @BeforeEach
  public void setup() {
    blockchainStorage =
        new KeyValueStoragePrefixedKeyBlockchainStorage(
            new InMemoryKeyValueStorage(),
            new VariablesKeyValueStorage(new InMemoryKeyValueStorage()),
            new MainnetBlockHeaderFunctions(),
            false);
    blockchain =
        DefaultBlockchain.createMutable(
            generateBlock(0), blockchainStorage, mock(MetricsSystem.class), 0);
    checkPointHeaderImportStep =
        new CheckpointBlockImportStep(checkPointSource, checkpoint, blockchain);
  }

  @Test
  public void shouldSaveNewHeader() {
    when(checkPointSource.hasNext()).thenReturn(true);
    assertThat(blockchainStorage.getBlockHash(1)).isEmpty();
    final Block block = generateBlock(1);
    checkPointHeaderImportStep.accept(Optional.of(new BlockWithReceipts(block, new ArrayList<>())));
    assertThat(blockchainStorage.getBlockHash(1)).isPresent();
  }

  @Test
  public void shouldSaveChainHeadForLastBlock() {
    when(checkPointSource.hasNext()).thenReturn(false);
    final Block block = generateBlock(2);
    when(checkPointSource.getCheckpoint()).thenReturn(block.getHeader());
    checkPointHeaderImportStep.accept(Optional.of(new BlockWithReceipts(block, new ArrayList<>())));
    assertThat(blockchainStorage.getBlockHash(2)).isPresent();
  }

  private Block generateBlock(final int blockNumber) {
    final BlockBody body = new BlockBody(Collections.emptyList(), Collections.emptyList());
    return new Block(new BlockHeaderTestFixture().number(blockNumber).buildHeader(), body);
  }
}
