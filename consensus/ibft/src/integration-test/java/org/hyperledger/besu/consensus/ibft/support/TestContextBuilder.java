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
package org.idnecology.idn.consensus.ibft.support;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.BftConfigOptions;
import org.idnecology.idn.config.BftFork;
import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftContext;
import org.idnecology.idn.consensus.common.bft.BftEventQueue;
import org.idnecology.idn.consensus.common.bft.BftExecutors;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftHelpers;
import org.idnecology.idn.consensus.common.bft.BftProtocolSchedule;
import org.idnecology.idn.consensus.common.bft.BlockTimer;
import org.idnecology.idn.consensus.common.bft.EventMultiplexer;
import org.idnecology.idn.consensus.common.bft.Gossiper;
import org.idnecology.idn.consensus.common.bft.MessageTracker;
import org.idnecology.idn.consensus.common.bft.RoundTimer;
import org.idnecology.idn.consensus.common.bft.SynchronizerUpdater;
import org.idnecology.idn.consensus.common.bft.UniqueMessageMulticaster;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.idnecology.idn.consensus.common.bft.blockcreation.ProposerSelector;
import org.idnecology.idn.consensus.common.bft.inttest.DefaultValidatorPeer;
import org.idnecology.idn.consensus.common.bft.inttest.NetworkLayout;
import org.idnecology.idn.consensus.common.bft.inttest.NodeParams;
import org.idnecology.idn.consensus.common.bft.inttest.StubValidatorMulticaster;
import org.idnecology.idn.consensus.common.bft.inttest.StubbedSynchronizerUpdater;
import org.idnecology.idn.consensus.common.bft.inttest.TestTransitions;
import org.idnecology.idn.consensus.common.bft.statemachine.BftEventHandler;
import org.idnecology.idn.consensus.common.bft.statemachine.BftFinalState;
import org.idnecology.idn.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.consensus.ibft.IbftForksSchedulesFactory;
import org.idnecology.idn.consensus.ibft.IbftGossip;
import org.idnecology.idn.consensus.ibft.IbftProtocolScheduleBuilder;
import org.idnecology.idn.consensus.ibft.payload.MessageFactory;
import org.idnecology.idn.consensus.ibft.statemachine.IbftBlockHeightManagerFactory;
import org.idnecology.idn.consensus.ibft.statemachine.IbftController;
import org.idnecology.idn.consensus.ibft.statemachine.IbftRoundFactory;
import org.idnecology.idn.consensus.ibft.validation.MessageValidatorFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MinedBlockObserver;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.GasPricePendingTransactionsSorter;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;
import org.idnecology.idn.testutil.TestClock;
import org.idnecology.idn.util.Subscribers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.apache.tuweni.bytes.Bytes;

public class TestContextBuilder {

  private static final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  private static class ControllerAndState {

    private final BftExecutors bftExecutors;
    private final BftEventHandler eventHandler;
    private final BftFinalState finalState;
    private final EventMultiplexer eventMultiplexer;
    private final MessageFactory messageFactory;

    public ControllerAndState(
        final BftExecutors bftExecutors,
        final BftEventHandler eventHandler,
        final BftFinalState finalState,
        final EventMultiplexer eventMultiplexer,
        final MessageFactory messageFactory) {
      this.bftExecutors = bftExecutors;
      this.eventHandler = eventHandler;
      this.finalState = finalState;
      this.eventMultiplexer = eventMultiplexer;
      this.messageFactory = messageFactory;
    }

    public BftExecutors getIbftExecutors() {
      return bftExecutors;
    }

    public BftEventHandler getEventHandler() {
      return eventHandler;
    }

    public BftFinalState getFinalState() {
      return finalState;
    }

    public EventMultiplexer getEventMultiplexer() {
      return eventMultiplexer;
    }

    public MessageFactory getMessageFactory() {
      return messageFactory;
    }
  }

  public static final int EPOCH_LENGTH = 10_000;
  public static final int BLOCK_TIMER_SEC = 3;
  public static final int ROUND_TIMER_SEC = 12;
  public static final int MESSAGE_QUEUE_LIMIT = 1000;
  public static final int GOSSIPED_HISTORY_LIMIT = 100;
  public static final int DUPLICATE_MESSAGE_LIMIT = 100;
  public static final int FUTURE_MESSAGES_MAX_DISTANCE = 10;
  public static final int FUTURE_MESSAGES_LIMIT = 1000;

  private Clock clock = Clock.fixed(Instant.MIN, ZoneId.of("UTC"));
  private BftEventQueue bftEventQueue = new BftEventQueue(MESSAGE_QUEUE_LIMIT);
  private int validatorCount = 4;
  private int indexOfFirstLocallyProposedBlock = 0; // Meaning first block is from remote peer.
  private boolean useGossip = false;
  private List<BftFork> bftForks = Collections.emptyList();
  private static final IbftExtraDataCodec IBFT_EXTRA_DATA_ENCODER = new IbftExtraDataCodec();

  public TestContextBuilder clock(final Clock clock) {
    this.clock = clock;
    return this;
  }

