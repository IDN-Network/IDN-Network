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
package org.idnecology.idn.consensus.ibft.statemachine;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.consensus.common.bft.BftContextBuilder.setupContextWithValidators;
import static org.idnecology.idn.consensus.common.bft.ConsensusRoundHelpers.createFrom;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.BftProtocolSchedule;
import org.idnecology.idn.consensus.common.bft.BlockTimer;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.RoundTimer;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftBlockCreator;
import org.idnecology.idn.consensus.common.bft.events.RoundExpiry;
import org.idnecology.idn.consensus.common.bft.network.ValidatorMulticaster;
import org.idnecology.idn.consensus.common.bft.statemachine.BftFinalState;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.consensus.ibft.messagedata.RoundChangeMessageData;
import org.idnecology.idn.consensus.ibft.messagewrappers.Commit;
import org.idnecology.idn.consensus.ibft.messagewrappers.Prepare;
import org.idnecology.idn.consensus.ibft.messagewrappers.Proposal;
import org.idnecology.idn.consensus.ibft.messagewrappers.RoundChange;
import org.idnecology.idn.consensus.ibft.network.IbftMessageTransmitter;
import org.idnecology.idn.consensus.ibft.payload.MessageFactory;
import org.idnecology.idn.consensus.ibft.payload.PreparedCertificate;
import org.idnecology.idn.consensus.ibft.payload.RoundChangeCertificate;
import org.idnecology.idn.consensus.ibft.validation.FutureRoundProposalMessageValidator;
import org.idnecology.idn.consensus.ibft.validation.MessageValidator;
import org.idnecology.idn.consensus.ibft.validation.MessageValidatorFactory;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.BlockCreationTiming;
import org.idnecology.idn.ethereum.blockcreation.BlockCreator.BlockCreationResult;
import org.idnecology.idn.ethereum.blockcreation.txselection.TransactionSelectionResults;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.DefaultBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Util;
import org.idnecology.idn.ethereum.mainnet.DefaultProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.util.Subscribers;

