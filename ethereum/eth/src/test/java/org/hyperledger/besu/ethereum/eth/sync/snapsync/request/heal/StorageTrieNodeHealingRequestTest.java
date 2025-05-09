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
package org.idnecology.idn.ethereum.eth.sync.snapsync.request.heal;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.TrieGenerator;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.storage.StorageProvider;
import org.idnecology.idn.ethereum.trie.MerkleTrie;
import org.idnecology.idn.ethereum.trie.common.PmtStateTrieAccountValue;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageTrieNodeHealingRequestTest {

  final List<Address> accounts =
      List.of(
          Address.fromHexString("0xdeadbeef"),
          Address.fromHexString("0xdeadbeee"),
          Address.fromHexString("0xdeadbeea"),
          Address.fromHexString("0xdeadbeeb"));

  private WorldStateStorageCoordinator worldStateStorageCoordinator;
  private Hash account0Hash;
  private Hash account0StorageRoot;

  static class StorageFormatArguments implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
      return Stream.of(
          Arguments.of(DataStorageFormat.BONSAI), Arguments.of(DataStorageFormat.FOREST));
    }
  }

  public void setup(final DataStorageFormat storageFormat) {
    if (storageFormat.equals(DataStorageFormat.FOREST)) {
      worldStateStorageCoordinator =
          new WorldStateStorageCoordinator(
              new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage()));
    } else {
      final StorageProvider storageProvider = new InMemoryKeyValueStorageProvider();
      worldStateStorageCoordinator =
          new WorldStateStorageCoordinator(
              new BonsaiWorldStateKeyValueStorage(
                  storageProvider,
                  new NoOpMetricsSystem(),
                  DataStorageConfiguration.DEFAULT_BONSAI_CONFIG));
    }
    final MerkleTrie<Bytes, Bytes> trie =
        TrieGenerator.generateTrie(
            worldStateStorageCoordinator,
            accounts.stream().map(Address::addressHash).collect(Collectors.toList()));

    account0Hash = accounts.get(0).addressHash();
    account0StorageRoot =
        trie.get(account0Hash)
            .map(RLP::input)
            .map(PmtStateTrieAccountValue::readFrom)
            .map(PmtStateTrieAccountValue::getStorageRoot)
            .orElseThrow();
  }

  @ParameterizedTest
  @ArgumentsSource(StorageFormatArguments.class)
  void shouldDetectExistingData(final DataStorageFormat storageFormat) {
    setup(storageFormat);

    final StorageTrieNodeHealingRequest request =
        new StorageTrieNodeHealingRequest(
            account0StorageRoot, account0Hash, Hash.EMPTY, Bytes.EMPTY);

    Assertions.assertThat(request.getExistingData(worldStateStorageCoordinator)).isPresent();
  }

  @ParameterizedTest
  @ArgumentsSource(StorageFormatArguments.class)
  void shouldDetectMissingData(final DataStorageFormat storageFormat) {
    setup(storageFormat);
    final StorageTrieNodeHealingRequest request =
        new StorageTrieNodeHealingRequest(Hash.EMPTY, account0Hash, Hash.EMPTY, Bytes.EMPTY);

    Assertions.assertThat(request.getExistingData(worldStateStorageCoordinator)).isEmpty();
  }

  @Test
  void dryRunDetector() {
    assertThat(true)
        .withFailMessage("This test is here so gradle --dry-run executes this class")
        .isTrue();
  }
}