  public TestContextBuilder eventQueue(final BftEventQueue bftEventQueue) {
    this.bftEventQueue = bftEventQueue;
    return this;
  }

  public TestContextBuilder validatorCount(final int validatorCount) {
    this.validatorCount = validatorCount;
    return this;
  }

  public TestContextBuilder indexOfFirstLocallyProposedBlock(
      final int indexOfFirstLocallyProposedBlock) {
    this.indexOfFirstLocallyProposedBlock = indexOfFirstLocallyProposedBlock;
    return this;
  }

  public TestContextBuilder useGossip(final boolean useGossip) {
    this.useGossip = useGossip;
    return this;
  }

  public TestContextBuilder bftForks(final List<BftFork> bftForks) {
    this.bftForks = bftForks;
    return this;
  }

  public TestContext build() {
    final NetworkLayout networkNodes =
        NetworkLayout.createNetworkLayout(validatorCount, indexOfFirstLocallyProposedBlock);

    final Block genesisBlock = createGenesisBlock(networkNodes.getValidatorAddresses());

    final MutableBlockchain blockChain =
        createInMemoryBlockchain(
            genesisBlock, BftBlockHeaderFunctions.forOnchainBlock(IBFT_EXTRA_DATA_ENCODER));

    // Use a stubbed version of the multicaster, to prevent creating PeerConnections etc.
    final StubValidatorMulticaster multicaster = new StubValidatorMulticaster();
    final UniqueMessageMulticaster uniqueMulticaster =
        new UniqueMessageMulticaster(multicaster, GOSSIPED_HISTORY_LIMIT);

    final Gossiper gossiper = useGossip ? new IbftGossip(uniqueMulticaster) : mock(Gossiper.class);

    final StubbedSynchronizerUpdater synchronizerUpdater = new StubbedSynchronizerUpdater();

    final ControllerAndState controllerAndState =
        createControllerAndFinalState(
            blockChain,
            multicaster,
            networkNodes.getLocalNode().getNodeKey(),
            clock,
            bftEventQueue,
            gossiper,
            synchronizerUpdater,
            bftForks);

    // Add each networkNode to the Multicaster (such that each can receive msgs from local node).
    // NOTE: the remotePeers needs to be ordered based on Address (as this is used to determine
    // the proposer order which must be managed in test).
    final Map<Address, ValidatorPeer> remotePeers =
        networkNodes.getRemotePeers().stream()
            .collect(
                Collectors.toMap(
                    NodeParams::getAddress,
                    nodeParams ->
                        new ValidatorPeer(
                            nodeParams,
                            new MessageFactory(nodeParams.getNodeKey()),
                            controllerAndState.getEventMultiplexer()),
                    (u, v) -> {
                      throw new IllegalStateException(String.format("Duplicate key %s", u));
                    },
                    LinkedHashMap::new));

    final List<DefaultValidatorPeer> peerCollection = new ArrayList<>(remotePeers.values());
    multicaster.addNetworkPeers(peerCollection);
    synchronizerUpdater.addNetworkPeers(peerCollection);

    return new TestContext(
        remotePeers,
        blockChain,
        controllerAndState.getIbftExecutors(),
        controllerAndState.getEventHandler(),
        controllerAndState.getFinalState(),
        controllerAndState.getEventMultiplexer(),
        controllerAndState.getMessageFactory());
  }

  public TestContext buildAndStart() {
    TestContext testContext = build();
    testContext.start();
    return testContext;
  }

  private static Block createGenesisBlock(final Set<Address> validators) {
    final Address coinbase = Iterables.get(validators, 0);
    final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();
    final BftExtraData extraData =
        new BftExtraData(
            Bytes.wrap(new byte[32]), Collections.emptyList(), Optional.empty(), 0, validators);
    headerTestFixture.extraData(IBFT_EXTRA_DATA_ENCODER.encode(extraData));
    headerTestFixture.mixHash(BftHelpers.EXPECTED_MIX_HASH);
    headerTestFixture.difficulty(Difficulty.ONE);
    headerTestFixture.ommersHash(Hash.EMPTY_LIST_HASH);
    headerTestFixture.nonce(0);
    headerTestFixture.timestamp(0);
    headerTestFixture.parentHash(Hash.ZERO);
    headerTestFixture.gasLimit(5000);
    headerTestFixture.coinbase(coinbase);

    final BlockHeader genesisHeader = headerTestFixture.buildHeader();
    return new Block(
        genesisHeader, new BlockBody(Collections.emptyList(), Collections.emptyList()));
  }

