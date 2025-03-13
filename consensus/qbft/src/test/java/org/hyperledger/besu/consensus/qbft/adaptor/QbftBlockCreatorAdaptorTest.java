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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.Vote;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.blockcreation.BlockCreator;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QbftBlockCreatorAdaptorTest {
  @Mock private BlockCreator blockCreator;
  @Mock private Block idnBlock;
  private final QbftExtraDataCodec qbftExtraDataCodec = new QbftExtraDataCodec();

  @Test
  void createsBlockUsingIdnBlockCreator() {
    BlockHeader idnParentHeader = new BlockHeaderTestFixture().buildHeader();
    QbftBlockHeader parentHeader = new QbftBlockHeaderAdaptor(idnParentHeader);

    when(blockCreator.createBlock(10, idnParentHeader))
        .thenReturn(new BlockCreator.BlockCreationResult(idnBlock, null, null));

    QbftBlockCreatorAdaptor qbftBlockCreator =
        new QbftBlockCreatorAdaptor(blockCreator, qbftExtraDataCodec);
    QbftBlock qbftBlock = qbftBlockCreator.createBlock(10, parentHeader);
    assertThat(((QbftBlockAdaptor) qbftBlock).getIdnBlock()).isEqualTo(idnBlock);
  }

  @Test
  void createsSealedBlockWithRoundAndSeals() {
    BftExtraData bftExtraData =
        new BftExtraData(
            Bytes.wrap(new byte[32]),
            emptyList(),
            Optional.of(Vote.authVote(Address.ZERO)),
            0,
            List.of(Address.ZERO));
    Bytes extraDataBytes = qbftExtraDataCodec.encode(bftExtraData);
    BlockHeader header = new BlockHeaderTestFixture().extraData(extraDataBytes).buildHeader();
    Block idnBlock = new Block(header, BlockBody.empty());
    QbftBlock block = new QbftBlockAdaptor(idnBlock);
    SECPSignature seal = new SECPSignature(BigInteger.ONE, BigInteger.ONE, (byte) 1);

    QbftBlockCreatorAdaptor qbftBlockCreator =
        new QbftBlockCreatorAdaptor(blockCreator, qbftExtraDataCodec);
    QbftBlock sealedBlock = qbftBlockCreator.createSealedBlock(block, 1, List.of(seal));
    BftExtraData sealedExtraData =
        qbftExtraDataCodec.decode(BlockUtil.toIdnBlockHeader(sealedBlock.getHeader()));
    assertThat(sealedExtraData.getVanityData()).isEqualTo(Bytes.wrap(new byte[32]));
    assertThat(sealedExtraData.getVote()).contains(Vote.authVote(Address.ZERO));
    assertThat(sealedExtraData.getValidators()).isEqualTo(List.of(Address.ZERO));
    assertThat(sealedExtraData.getRound()).isEqualTo(1);
    assertThat(sealedExtraData.getSeals()).isEqualTo(List.of(seal));
  }
}