import java.math.BigInteger;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IbftBlockHeightManagerTest {

  private final NodeKey nodeKey = NodeKeyUtils.generate();
  private final MessageFactory messageFactory = new MessageFactory(nodeKey);
  private final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();

  @Mock private BftFinalState finalState;
  @Mock private IbftMessageTransmitter messageTransmitter;
  @Mock private RoundChangeManager roundChangeManager;
  @Mock private IbftRoundFactory roundFactory;
  @Mock private Clock clock;
  @Mock private MessageValidatorFactory messageValidatorFactory;
  @Mock private BftBlockCreator blockCreator;
  @Mock private BlockTimer blockTimer;
  @Mock private DefaultBlockchain blockchain;
  @Mock private RoundTimer roundTimer;
  @Mock private FutureRoundProposalMessageValidator futureRoundProposalMessageValidator;
  @Mock private ValidatorMulticaster validatorMulticaster;
  @Mock private BlockHeader parentHeader;

  @Captor private ArgumentCaptor<MessageData> sentMessageArgCaptor;

  private final List<Address> validators = Lists.newArrayList();
  private final List<MessageFactory> validatorMessageFactory = Lists.newArrayList();
  private final BftExtraDataCodec bftExtraDataCodec = new IbftExtraDataCodec();

  private ProtocolContext protocolContext;
  private final ConsensusRoundIdentifier roundIdentifier = new ConsensusRoundIdentifier(1, 0);
  private Block createdBlock;

  private void buildCreatedBlock() {

    final BftExtraData extraData =
        new BftExtraData(Bytes.wrap(new byte[32]), emptyList(), Optional.empty(), 0, validators);

    headerTestFixture.extraData(new IbftExtraDataCodec().encode(extraData));
    final BlockHeader header = headerTestFixture.buildHeader();
    createdBlock = new Block(header, new BlockBody(emptyList(), emptyList()));
  }

  @BeforeEach
  public void setup() {
    for (int i = 0; i < 3; i++) {
      final NodeKey nodeKey = NodeKeyUtils.generate();
      validators.add(Util.publicKeyToAddress(nodeKey.getPublicKey()));
      validatorMessageFactory.add(new MessageFactory(nodeKey));
    }

    buildCreatedBlock();

    final MessageValidator messageValidator = mock(MessageValidator.class);
    lenient().when(messageValidator.validateProposal(any())).thenReturn(true);
    lenient().when(messageValidator.validateCommit(any())).thenReturn(true);
    lenient().when(messageValidator.validatePrepare(any())).thenReturn(true);
    when(finalState.getBlockTimer()).thenReturn(blockTimer);
    lenient().when(finalState.getQuorum()).thenReturn(3);
    when(finalState.getValidatorMulticaster()).thenReturn(validatorMulticaster);
    lenient()
        .when(blockCreator.createBlock(anyLong(), any()))
        .thenReturn(
            new BlockCreationResult(
                createdBlock, new TransactionSelectionResults(), new BlockCreationTiming()));

    lenient()
        .when(futureRoundProposalMessageValidator.validateProposalMessage(any()))
        .thenReturn(true);
    when(messageValidatorFactory.createFutureRoundProposalMessageValidator(anyLong(), any()))
        .thenReturn(futureRoundProposalMessageValidator);
    lenient()
        .when(messageValidatorFactory.createMessageValidator(any(), any()))
        .thenReturn(messageValidator);

    protocolContext =
        new ProtocolContext(
            blockchain, null, setupContextWithValidators(validators), new BadBlockManager());

    final ProtocolScheduleBuilder protocolScheduleBuilder =
        new ProtocolScheduleBuilder(
            new StubGenesisConfigOptions(),
            Optional.empty(),
            ProtocolSpecAdapters.create(0, Function.identity()),
            new PrivacyParameters(),
            false,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    ProtocolSchedule protocolSchedule =
        new BftProtocolSchedule(
            (DefaultProtocolSchedule) protocolScheduleBuilder.createProtocolSchedule());

    // Ensure the created IbftRound has the valid ConsensusRoundIdentifier;
    when(roundFactory.createNewRound(any(), anyInt()))
        .thenAnswer(
            invocation -> {
              final int round = invocation.getArgument(1);
              final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(1, round);
              final RoundState createdRoundState = new RoundState(roundId, 3, messageValidator);
              return new IbftRound(
                  createdRoundState,
                  blockCreator,
                  protocolContext,
                  protocolSchedule,
                  Subscribers.create(),
                  nodeKey,
                  messageFactory,
                  messageTransmitter,
                  roundTimer,
                  bftExtraDataCodec,
                  parentHeader);
            });

    lenient()
        .when(roundFactory.createNewRoundWithState(any(), any()))
        .thenAnswer(
            invocation -> {
              final RoundState providedRoundState = invocation.getArgument(1);
              return new IbftRound(
                  providedRoundState,
                  blockCreator,
                  protocolContext,
                  protocolSchedule,
                  Subscribers.create(),
                  nodeKey,
                  messageFactory,
                  messageTransmitter,
                  roundTimer,
                  bftExtraDataCodec,
                  parentHeader);
            });
  }

  @Test
  public void startsABlockTimerOnStartIfLocalNodeIsTheProoserForRound() {
    when(finalState.isLocalNodeProposerForRound(any())).thenReturn(true);

    new IbftBlockHeightManager(
        headerTestFixture.buildHeader(),
        finalState,
        roundChangeManager,
        roundFactory,
        clock,
        messageValidatorFactory,
        messageFactory);

    verify(blockTimer, times(1)).startTimer(any(), any());
  }

  @Test
  public void onBlockTimerExpiryProposalMessageIsTransmitted() {
    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);

    manager.handleBlockTimerExpiry(roundIdentifier);
    verify(messageTransmitter, times(1)).multicastProposal(eq(roundIdentifier), any(), any());
    verify(messageTransmitter, never()).multicastPrepare(any(), any());
    verify(messageTransmitter, never()).multicastPrepare(any(), any());
  }

  @Test
  public void onRoundChangeReceptionRoundChangeManagerIsInvokedAndNewRoundStarted() {
    final ConsensusRoundIdentifier futureRoundIdentifier = createFrom(roundIdentifier, 0, +2);
    final RoundChange roundChange =
        messageFactory.createRoundChange(futureRoundIdentifier, Optional.empty());
    when(roundChangeManager.appendRoundChangeMessage(any()))
        .thenReturn(Optional.of(singletonList(roundChange)));
    when(finalState.isLocalNodeProposerForRound(any())).thenReturn(false);

    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);
    verify(roundFactory).createNewRound(any(), eq(0));

    manager.handleRoundChangePayload(roundChange);

    verify(roundChangeManager, times(1)).appendRoundChangeMessage(roundChange);
    verify(roundFactory, times(1))
        .createNewRound(any(), eq(futureRoundIdentifier.getRoundNumber()));
  }

  @Test
  public void onRoundTimerExpiryANewRoundIsCreatedWithAnIncrementedRoundNumber() {
    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);
    verify(roundFactory).createNewRound(any(), eq(0));

    manager.roundExpired(new RoundExpiry(roundIdentifier));
    verify(roundFactory).createNewRound(any(), eq(1));
  }

  @Test
  public void whenSufficientRoundChangesAreReceivedAProposalMessageIsTransmitted() {
    final ConsensusRoundIdentifier futureRoundIdentifier = createFrom(roundIdentifier, 0, +2);
    final RoundChange roundChange =
        messageFactory.createRoundChange(futureRoundIdentifier, Optional.empty());
    final RoundChangeCertificate roundChangCert =
        new RoundChangeCertificate(singletonList(roundChange.getSignedPayload()));

    when(roundChangeManager.appendRoundChangeMessage(any()))
        .thenReturn(Optional.of(singletonList(roundChange)));
    when(finalState.isLocalNodeProposerForRound(any())).thenReturn(true);

    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);
    reset(messageTransmitter);

    manager.handleRoundChangePayload(roundChange);

    verify(messageTransmitter, times(1))
        .multicastProposal(eq(futureRoundIdentifier), any(), eq(Optional.of(roundChangCert)));
  }

  @Test
  public void messagesForFutureRoundsAreBufferedAndUsedToPreloadNewRoundWhenItIsStarted() {
    final ConsensusRoundIdentifier futureRoundIdentifier = createFrom(roundIdentifier, 0, +2);

    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);

    final Prepare prepare =
        validatorMessageFactory
            .get(0)
            .createPrepare(futureRoundIdentifier, Hash.fromHexStringLenient("0"));
    final Commit commit =
        validatorMessageFactory
            .get(1)
            .createCommit(
                futureRoundIdentifier,
                Hash.fromHexStringLenient("0"),
                SignatureAlgorithmFactory.getInstance()
                    .createSignature(BigInteger.ONE, BigInteger.ONE, (byte) 1));

    manager.handlePreparePayload(prepare);
    manager.handleCommitPayload(commit);

    // Force a new round to be started at new round number.
    final Proposal futureRoundProposal =
        messageFactory.createProposal(
            futureRoundIdentifier,
            createdBlock,
            Optional.of(new RoundChangeCertificate(Collections.emptyList())));

    manager.handleProposalPayload(futureRoundProposal);

    // Final state sets the Quorum Size to 3, so should send a Prepare and also a commit
    verify(messageTransmitter, times(1)).multicastPrepare(eq(futureRoundIdentifier), any());
    verify(messageTransmitter, times(1)).multicastPrepare(eq(futureRoundIdentifier), any());
  }

  @Test
  public void preparedCertificateIncludedInRoundChangeMessageOnRoundTimeoutExpired() {
    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);

    manager.handleBlockTimerExpiry(roundIdentifier); // Trigger a Proposal creation.

    final Prepare firstPrepare =
        validatorMessageFactory
            .get(0)
            .createPrepare(roundIdentifier, Hash.fromHexStringLenient("0"));
    final Prepare secondPrepare =
        validatorMessageFactory
            .get(1)
            .createPrepare(roundIdentifier, Hash.fromHexStringLenient("0"));
    manager.handlePreparePayload(firstPrepare);
    manager.handlePreparePayload(secondPrepare);

    manager.roundExpired(new RoundExpiry(roundIdentifier));

    verify(validatorMulticaster, times(1)).send(sentMessageArgCaptor.capture());
    final MessageData capturedMessageData = sentMessageArgCaptor.getValue();

    assertThat(capturedMessageData).isInstanceOf(RoundChangeMessageData.class);
    final RoundChangeMessageData roundChange = (RoundChangeMessageData) capturedMessageData;

    Optional<PreparedCertificate> preparedCert = roundChange.decode().getPreparedCertificate();
    Assertions.assertThat(preparedCert).isNotEmpty();

    assertThat(preparedCert.get().getPreparePayloads())
        .containsOnly(firstPrepare.getSignedPayload(), secondPrepare.getSignedPayload());
  }

  @Test
  public void illegalFutureRoundProposalDoesNotTriggerNewRound() {
    when(futureRoundProposalMessageValidator.validateProposalMessage(any())).thenReturn(false);

    final ConsensusRoundIdentifier futureRoundIdentifier = createFrom(roundIdentifier, 0, +2);

    final IbftBlockHeightManager manager =
        new IbftBlockHeightManager(
            headerTestFixture.buildHeader(),
            finalState,
            roundChangeManager,
            roundFactory,
            clock,
            messageValidatorFactory,
            messageFactory);

    // Force a new round to be started at new round number.
    final Proposal futureRoundProposal =
        messageFactory.createProposal(
            futureRoundIdentifier,
            createdBlock,
            Optional.of(new RoundChangeCertificate(Collections.emptyList())));
    reset(roundFactory); // Discard the existing createNewRound invocation.

    manager.handleProposalPayload(futureRoundProposal);
    verify(roundFactory, never()).createNewRound(any(), anyInt());
  }
}
