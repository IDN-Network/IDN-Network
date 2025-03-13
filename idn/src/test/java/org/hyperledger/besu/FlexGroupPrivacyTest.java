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
package org.idnecology.idn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.PrivacyParameters.FLEXIBLE_PRIVACY;

import org.idnecology.idn.components.IdnComponent;
import org.idnecology.idn.components.IdnPluginContextModule;
import org.idnecology.idn.components.MockIdnCommandModule;
import org.idnecology.idn.components.NoOpMetricsSystemModule;
import org.idnecology.idn.components.PrivacyTestModule;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.enclave.EnclaveFactory;
import org.idnecology.idn.ethereum.api.ImmutableApiConfiguration;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.BlobCacheModule;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.privacy.storage.PrivacyStorageProvider;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoaderModule;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.precompile.PrecompiledContract;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.testutil.TestClock;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FlexGroupPrivacyTest {

  private final Vertx vertx = Vertx.vertx();

  @AfterEach
  public void cleanUp() {
    vertx.close();
  }

  @Test
  void flexibleEnabledPrivacy() {
    final IdnController idnController =
        DaggerFlexGroupPrivacyTest_FlexGroupPrivacyTestComponent.builder()
            .build()
            .getIdnController();

    final PrecompiledContract flexiblePrecompiledContract =
        getPrecompile(idnController, FLEXIBLE_PRIVACY);

    assertThat(flexiblePrecompiledContract.getName()).isEqualTo("FlexiblePrivacy");
  }

  private PrecompiledContract getPrecompile(
      final IdnController idnController, final Address defaultPrivacy) {
    return idnController
        .getProtocolSchedule()
        .getByBlockHeader(blockHeader(0))
        .getPrecompileContractRegistry()
        .get(defaultPrivacy);
  }

  private BlockHeader blockHeader(final long number) {
    return new BlockHeaderTestFixture().number(number).buildHeader();
  }

  @Singleton
  @Component(
      modules = {
        FlexGroupPrivacyParametersModule.class,
        FlexGroupPrivacyTest.PrivacyTestIdnControllerModule.class,
        PrivacyTestModule.class,
        MockIdnCommandModule.class,
        BonsaiCachedMerkleTrieLoaderModule.class,
        NoOpMetricsSystemModule.class,
        IdnPluginContextModule.class,
        BlobCacheModule.class
      })
  interface FlexGroupPrivacyTestComponent extends IdnComponent {
    IdnController getIdnController();
  }

  @Module
  static class FlexGroupPrivacyParametersModule {

    @Provides
    PrivacyParameters providePrivacyParameters(
        final PrivacyStorageProvider storageProvider, final Vertx vertx) {
      try {
        return new PrivacyParameters.Builder()
            .setEnabled(true)
            .setEnclaveUrl(new URI("http://127.0.0.1:8000"))
            .setStorageProvider(storageProvider)
            .setEnclaveFactory(new EnclaveFactory(vertx))
            .setFlexiblePrivacyGroupsEnabled(true)
            .build();
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Module
  static class PrivacyTestIdnControllerModule {

    @Provides
    @Singleton
    @SuppressWarnings("CloseableProvides")
    IdnController provideIdnController(
        final PrivacyParameters privacyParameters,
        final DataStorageConfiguration dataStorageConfiguration,
        final FlexGroupPrivacyTestComponent context,
        @Named("dataDir") final Path dataDir) {

      return new IdnController.Builder()
          .fromGenesisFile(GenesisConfig.mainnet(), SyncMode.FULL)
          .synchronizerConfiguration(SynchronizerConfiguration.builder().build())
          .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
          .storageProvider(new InMemoryKeyValueStorageProvider())
          .networkId(BigInteger.ONE)
          .miningParameters(MiningConfiguration.newDefault())
          .dataStorageConfiguration(dataStorageConfiguration)
          .nodeKey(NodeKeyUtils.generate())
          .metricsSystem(new NoOpMetricsSystem())
          .dataDirectory(dataDir)
          .clock(TestClock.fixed())
          .privacyParameters(privacyParameters)
          .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
          .evmConfiguration(EvmConfiguration.DEFAULT)
          .networkConfiguration(NetworkingConfiguration.create())
          .idnComponent(context)
          .apiConfiguration(ImmutableApiConfiguration.builder().build())
          .build();
    }
  }
}
