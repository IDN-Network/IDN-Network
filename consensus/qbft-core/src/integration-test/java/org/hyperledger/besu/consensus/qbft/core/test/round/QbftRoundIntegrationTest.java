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
package org.idnecology.idn.consensus.qbft.core.test.round;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.RoundTimer;
import org.idnecology.idn.consensus.common.bft.inttest.StubValidatorMulticaster;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.adaptor.QbftBlockAdaptor;
import org.idnecology.idn.consensus.qbft.adaptor.QbftBlockInterfaceAdaptor;
import org.idnecology.idn.consensus.qbft.core.network.QbftMessageTransmitter;
import org.idnecology.idn.consensus.qbft.core.payload.MessageFactory;
import org.idnecology.idn.consensus.qbft.core.statemachine.QbftRound;
import org.idnecology.idn.consensus.qbft.core.statemachine.RoundState;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCreator;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockImporter;
import org.idnecology.idn.consensus.qbft.core.types.QbftContext;
import org.idnecology.idn.consensus.qbft.core.types.QbftMinedBlockObserver;
import org.idnecology.idn.consensus.qbft.core.types.QbftProtocolSchedule;
import org.idnecology.idn.consensus.qbft.core.validation.MessageValidator;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.plugin.services.securitymodule.SecurityModuleException;
import org.idnecology.idn.util.Subscribers;

import java.math.BigInteger;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QbftRoundIntegrationTest {

  private final ConsensusRoundIdentifier roundIdentifier = new ConsensusRoundIdentifier(1, 0);
  private final Subscribers<QbftMinedBlockObserver> subscribers = Subscribers.create();
  private final BftExtraDataCodec bftExtraDataCodec = new QbftExtraDataCodec();
  private MessageFactory peerMessageFactory;
  private MessageFactory peerMessageFactory2;
  private ProtocolContext protocolContext;

  @Mock private QbftProtocolSchedule protocolSchedule;
  @Mock private MutableBlockchain blockChain;
  @Mock private WorldStateArchive worldStateArchive;
  @Mock private QbftBlockImporter blockImporter;

  @Mock private QbftBlockCreator blockCreator;
  @Mock private MessageValidator messageValidator;
  @Mock private RoundTimer roundTimer;
  @Mock private NodeKey nodeKey;
  private MessageFactory throwingMessageFactory;
  private QbftMessageTransmitter transmitter;
  @Mock private StubValidatorMulticaster multicaster;
  @Mock private QbftBlockHeader parentHeader;
  @Mock private QbftBlockCodec blockEncoder;

  private QbftBlock proposedBlock;

  private final SECPSignature remoteCommitSeal =
      SignatureAlgorithmFactory.getInstance()
          .createSignature(BigInteger.ONE, BigInteger.ONE, (byte) 1);

  @BeforeEach
  public void setup() {
    peerMessageFactory = new MessageFactory(NodeKeyUtils.generate(), blockEncoder);
    peerMessageFactory2 = new MessageFactory(NodeKeyUtils.generate(), blockEncoder);

    when(messageValidator.validateProposal(any())).thenReturn(true);
    when(messageValidator.validatePrepare(any())).thenReturn(true);
    when(messageValidator.validateCommit(any())).thenReturn(true);

    when(nodeKey.sign(any())).thenThrow(new SecurityModuleException("Hsm Is Down"));

    final QbftExtraDataCodec qbftExtraDataEncoder = new QbftExtraDataCodec();
    throwingMessageFactory = new MessageFactory(nodeKey, blockEncoder);
    transmitter = new QbftMessageTransmitter(throwingMessageFactory, multicaster);

    final BftExtraData proposedExtraData =
        new BftExtraData(Bytes.wrap(new byte[32]), emptyList(), empty(), 0, emptyList());
    final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();
    headerTestFixture.extraData(qbftExtraDataEncoder.encode(proposedExtraData));
    headerTestFixture.number(1);
    final BlockHeader header = headerTestFixture.buildHeader();
    final Block block = new Block(header, new BlockBody(emptyList(), emptyList()));
    proposedBlock = new QbftBlockAdaptor(block);

    when(protocolSchedule.getBlockImporter(any())).thenReturn(blockImporter);

    when(blockImporter.importBlock(any())).thenReturn(true);

    protocolContext =
        new ProtocolContext(
            blockChain,
            worldStateArchive,
            new QbftContext(
                null, new QbftBlockInterfaceAdaptor(new BftBlockInterface(qbftExtraDataEncoder))),
            new BadBlockManager());
  }

  @Test
  public void signingFailsOnReceiptOfProposalUpdatesRoundButTransmitsNothing() {
    final int QUORUM_SIZE = 1;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            throwingMessageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    round.handleProposalMessage(
        peerMessageFactory.createProposal(
            roundIdentifier, proposedBlock, emptyList(), emptyList()));

    assertThat(roundState.getProposedBlock()).isNotEmpty();
    assertThat(roundState.isPrepared()).isFalse();
    assertThat(roundState.isCommitted()).isFalse();

    verifyNoInteractions(multicaster);
  }

  @Test
  public void failuresToSignStillAllowBlockToBeImported() {
    final BlockHeader header = new BlockHeaderTestFixture().number(1).buildHeader();
    final Block sealedIdnBlock = new Block(header, new BlockBody(emptyList(), emptyList()));
    final QbftBlock sealedBlock = new QbftBlockAdaptor(sealedIdnBlock);
    when(blockCreator.createSealedBlock(
            proposedBlock,
            roundIdentifier.getRoundNumber(),
            List.of(remoteCommitSeal, remoteCommitSeal)))
        .thenReturn(sealedBlock);

    final int QUORUM_SIZE = 2;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final QbftRound round =
        new QbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            throwingMessageFactory,
            transmitter,
            roundTimer,
            bftExtraDataCodec,
            parentHeader);

    // inject a block first, then a prepare on it.
    round.handleProposalMessage(
        peerMessageFactory.createProposal(
            roundIdentifier, proposedBlock, emptyList(), emptyList()));

    assertThat(roundState.getProposedBlock()).isNotEmpty();
    assertThat(roundState.isPrepared()).isFalse();
    assertThat(roundState.isCommitted()).isFalse();

    round.handlePrepareMessage(peerMessageFactory.createPrepare(roundIdentifier, Hash.EMPTY));

    assertThat(roundState.getProposedBlock()).isNotEmpty();
    assertThat(roundState.isPrepared()).isFalse();
    assertThat(roundState.isCommitted()).isFalse();
    verifyNoInteractions(multicaster);

    round.handleCommitMessage(
        peerMessageFactory.createCommit(roundIdentifier, Hash.EMPTY, remoteCommitSeal));
    assertThat(roundState.isCommitted()).isFalse();
    verifyNoInteractions(multicaster);

    round.handleCommitMessage(
        peerMessageFactory2.createCommit(roundIdentifier, Hash.EMPTY, remoteCommitSeal));
    assertThat(roundState.isCommitted()).isTrue();
    verifyNoInteractions(multicaster);

    verify(blockImporter).importBlock(any());
  }
}
