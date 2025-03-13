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
package org.idnecology.idn.consensus.qbft.core.support;

import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.consensus.common.bft.BftExecutors;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.BftHelpers;
import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.EventMultiplexer;
import org.idnecology.idn.consensus.common.bft.blockcreation.ProposerSelector;
import org.idnecology.idn.consensus.common.bft.inttest.NodeParams;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.qbft.adaptor.BlockUtil;
import org.idnecology.idn.consensus.qbft.adaptor.QbftBlockAdaptor;
import org.idnecology.idn.consensus.qbft.adaptor.QbftBlockHeaderAdaptor;
import org.idnecology.idn.consensus.qbft.adaptor.QbftBlockchainAdaptor;
import org.idnecology.idn.consensus.qbft.adaptor.QbftValidatorProviderAdaptor;
import org.idnecology.idn.consensus.qbft.core.payload.MessageFactory;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockchain;
import org.idnecology.idn.consensus.qbft.core.types.QbftEventHandler;
import org.idnecology.idn.consensus.qbft.core.types.QbftFinalState;
import org.idnecology.idn.consensus.qbft.core.types.QbftValidatorProvider;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/*
Responsible for creating an environment in which integration testing can be conducted.

The test setup is an 'n' node network, one of which is the local node (i.e. the Unit Under Test).

There is some complexity with determining the which node is the proposer etc. THus necessitating
NetworkLayout and RoundSpecificNodeRoles concepts.
 */
public class TestContext {

  private final Map<Address, ValidatorPeer> remotePeers;
  private final MutableBlockchain blockchain;
  private final BftExecutors bftExecutors;
  private final QbftEventHandler controller;
  private final QbftFinalState finalState;
  private final EventMultiplexer eventMultiplexer;
  private final MessageFactory messageFactory;
  private final ValidatorProvider validatorProvider;
  private final ProposerSelector proposerSelector;
  private final BftExtraDataCodec bftExtraDataCodec;
  private final QbftBlockCodec blockEncoder;

  public TestContext(
      final Map<Address, ValidatorPeer> remotePeers,
      final MutableBlockchain blockchain,
      final BftExecutors bftExecutors,
      final QbftEventHandler controller,
      final QbftFinalState finalState,
      final EventMultiplexer eventMultiplexer,
      final MessageFactory messageFactory,
      final ValidatorProvider validatorProvider,
      final ProposerSelector proposerSelector,
      final BftExtraDataCodec bftExtraDataCodec,
      final QbftBlockCodec blockEncoder) {
    this.remotePeers = remotePeers;
    this.blockchain = blockchain;
    this.bftExecutors = bftExecutors;
    this.controller = controller;
    this.finalState = finalState;
    this.eventMultiplexer = eventMultiplexer;
    this.messageFactory = messageFactory;
    this.validatorProvider = validatorProvider;
    this.proposerSelector = proposerSelector;
    this.bftExtraDataCodec = bftExtraDataCodec;
    this.blockEncoder = blockEncoder;
  }

  public void start() {
    bftExecutors.start();
    controller.start();
  }

  public QbftBlockchain getBlockchain() {
    return new QbftBlockchainAdaptor(blockchain);
  }

  public QbftEventHandler getController() {
    return controller;
  }

  public EventMultiplexer getEventMultiplexer() {
    return eventMultiplexer;
  }

  public MessageFactory getLocalNodeMessageFactory() {
    return messageFactory;
  }

  public QbftBlockCodec getBlockEncoder() {
    return blockEncoder;
  }

  public QbftBlock createBlockForProposalFromChainHead(final long timestamp) {
    return createBlockForProposalFromChainHead(timestamp, finalState.getLocalAddress(), 0);
  }

  public QbftBlock createBlockForProposalFromChainHead(
      final long timestamp, final int roundNumber) {
    return createBlockForProposalFromChainHead(
        timestamp, finalState.getLocalAddress(), roundNumber);
  }

  public QbftBlock createBlockForProposalFromChainHead(
      final long timestamp, final Address proposer) {
    // this implies that EVERY block will have this node as the proposer :/
    return createBlockForProposal(
        new QbftBlockHeaderAdaptor(blockchain.getChainHeadHeader()), timestamp, proposer, 0);
  }

  public QbftBlock createBlockForProposalFromChainHead(
      final long timestamp, final Address proposer, final int roundNumber) {
    // this implies that EVERY block will have this node as the proposer :/
    return createBlockForProposal(
        new QbftBlockHeaderAdaptor(blockchain.getChainHeadHeader()),
        timestamp,
        proposer,
        roundNumber);
  }

  public QbftBlock createBlockForProposal(
      final QbftBlockHeader parent,
      final long timestamp,
      final Address proposer,
      final int roundNumber) {
    final QbftBlock block =
        finalState.getBlockCreatorFactory().create(roundNumber).createBlock(timestamp, parent);

    final BlockHeaderBuilder headerBuilder =
        BlockHeaderBuilder.fromHeader(BlockUtil.toIdnBlockHeader(block.getHeader()));
    headerBuilder
        .coinbase(proposer)
        .blockHeaderFunctions(BftBlockHeaderFunctions.forCommittedSeal(bftExtraDataCodec));
    final BlockHeader newHeader = headerBuilder.buildBlockHeader();
    final Block newBlock =
        new Block(newHeader, new BlockBody(Collections.emptyList(), Collections.emptyList()));
    return new QbftBlockAdaptor(newBlock);
  }

  public QbftBlock createBlockForProposal(
      final QbftBlockHeader parent, final long timestamp, final Address proposer) {
    return createBlockForProposal(parent, timestamp, proposer, 0);
  }

  public QbftBlock createSealedBlock(
      final BftExtraDataCodec bftExtraDataCodec,
      final QbftBlock block,
      final int roundNumber,
      final Collection<SECPSignature> commitSeals) {
    final Block sealedBlock =
        BftHelpers.createSealedBlock(
            bftExtraDataCodec, BlockUtil.toIdnBlock(block), roundNumber, commitSeals);
    return new QbftBlockAdaptor(sealedBlock);
  }

  public RoundSpecificPeers roundSpecificPeers(final ConsensusRoundIdentifier roundId) {
    // This will return NULL if the LOCAL node is the proposer for the specified round
    final Address proposerAddress = proposerSelector.selectProposerForRound(roundId);
    final ValidatorPeer proposer = remotePeers.getOrDefault(proposerAddress, null);

    final List<ValidatorPeer> nonProposers = new ArrayList<>(remotePeers.values());
    nonProposers.remove(proposer);

    return new RoundSpecificPeers(proposer, remotePeers.values(), nonProposers, blockEncoder);
  }

  public NodeParams getLocalNodeParams() {
    return new NodeParams(finalState.getLocalAddress(), finalState.getNodeKey());
  }

  public long getCurrentChainHeight() {
    return blockchain.getChainHeadBlockNumber();
  }

  public QbftValidatorProvider getValidatorProvider() {
    return new QbftValidatorProviderAdaptor(validatorProvider);
  }

  public void appendBlock(final QbftBlock signedCurrentHeightBlock) {
    blockchain.appendBlock(
        BlockUtil.toIdnBlock(signedCurrentHeightBlock), Collections.emptyList());
  }

  public QbftBlockHeader getBlockHeader(final int blockNumber) {
    return new QbftBlockHeaderAdaptor(blockchain.getBlockHeader(blockNumber).get());
  }
}
