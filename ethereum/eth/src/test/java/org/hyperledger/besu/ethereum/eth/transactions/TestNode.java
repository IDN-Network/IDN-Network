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
package org.idnecology.idn.ethereum.eth.transactions;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.GenesisState;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.difficulty.fixed.FixedDifficultyProtocolSchedule;
import org.idnecology.idn.ethereum.eth.EthProtocol;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthMessages;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.sync.ChainHeadTracker;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.forkid.ForkIdManager;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.idnecology.idn.ethereum.p2p.config.DiscoveryConfiguration;
import org.idnecology.idn.ethereum.p2p.config.NetworkingConfiguration;
import org.idnecology.idn.ethereum.p2p.config.RlpxConfiguration;
import org.idnecology.idn.ethereum.p2p.network.DefaultP2PNetwork;
import org.idnecology.idn.ethereum.p2p.network.NetworkRunner;
import org.idnecology.idn.ethereum.p2p.network.P2PNetwork;
import org.idnecology.idn.ethereum.p2p.peers.DefaultPeer;
import org.idnecology.idn.ethereum.p2p.peers.Peer;
import org.idnecology.idn.ethereum.p2p.rlpx.RlpxAgent;
import org.idnecology.idn.ethereum.p2p.rlpx.connections.PeerConnection;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.data.EnodeURL;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.permissioning.NodeMessagePermissioningProvider;
import org.idnecology.idn.testutil.TestClock;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNode implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(TestNode.class);
  private static final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  protected final NodeKey nodeKey;
  protected final P2PNetwork network;
  protected final Peer selfPeer;
  protected final Map<PeerConnection, DisconnectReason> disconnections = new HashMap<>();
  private final TransactionPool transactionPool;

  public TestNode(
      final Vertx vertx,
      final Integer port,
      final KeyPair kp,
      final DiscoveryConfiguration discoveryCfg) {
    requireNonNull(vertx);
    requireNonNull(discoveryCfg);

    final int listenPort = port != null ? port : 0;
    this.nodeKey = kp != null ? NodeKeyUtils.createFrom(kp) : NodeKeyUtils.generate();

    final NetworkingConfiguration networkingConfiguration =
        NetworkingConfiguration.create()
            .setDiscovery(discoveryCfg)
            .setRlpx(
                RlpxConfiguration.create()
                    .setBindPort(listenPort)
                    .setSupportedProtocols(EthProtocol.get()));

    final GenesisConfig genesisConfig = GenesisConfig.fromResource("/dev.json");
    final ProtocolSchedule protocolSchedule =
        FixedDifficultyProtocolSchedule.create(
            GenesisConfig.fromResource("/dev.json").getConfigOptions(),
            false,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final GenesisState genesisState = GenesisState.fromConfig(genesisConfig, protocolSchedule);
    final BlockHeaderFunctions blockHeaderFunctions =
        ScheduleBasedBlockHeaderFunctions.create(protocolSchedule);
    final MutableBlockchain blockchain =
        createInMemoryBlockchain(genesisState.getBlock(), blockHeaderFunctions);
    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();
    genesisState.writeStateTo(worldStateArchive.getWorldState());
    final ProtocolContext protocolContext =
        new ProtocolContext(
            blockchain, worldStateArchive, mock(ConsensusContext.class), new BadBlockManager());

    final SyncState syncState = mock(SyncState.class);
    final SynchronizerConfiguration syncConfig = mock(SynchronizerConfiguration.class);
    when(syncState.isInSync(anyLong())).thenReturn(true);
    when(syncState.isInitialSyncPhaseDone()).thenReturn(true);

    final EthMessages ethMessages = new EthMessages();
    final NodeMessagePermissioningProvider nmpp =
        new NodeMessagePermissioningProvider() {
          @Override
          public boolean isMessagePermitted(final EnodeURL destinationEnode, final int code) {
            return true;
          }
        };
    final EthPeers ethPeers =
        new EthPeers(
            () -> protocolSchedule.getByBlockHeader(blockchain.getChainHeadHeader()),
            TestClock.fixed(),
            metricsSystem,
            EthProtocolConfiguration.DEFAULT_MAX_MESSAGE_SIZE,
            Collections.singletonList(nmpp),
            Bytes.random(64),
            25,
            25,
            false,
            SyncMode.SNAP,
            new ForkIdManager(blockchain, Collections.emptyList(), Collections.emptyList(), false));

    final ChainHeadTracker mockCHT = getChainHeadTracker();
    ethPeers.setChainHeadTracker(mockCHT);

    final EthScheduler scheduler = new EthScheduler(1, 1, 1, metricsSystem);
    final EthContext ethContext = new EthContext(ethPeers, ethMessages, scheduler, null);

    transactionPool =
        TransactionPoolFactory.createTransactionPool(
            protocolSchedule,
            protocolContext,
            ethContext,
            TestClock.system(ZoneId.systemDefault()),
            metricsSystem,
            syncState,
            TransactionPoolConfiguration.DEFAULT,
            new BlobCache(),
            MiningConfiguration.newDefault(),
            false);

    final EthProtocolManager ethProtocolManager =
        new EthProtocolManager(
            blockchain,
            BigInteger.ONE,
            worldStateArchive,
            transactionPool,
            EthProtocolConfiguration.defaultConfig(),
            ethPeers,
            ethMessages,
            ethContext,
            Collections.emptyList(),
            Optional.empty(),
            syncConfig,
            scheduler);

    final NetworkRunner networkRunner =
        NetworkRunner.builder()
            .subProtocols(EthProtocol.get())
            .protocolManagers(singletonList(ethProtocolManager))
            .ethPeersShouldConnect((p, d) -> true)
            .network(
                capabilities ->
                    DefaultP2PNetwork.builder()
                        .vertx(vertx)
                        .nodeKey(nodeKey)
                        .config(networkingConfiguration)
                        .metricsSystem(new NoOpMetricsSystem())
                        .supportedCapabilities(capabilities)
                        .storageProvider(new InMemoryKeyValueStorageProvider())
                        .blockchain(blockchain)
                        .blockNumberForks(Collections.emptyList())
                        .timestampForks(Collections.emptyList())
                        .allConnectionsSupplier(ethPeers::streamAllConnections)
                        .allActiveConnectionsSupplier(ethPeers::streamAllActiveConnections)
                        .build())
            .metricsSystem(new NoOpMetricsSystem())
            .build();
    network = networkRunner.getNetwork();
    final RlpxAgent rlpxAgent = network.getRlpxAgent();
    rlpxAgent.subscribeConnectRequest((p, d) -> true);
    ethPeers.setRlpxAgent(rlpxAgent);
    network.subscribeDisconnect(
        (connection, reason, initiatedByPeer) -> disconnections.put(connection, reason));

    networkRunner.start();
    selfPeer = DefaultPeer.fromEnodeURL(network.getLocalEnode().get());
  }

  private static ChainHeadTracker getChainHeadTracker() {
    final ChainHeadTracker mockCHT = mock(ChainHeadTracker.class);
    final BlockHeader mockBlockHeader = mock(BlockHeader.class);
    Mockito.lenient().when(mockBlockHeader.getNumber()).thenReturn(0L);
    Mockito.lenient()
        .when(mockCHT.getBestHeaderFromPeer(any()))
        .thenReturn(CompletableFuture.completedFuture(mockBlockHeader));
    return mockCHT;
  }

  public Bytes id() {
    return nodeKey.getPublicKey().getEncodedBytes();
  }

  public static String shortId(final Bytes id) {
    return id.slice(62).toString().substring(2);
  }

  public String shortId() {
    return shortId(id());
  }

  public Peer selfPeer() {
    return selfPeer;
  }

  public CompletableFuture<PeerConnection> connect(final TestNode remoteNode) {
    return network.connect(remoteNode.selfPeer());
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public void close() throws IOException {
    IOException firstEx = null;
    try {
      network.close();
    } catch (final IOException e) {
      if (firstEx == null) {
        firstEx = e;
      }
      LOG.warn("Error closing.  Continuing", e);
    }

    if (firstEx != null) {
      throw new IOException("Unable to close successfully.  Wrapping first exception.", firstEx);
    }
  }

  @Override
  public String toString() {
    return shortId()
        + "@"
        + selfPeer.getEnodeURL().getIpAsString()
        + ':'
        + selfPeer.getEnodeURL().getListeningPortOrZero();
  }

  public void receiveRemoteTransaction(final Transaction transaction) {
    transactionPool.addRemoteTransactions(singletonList(transaction));
  }

  public void receiveLocalTransaction(final Transaction transaction) {
    transactionPool.addTransactionViaApi(transaction);
  }

  public int getPendingTransactionCount() {
    return transactionPool.count();
  }
}
