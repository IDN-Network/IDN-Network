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
import static org.mockito.Mockito.mock;

import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.cli.config.NetworkName;
import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.MergeConfiguration;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.testutil.BlockTestUtil;
import org.idnecology.idn.testutil.TestClock;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletionException;

import com.google.common.io.Resources;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for {@link RlpBlockImporter}. */
@ExtendWith(MockitoExtension.class)
public final class RlpBlockImporterTest {

  @TempDir Path dataDir;

  private final RlpBlockImporter rlpBlockImporter = new RlpBlockImporter();

  @Test
  public void blockImport() throws IOException {
    final Path source = dataDir.resolve("1000.blocks");
    BlockTestUtil.write1000Blocks(source);
    final IdnController targetController =
        new IdnController.Builder()
            .fromEthNetworkConfig(
                EthNetworkConfig.getNetworkConfig(NetworkName.MAINNET), SyncMode.FAST)
            .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
            .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .storageProvider(new InMemoryKeyValueStorageProvider())
            .networkId(BigInteger.ONE)
            .miningParameters(MiningConfiguration.newDefault())
            .nodeKey(NodeKeyUtils.generate())
            .metricsSystem(new NoOpMetricsSystem())
            .privacyParameters(PrivacyParameters.DEFAULT)
            .dataDirectory(dataDir)
            .clock(TestClock.fixed())
            .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
            .evmConfiguration(EvmConfiguration.DEFAULT)
            .networkConfiguration(NetworkingConfiguration.create())
            .idnComponent(mock(IdnComponent.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();
    final RlpBlockImporter.ImportResult result =
        rlpBlockImporter.importBlockchain(source, targetController, false);
    // Don't count the Genesis block
    assertThat(result.count).isEqualTo(999);
    assertThat(result.td).isEqualTo(UInt256.valueOf(21991996248790L));
  }

  @Test
  public void blockImportRejectsBadPow() throws IOException {
    // set merge flag to false, otherwise this test can fail if a merge test runs first
    MergeConfiguration.setMergeEnabled(false);

    final Path source = dataDir.resolve("badpow.blocks");
    BlockTestUtil.writeBadPowBlocks(source);
    final IdnController targetController =
        new IdnController.Builder()
            .fromEthNetworkConfig(
                EthNetworkConfig.getNetworkConfig(NetworkName.MAINNET), SyncMode.FAST)
            .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
            .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .storageProvider(new InMemoryKeyValueStorageProvider())
            .networkId(BigInteger.ONE)
            .miningParameters(MiningConfiguration.newDefault())
            .nodeKey(NodeKeyUtils.generate())
            .metricsSystem(new NoOpMetricsSystem())
            .privacyParameters(PrivacyParameters.DEFAULT)
            .dataDirectory(dataDir)
            .clock(TestClock.fixed())
            .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
            .evmConfiguration(EvmConfiguration.DEFAULT)
            .networkConfiguration(NetworkingConfiguration.create())
            .idnComponent(mock(IdnComponent.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();

    assertThatThrownBy(
        () -> rlpBlockImporter.importBlockchain(source, targetController, false),
        "Invalid header at block number 2.",
        CompletionException.class);
  }

  @Test
  public void blockImportCanSkipPow() throws IOException {
    final Path source = dataDir.resolve("badpow.blocks");
    BlockTestUtil.writeBadPowBlocks(source);
    final IdnController targetController =
        new IdnController.Builder()
            .fromEthNetworkConfig(
                EthNetworkConfig.getNetworkConfig(NetworkName.MAINNET), SyncMode.FAST)
            .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
            .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .storageProvider(new InMemoryKeyValueStorageProvider())
            .networkId(BigInteger.ONE)
            .miningParameters(MiningConfiguration.newDefault())
            .nodeKey(NodeKeyUtils.generate())
            .metricsSystem(new NoOpMetricsSystem())
            .privacyParameters(PrivacyParameters.DEFAULT)
            .dataDirectory(dataDir)
            .clock(TestClock.fixed())
            .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
            .evmConfiguration(EvmConfiguration.DEFAULT)
            .networkConfiguration(NetworkingConfiguration.create())
            .idnComponent(mock(IdnComponent.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();

    final RlpBlockImporter.ImportResult result =
        rlpBlockImporter.importBlockchain(source, targetController, true);
    assertThat(result.count).isEqualTo(1);
    assertThat(result.td).isEqualTo(UInt256.valueOf(34351349760L));
  }

  @Test
  public void ibftImport() throws IOException {
    final Path source = dataDir.resolve("ibft.blocks");
    final String config =
        Resources.toString(this.getClass().getResource("/ibft-genesis-2.json"), UTF_8);

    try {
      Files.write(
          source,
          Resources.toByteArray(this.getClass().getResource("/ibft.blocks")),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (final IOException ex) {
      throw new IllegalStateException(ex);
    }

    final IdnController controller =
        new IdnController.Builder()
            .fromGenesisFile(GenesisConfig.fromConfig(config), SyncMode.FULL)
            .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
            .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .storageProvider(new InMemoryKeyValueStorageProvider())
            .networkId(BigInteger.valueOf(1337))
            .miningParameters(MiningConfiguration.newDefault())
            .nodeKey(NodeKeyUtils.generate())
            .metricsSystem(new NoOpMetricsSystem())
            .privacyParameters(PrivacyParameters.DEFAULT)
            .dataDirectory(dataDir)
            .clock(TestClock.fixed())
            .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
            .evmConfiguration(EvmConfiguration.DEFAULT)
            .networkConfiguration(NetworkingConfiguration.create())
            .idnComponent(mock(IdnComponent.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .build();
    final RlpBlockImporter.ImportResult result =
        rlpBlockImporter.importBlockchain(source, controller, true);

    // Don't count the Genesis block
    assertThat(result.count).isEqualTo(100);
  }
}
