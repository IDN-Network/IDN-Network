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
package org.idnecology.idn.consensus.ibft.tests.round;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.consensus.common.bft.BftContextBuilder.setupContextWithBftExtraDataEncoder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftProtocolSchedule;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.RoundTimer;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftBlockCreator;
import org.idnecology.idn.consensus.common.bft.inttest.StubValidatorMulticaster;
import org.idnecology.idn.consensus.ibft.IbftExtraDataCodec;
import org.idnecology.idn.consensus.ibft.network.IbftMessageTransmitter;
import org.idnecology.idn.consensus.ibft.payload.MessageFactory;
import org.idnecology.idn.consensus.ibft.statemachine.IbftRound;
import org.idnecology.idn.consensus.ibft.statemachine.RoundState;
import org.idnecology.idn.consensus.ibft.validation.MessageValidator;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MinedBlockObserver;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.mainnet.BlockImportResult;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.plugin.services.securitymodule.SecurityModuleException;
import org.idnecology.idn.util.Subscribers;

import java.math.BigInteger;
import java.util.Optional;

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
public class IbftRoundIntegrationTest {

  private final MessageFactory peerMessageFactory = new MessageFactory(NodeKeyUtils.generate());
  private final MessageFactory peerMessageFactory2 = new MessageFactory(NodeKeyUtils.generate());
  private final ConsensusRoundIdentifier roundIdentifier = new ConsensusRoundIdentifier(1, 0);
  private final Subscribers<MinedBlockObserver> subscribers = Subscribers.create();
  private ProtocolContext protocolContext;

  @Mock private BftProtocolSchedule protocolSchedule;
  @Mock private ProtocolSpec protocolSpec;
  @Mock private MutableBlockchain blockChain;
  @Mock private WorldStateArchive worldStateArchive;
  @Mock private BlockImporter blockImporter;

  @Mock private BftBlockCreator blockCreator;
  @Mock private MessageValidator messageValidator;
  @Mock private RoundTimer roundTimer;
  @Mock private NodeKey nodeKey;
  private MessageFactory throwingMessageFactory;
  private IbftMessageTransmitter transmitter;
  @Mock private StubValidatorMulticaster multicaster;
  @Mock BlockHeader parentHeader;

  private Block proposedBlock;

  private final SECPSignature remoteCommitSeal =
      SignatureAlgorithmFactory.getInstance()
          .createSignature(BigInteger.ONE, BigInteger.ONE, (byte) 1);
  private final IbftExtraDataCodec bftExtraDataEncoder = new IbftExtraDataCodec();

  @BeforeEach
  public void setup() {
    when(messageValidator.validateProposal(any())).thenReturn(true);
    when(messageValidator.validatePrepare(any())).thenReturn(true);
    when(messageValidator.validateCommit(any())).thenReturn(true);

    when(nodeKey.sign(any())).thenThrow(new SecurityModuleException("Hsm Is Down"));

    throwingMessageFactory = new MessageFactory(nodeKey);
    transmitter = new IbftMessageTransmitter(throwingMessageFactory, multicaster);

    final BftExtraData proposedExtraData =
        new BftExtraData(Bytes.wrap(new byte[32]), emptyList(), empty(), 0, emptyList());
    final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();
    headerTestFixture.extraData(bftExtraDataEncoder.encode(proposedExtraData));
    headerTestFixture.number(1);
    final BlockHeader header = headerTestFixture.buildHeader();
    proposedBlock = new Block(header, new BlockBody(emptyList(), emptyList()));

    when(protocolSchedule.getByBlockHeader(any())).thenReturn(protocolSpec);
    when(protocolSpec.getBlockImporter()).thenReturn(blockImporter);

    when(blockImporter.importBlock(any(), any(), any())).thenReturn(new BlockImportResult(true));

    protocolContext =
        new ProtocolContext(
            blockChain,
            worldStateArchive,
            setupContextWithBftExtraDataEncoder(emptyList(), bftExtraDataEncoder),
            new BadBlockManager());
  }

  @Test
  public void signingFailsOnReceiptOfProposalUpdatesRoundButTransmitsNothing() {
    final int QUORUM_SIZE = 1;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final IbftRound round =
        new IbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            throwingMessageFactory,
            transmitter,
            roundTimer,
            bftExtraDataEncoder,
            parentHeader);

    round.handleProposalMessage(
        peerMessageFactory.createProposal(roundIdentifier, proposedBlock, Optional.empty()));

    assertThat(roundState.getProposedBlock()).isNotEmpty();
    assertThat(roundState.isPrepared()).isTrue();
    assertThat(roundState.isCommitted()).isFalse();

    verifyNoInteractions(multicaster);
  }

  @Test
  public void failuresToSignStillAllowBlockToBeImported() {
    final int QUORUM_SIZE = 2;
    final RoundState roundState = new RoundState(roundIdentifier, QUORUM_SIZE, messageValidator);
    final IbftRound round =
        new IbftRound(
            roundState,
            blockCreator,
            protocolContext,
            protocolSchedule,
            subscribers,
            nodeKey,
            throwingMessageFactory,
            transmitter,
            roundTimer,
            bftExtraDataEncoder,
            parentHeader);

    // inject a block first, then a prepare on it.
    round.handleProposalMessage(
        peerMessageFactory.createProposal(roundIdentifier, proposedBlock, Optional.empty()));

    assertThat(roundState.getProposedBlock()).isNotEmpty();
    assertThat(roundState.isPrepared()).isFalse();
    assertThat(roundState.isCommitted()).isFalse();

    round.handlePrepareMessage(peerMessageFactory.createPrepare(roundIdentifier, Hash.EMPTY));

    assertThat(roundState.getProposedBlock()).isNotEmpty();
    assertThat(roundState.isPrepared()).isTrue();
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

    verify(blockImporter).importBlock(any(), any(), any());
  }
}