  private static ControllerAndState createControllerAndFinalState(
      final MutableBlockchain blockChain,
      final StubValidatorMulticaster multicaster,
      final NodeKey nodeKey,
      final Clock clock,
      final BftEventQueue bftEventQueue,
      final Gossiper gossiper,
      final SynchronizerUpdater synchronizerUpdater,
      final List<BftFork> bftForks) {

    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();

    final MiningConfiguration miningParams =
        ImmutableMiningConfiguration.builder()
            .mutableInitValues(
                MutableInitValues.builder()
                    .isMiningEnabled(true)
                    .minTransactionGasPrice(Wei.ZERO)
                    .extraData(Bytes.wrap("Ibft Int tests".getBytes(UTF_8)))
                    .coinbase(AddressHelpers.ofValue(1))
                    .build())
            .build();

    final StubGenesisConfigOptions genesisConfigOptions = new StubGenesisConfigOptions();
    genesisConfigOptions.byzantiumBlock(0);
    genesisConfigOptions.transitions(TestTransitions.createIbftTestTransitions(bftForks));

    final ForksSchedule<BftConfigOptions> forksSchedule =
        IbftForksSchedulesFactory.create(genesisConfigOptions);

    final BftProtocolSchedule protocolSchedule =
        IbftProtocolScheduleBuilder.create(
            genesisConfigOptions,
            forksSchedule,
            IBFT_EXTRA_DATA_ENCODER,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    /////////////////////////////////////////////////////////////////////////////////////
    // From here down is BASICALLY taken from IbftIdnController
    final EpochManager epochManager = new EpochManager(EPOCH_LENGTH);

    final BftBlockInterface blockInterface = new BftBlockInterface(IBFT_EXTRA_DATA_ENCODER);

    final ValidatorProvider validatorProvider =
        BlockValidatorProvider.nonForkingValidatorProvider(
            blockChain, epochManager, blockInterface);

    final ProtocolContext protocolContext =
        new ProtocolContext(
            blockChain,
            worldStateArchive,
            new BftContext(validatorProvider, epochManager, blockInterface),
            new BadBlockManager());
    final TransactionPoolConfiguration poolConf =
        ImmutableTransactionPoolConfiguration.builder().txPoolMaxSize(1).build();

    final GasPricePendingTransactionsSorter pendingTransactions =
        new GasPricePendingTransactionsSorter(
            poolConf, clock, metricsSystem, blockChain::getChainHeadHeader);

    final EthContext ethContext = mock(EthContext.class, RETURNS_DEEP_STUBS);
    when(ethContext.getEthPeers().subscribeConnect(any())).thenReturn(1L);

    final TransactionPool transactionPool =
        new TransactionPool(
            () -> pendingTransactions,
            protocolSchedule,
            protocolContext,
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(metricsSystem),
            poolConf,
            new BlobCache());

    transactionPool.setEnabled();

    final EthScheduler ethScheduler = new DeterministicEthScheduler();

    final Address localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    final BftBlockCreatorFactory<?> blockCreatorFactory =
        new BftBlockCreatorFactory<>(
            transactionPool, // changed from IbftIdnController
            protocolContext,
            protocolSchedule,
            forksSchedule,
            miningParams,
            localAddress,
            IBFT_EXTRA_DATA_ENCODER,
            ethScheduler);

    final ProposerSelector proposerSelector =
        new ProposerSelector(blockChain, blockInterface, true, validatorProvider);

    final BftExecutors bftExecutors =
        BftExecutors.create(new NoOpMetricsSystem(), BftExecutors.ConsensusType.IBFT);
    final BftFinalState finalState =
        new BftFinalState(
            protocolContext.getConsensusContext(BftContext.class).getValidatorProvider(),
            nodeKey,
            Util.publicKeyToAddress(nodeKey.getPublicKey()),
            proposerSelector,
            multicaster,
            new RoundTimer(bftEventQueue, Duration.ofSeconds(ROUND_TIMER_SEC), bftExecutors),
            new BlockTimer(bftEventQueue, forksSchedule, bftExecutors, TestClock.fixed()),
            blockCreatorFactory,
            clock);

    final MessageFactory messageFactory = new MessageFactory(nodeKey);

    final MessageValidatorFactory messageValidatorFactory =
        new MessageValidatorFactory(
            proposerSelector, protocolSchedule, protocolContext, IBFT_EXTRA_DATA_ENCODER);

    final Subscribers<MinedBlockObserver> minedBlockObservers = Subscribers.create();

    final MessageTracker duplicateMessageTracker = new MessageTracker(DUPLICATE_MESSAGE_LIMIT);
    final FutureMessageBuffer futureMessageBuffer =
        new FutureMessageBuffer(
            FUTURE_MESSAGES_MAX_DISTANCE,
            FUTURE_MESSAGES_LIMIT,
            blockChain.getChainHeadBlockNumber());

    final BftEventHandler ibftController =
        new IbftController(
            blockChain,
            finalState,
            new IbftBlockHeightManagerFactory(
                finalState,
                new IbftRoundFactory(
                    finalState,
                    protocolContext,
                    protocolSchedule,
                    minedBlockObservers,
                    messageValidatorFactory,
                    messageFactory,
                    IBFT_EXTRA_DATA_ENCODER),
                messageValidatorFactory,
                messageFactory),
            gossiper,
            duplicateMessageTracker,
            futureMessageBuffer,
            synchronizerUpdater);

    final EventMultiplexer eventMultiplexer = new EventMultiplexer(ibftController);
    //////////////////////////// END IBFT IdnController ////////////////////////////

    return new ControllerAndState(
        bftExecutors, ibftController, finalState, eventMultiplexer, messageFactory);
  }
}
