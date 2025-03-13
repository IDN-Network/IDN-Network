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
package org.idnecology.idn.consensus.qbft.adaptor;

import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCreator;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.ethereum.blockcreation.BlockCreator;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;

import java.util.Collection;

/** Adaptor class to allow a {@link BlockCreator} to be used as a {@link QbftBlockCreator}. */
public class QbftBlockCreatorAdaptor implements QbftBlockCreator {

  private final BlockCreator idnBlockCreator;
  private final BftExtraDataCodec bftExtraDataCodec;

  /**
   * Constructs a new QbftBlockCreator
   *
   * @param idnBftBlockCreator the Idn BFT block creator
   * @param bftExtraDataCodec the bftExtraDataCodec used to encode extra data for the new header
   */
  public QbftBlockCreatorAdaptor(
      final BlockCreator idnBftBlockCreator, final BftExtraDataCodec bftExtraDataCodec) {
    this.idnBlockCreator = idnBftBlockCreator;
    this.bftExtraDataCodec = bftExtraDataCodec;
  }

  @Override
  public QbftBlock createBlock(
      final long headerTimeStampSeconds, final QbftBlockHeader parentHeader) {
    var blockResult =
        idnBlockCreator.createBlock(
            headerTimeStampSeconds, BlockUtil.toIdnBlockHeader(parentHeader));
    return new QbftBlockAdaptor(blockResult.getBlock());
  }

  @Override
  public QbftBlock createSealedBlock(
      final QbftBlock block, final int roundNumber, final Collection<SECPSignature> commitSeals) {
    final Block idnBlock = BlockUtil.toIdnBlock(block);
    final QbftBlockHeader initialHeader = block.getHeader();
    final BftExtraData initialExtraData =
        bftExtraDataCodec.decode(BlockUtil.toIdnBlockHeader(initialHeader));

    final BftExtraData sealedExtraData =
        new BftExtraData(
            initialExtraData.getVanityData(),
            commitSeals,
            initialExtraData.getVote(),
            roundNumber,
            initialExtraData.getValidators());

    final BlockHeader sealedHeader =
        BlockHeaderBuilder.fromHeader(BlockUtil.toIdnBlockHeader(initialHeader))
            .extraData(bftExtraDataCodec.encode(sealedExtraData))
            .blockHeaderFunctions(BftBlockHeaderFunctions.forOnchainBlock(bftExtraDataCodec))
            .buildBlockHeader();
    final Block sealedIdnBlock = new Block(sealedHeader, idnBlock.getBody());
    return new QbftBlockAdaptor(sealedIdnBlock);
  }
}
