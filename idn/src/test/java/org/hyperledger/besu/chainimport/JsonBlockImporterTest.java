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
package org.idnecology.idn.chainimport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.idnecology.idn.components.IdnCommandModule;
import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.components.IdnPluginContextModule;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.JsonUtil;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.components.MiningParametersModule;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.BlobCacheModule;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoader;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.MetricsSystemModule;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.testutil.TestClock;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class JsonBlockImporterTest {

  @TempDir public Path dataDir;

  protected String consensusEngine;
  protected GenesisConfig genesisConfig;
  protected boolean isEthash;

  protected void setup(final String consensusEngine) throws IOException {
    this.consensusEngine = consensusEngine;
    final String genesisData = getFileContents("genesis.json");
    this.genesisConfig = GenesisConfig.fromConfig(genesisData);
    this.isEthash = genesisConfig.getConfigOptions().isEthHash();
  }

  public static class SingletonTests extends JsonBlockImporterTest {

    @BeforeEach
    public void setup() throws IOException {
      super.setup("unsupported");
    }

    @Test
    public void importChain_unsupportedConsensusAlgorithm() throws IOException {
      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      final String jsonData = getFileContents("clique", "blocks-import-valid.json");

      assertThatThrownBy(() -> importer.importChain(jsonData))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              "Unable to create block using current consensus engine: "
                  + genesisConfig.getConfigOptions().getConsensusEngine());
    }
  }

  public static class ParameterizedTests extends JsonBlockImporterTest {

    @Override
    public void setup(final String consensusEngine) throws IOException {
      super.setup(consensusEngine);
    }

    public static Stream<Arguments> getParameters() {
      return Stream.of(Arguments.of("ethash"), Arguments.of("clique"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getParameters")
    public void importChain_validJson_withBlockNumbers(final String consensusEngine)
        throws IOException {
      setup(consensusEngine);

      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      final String jsonData = getFileContents("blocks-import-valid.json");
      importer.importChain(jsonData);

      final Blockchain blockchain = controller.getProtocolContext().getBlockchain();

      // Check blocks were imported
      assertThat(blockchain.getChainHead().getHeight()).isEqualTo(4);
      // Get imported blocks
      final List<Block> blocks = new ArrayList<>(4);
      for (int i = 0; i < 4; i++) {
        blocks.add(getBlockAt(blockchain, i + 1));
      }

      // Check block 1
      Block block = blocks.get(0);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.EMPTY);
        assertThat(block.getHeader().getCoinbase()).isEqualTo(Address.ZERO);
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(2);
      // Check first tx
      Transaction tx = block.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFF1L);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(1L));
      assertThat(tx.getNonce()).isEqualTo(0L);
      // Check second tx
      tx = block.getBody().getTransactions().get(1);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFF2L);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xEF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(0L));
      assertThat(tx.getNonce()).isEqualTo(1L);

      // Check block 2
      block = blocks.get(1);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.fromHexString("0x1234"));
        assertThat(block.getHeader().getCoinbase()).isEqualTo(Address.fromHexString("0x02"));
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(1);
      // Check first tx
      tx = block.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFFFL);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(0L));
      assertThat(tx.getNonce()).isEqualTo(0L);

      // Check block 3
      block = blocks.get(2);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.fromHexString("0x3456"));
        assertThat(block.getHeader().getCoinbase())
            .isEqualTo(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732"));
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(0);

      // Check block 4
      block = blocks.get(3);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.EMPTY);
        assertThat(block.getHeader().getCoinbase()).isEqualTo(Address.ZERO);
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(1);
      // Check first tx
      tx = block.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getTo()).isEmpty();
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFFFL);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(0L));
      assertThat(tx.getNonce()).isEqualTo(1L);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getParameters")
    public void importChain_validJson_noBlockIdentifiers(final String consensusEngine)
        throws IOException {
      setup(consensusEngine);

      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      final String jsonData = getFileContents("blocks-import-valid-no-block-identifiers.json");
      importer.importChain(jsonData);

      final Blockchain blockchain = controller.getProtocolContext().getBlockchain();

      // Check blocks were imported
      assertThat(blockchain.getChainHead().getHeight()).isEqualTo(4);
      // Get imported blocks
      final List<Block> blocks = new ArrayList<>(4);
      for (int i = 0; i < 4; i++) {
        blocks.add(getBlockAt(blockchain, i + 1));
      }

      // Check block 1
      Block block = blocks.get(0);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.EMPTY);
        assertThat(block.getHeader().getCoinbase()).isEqualTo(Address.ZERO);
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(2);
      // Check first tx
      Transaction tx = block.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFF1L);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(1L));
      assertThat(tx.getNonce()).isEqualTo(0L);
      // Check second tx
      tx = block.getBody().getTransactions().get(1);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFF2L);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xEF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(0L));
      assertThat(tx.getNonce()).isEqualTo(1L);

      // Check block 2
      block = blocks.get(1);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.fromHexString("0x1234"));
        assertThat(block.getHeader().getCoinbase()).isEqualTo(Address.fromHexString("0x02"));
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(1);
      // Check first tx
      tx = block.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFFFL);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(0L));
      assertThat(tx.getNonce()).isEqualTo(0L);

      // Check block 3
      block = blocks.get(2);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.fromHexString("0x3456"));
        assertThat(block.getHeader().getCoinbase())
            .isEqualTo(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732"));
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(0);

      // Check block 4
      block = blocks.get(3);
      if (isEthash) {
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.EMPTY);
        assertThat(block.getHeader().getCoinbase()).isEqualTo(Address.ZERO);
      }
      assertThat(block.getBody().getTransactions().size()).isEqualTo(1);
      // Check first tx
      tx = block.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getTo()).isEmpty();
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFFFL);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(0L));
      assertThat(tx.getNonce()).isEqualTo(1L);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getParameters")
    public void importChain_validJson_withParentHashes(final String consensusEngine)
        throws IOException {
      setup(consensusEngine);

      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      String jsonData = getFileContents("blocks-import-valid.json");

      importer.importChain(jsonData);

      final Blockchain blockchain = controller.getProtocolContext().getBlockchain();

      // Check blocks were imported
      assertThat(blockchain.getChainHead().getHeight()).isEqualTo(4);
      // Get imported blocks
      final List<Block> blocks = new ArrayList<>(4);
      for (int i = 0; i < 4; i++) {
        blocks.add(getBlockAt(blockchain, i + 1));
      }

      // Run new import based on first file
      jsonData = getFileContents("blocks-import-valid-addendum.json");
      final ObjectNode newImportData = JsonUtil.objectNodeFromString(jsonData);
      final ObjectNode block0 = (ObjectNode) newImportData.get("blocks").get(0);
      final Block parentBlock = blocks.get(3);
      block0.put("parentHash", parentBlock.getHash().toString());
      final String newImportJsonData = JsonUtil.getJson(newImportData);
      importer.importChain(newImportJsonData);

      // Check blocks were imported
      assertThat(blockchain.getChainHead().getHeight()).isEqualTo(5);
      final Block newBlock = getBlockAt(blockchain, parentBlock.getHeader().getNumber() + 1L);

      // Check block 1
      assertThat(newBlock.getHeader().getParentHash()).isEqualTo(parentBlock.getHash());
      if (isEthash) {
        assertThat(newBlock.getHeader().getExtraData()).isEqualTo(Bytes.EMPTY);
        assertThat(newBlock.getHeader().getCoinbase()).isEqualTo(Address.ZERO);
      }
      assertThat(newBlock.getBody().getTransactions().size()).isEqualTo(1);
      // Check first tx
      final Transaction tx = newBlock.getBody().getTransactions().get(0);
      assertThat(tx.getSender())
          .isEqualTo(Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73"));
      assertThat(tx.getTo())
          .hasValue(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      assertThat(tx.getGasLimit()).isEqualTo(0xFFFFF1L);
      assertThat(tx.getGasPrice().get()).isEqualTo(Wei.fromHexString("0xFF"));
      assertThat(tx.getValue()).isEqualTo(Wei.of(1L));
      assertThat(tx.getNonce()).isEqualTo(2L);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getParameters")
    public void importChain_invalidParent(final String consensusEngine) throws IOException {
      setup(consensusEngine);

      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      final String jsonData = getFileContents("blocks-import-invalid-bad-parent.json");

      assertThatThrownBy(() -> importer.importChain(jsonData))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageStartingWith("Unable to locate block parent at 2456");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getParameters")
    public void importChain_invalidTransaction(final String consensusEngine) throws IOException {
      setup(consensusEngine);

      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      final String jsonData = getFileContents("blocks-import-invalid-bad-tx.json");

      assertThatThrownBy(() -> importer.importChain(jsonData))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageStartingWith(
              "Unable to create block.  1 transaction(s) were found to be invalid.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("getParameters")
    public void importChain_specialFields(final String consensusEngine) throws IOException {
      setup(consensusEngine);

      final IdnController controller = createController();
      final JsonBlockImporter importer = new JsonBlockImporter(controller);

      final String jsonData = getFileContents("blocks-import-special-fields.json");

      if (isEthash) {
        importer.importChain(jsonData);
        final Blockchain blockchain = controller.getProtocolContext().getBlockchain();
        final Block block = getBlockAt(blockchain, 1);
        assertThat(block.getHeader().getExtraData()).isEqualTo(Bytes.fromHexString("0x0123"));
        assertThat(block.getHeader().getCoinbase())
            .isEqualTo(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57"));
      } else {
        assertThatThrownBy(() -> importer.importChain(jsonData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "Some fields (coinbase, extraData) are unsupported by the current consensus engine: "
                    + genesisConfig.getConfigOptions().getConsensusEngine());
      }
    }

    @Test
    void dryRunDetector() {
      assertThat(true)
          .withFailMessage("This test is here so gradle --dry-run executes this class")
          .isTrue();
    }
  }

  protected Block getBlockAt(final Blockchain blockchain, final long blockNumber) {
    final BlockHeader header = blockchain.getBlockHeader(blockNumber).get();
    final BlockBody body = blockchain.getBlockBody(header.getHash()).get();
    return new Block(header, body);
  }

  protected String getFileContents(final String filename) throws IOException {
    return getFileContents(consensusEngine, filename);
  }

  protected String getFileContents(final String folder, final String filename) throws IOException {
    final String filePath = folder + "/" + filename;
    final URL fileURL = this.getClass().getResource(filePath);
    return Resources.toString(fileURL, UTF_8);
  }

  protected IdnController createController() throws IOException {
    return createController(genesisConfig);
  }

  protected IdnController createController(final GenesisConfig genesisConfig) {
    return new IdnController.Builder()
        .fromGenesisFile(genesisConfig, SyncMode.FAST)
        .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
        .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
        .storageProvider(new InMemoryKeyValueStorageProvider())
        .networkId(BigInteger.valueOf(10))
        .miningParameters(
            ImmutableMiningConfiguration.builder()
                .mutableInitValues(
                    MutableInitValues.builder()
                        .isMiningEnabled(true)
                        .minTransactionGasPrice(Wei.ZERO)
                        .build())
                .build())
        .nodeKey(NodeKeyUtils.generate())
        .metricsSystem(new NoOpMetricsSystem())
        .privacyParameters(PrivacyParameters.DEFAULT)
        .dataDirectory(dataDir)
        .clock(TestClock.fixed())
        .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
        .evmConfiguration(EvmConfiguration.DEFAULT)
        .networkConfiguration(NetworkingConfiguration.create())
        .idnComponent(DaggerJsonBlockImporterTest_JsonBlockImportComponent.builder().build())
        .apiConfiguration(ImmutableApiConfiguration.builder().build())
        .build();
  }

  @Module
  public static class JsonBlockImporterModule {

    @Provides
    BonsaiCachedMerkleTrieLoader provideCachedMerkleTrieLoaderModule() {
      return new BonsaiCachedMerkleTrieLoader(new NoOpMetricsSystem());
    }
  }

  @Singleton
  @Component(
      modules = {
        IdnCommandModule.class,
        MiningParametersModule.class,
        MetricsSystemModule.class,
        JsonBlockImporterModule.class,
        IdnPluginContextModule.class,
        BlobCacheModule.class
      })
  interface JsonBlockImportComponent extends IdnComponent {}
}
