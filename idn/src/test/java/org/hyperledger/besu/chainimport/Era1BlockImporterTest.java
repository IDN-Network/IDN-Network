/*
 * Copyright contributors to Idn.
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

import static org.mockito.Mockito.mock;

import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.cli.config.NetworkName;
import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Era1BlockImporterTest {
  @TempDir Path dataDirectory;

  private final Era1BlockImporter era1BlockImporter = new Era1BlockImporter();

  @Test
  public void testImport()
      throws IOException,
          ExecutionException,
          InterruptedException,
          TimeoutException,
          URISyntaxException {
    final Path source =
        Path.of(
            BlockTestUtil.class
                .getClassLoader()
                .getResource("mainnet-00000-5ec1ffb8.era1")
                .toURI());
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
            .clock(TestClock.fixed())
            .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
            .evmConfiguration(EvmConfiguration.DEFAULT)
            .networkConfiguration(NetworkingConfiguration.create())
            .idnComponent(mock(IdnComponent.class))
            .apiConfiguration(ImmutableApiConfiguration.builder().build())
            .dataDirectory(dataDirectory)
            .build();
    era1BlockImporter.importBlocks(targetController, source);

    Blockchain blockchain = targetController.getProtocolContext().getBlockchain();
    BlockHeader chainHeadHeader = blockchain.getChainHeadHeader();
    Assertions.assertEquals(8191, chainHeadHeader.getNumber());
  }
}
