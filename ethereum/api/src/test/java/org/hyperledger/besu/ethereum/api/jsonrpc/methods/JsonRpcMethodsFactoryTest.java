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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.core.ProtocolScheduleFixture.getGenesisConfigOptions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.graphql.GraphQLConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.p2p.network.P2PNetwork;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Capability;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.transaction.TransactionSimulator;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.nat.NatService;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonRpcMethodsFactoryTest {
  private static final String CLIENT_NODE_NAME = "TestClientVersion/0.1.0";
  private static final String CLIENT_VERSION = "0.1.0";
  private static final String CLIENT_COMMIT = "12345678";
  private static final BigInteger NETWORK_ID = BigInteger.valueOf(123);

  @Mock private BlockchainQueries blockchainQueries;
  @Mock private MergeMiningCoordinator mergeCoordinator;

  @TempDir private Path folder;

  final Set<Capability> supportedCapabilities = new HashSet<>();
  private final NatService natService = new NatService(Optional.empty());
  private final Vertx vertx = Vertx.vertx();

  private ProtocolSchedule pragueAllMilestonesZeroProtocolSchedule;
  private JsonRpcConfiguration configuration;

  @BeforeEach
  public void setup() {
    configuration = JsonRpcConfiguration.createEngineDefault();
    configuration.setPort(0);

    pragueAllMilestonesZeroProtocolSchedule =
        MainnetProtocolSchedule.fromConfig(
            getPragueAllZeroMilestonesConfigOptions(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            MiningConfiguration.newDefault(),
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    when(mergeCoordinator.isCompatibleWithEngineApi()).thenReturn(true);
  }

  @Test
  public void shouldActivateEngineApisIfMilestonesAreAllZero() {
    final Map<String, JsonRpcMethod> rpcMethods =
        new JsonRpcMethodsFactory()
            .methods(
                CLIENT_NODE_NAME,
                CLIENT_VERSION,
                CLIENT_COMMIT,
                NETWORK_ID,
                new StubGenesisConfigOptions(),
                mock(P2PNetwork.class),
                blockchainQueries,
                mock(Synchronizer.class),
                pragueAllMilestonesZeroProtocolSchedule,
                mock(ProtocolContext.class),
                mock(FilterManager.class),
                mock(TransactionPool.class),
                mock(MiningConfiguration.class),
                mergeCoordinator,
                new NoOpMetricsSystem(),
                supportedCapabilities,
                Optional.of(mock(AccountLocalConfigPermissioningController.class)),
                Optional.of(mock(NodeLocalConfigPermissioningController.class)),
                configuration.getRpcApis(),
                mock(PrivacyParameters.class),
                mock(JsonRpcConfiguration.class),
                mock(WebSocketConfiguration.class),
                mock(MetricsConfiguration.class),
                mock(GraphQLConfiguration.class),
                natService,
                new HashMap<>(),
                folder,
                mock(EthPeers.class),
                vertx,
                mock(ApiConfiguration.class),
                Optional.empty(),
                mock(TransactionSimulator.class),
                new DeterministicEthScheduler());

    assertThat(rpcMethods).containsKey("engine_getPayloadV3");
    assertThat(rpcMethods).containsKey("engine_getPayloadV4");
    assertThat(rpcMethods).containsKey("engine_newPayloadV4");
  }

  private GenesisConfigOptions getPragueAllZeroMilestonesConfigOptions() {
    return getGenesisConfigOptions("/prague_all_milestones_zero.json");
  }
}
