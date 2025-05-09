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

import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPInput;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLPInput;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class QbftBlockCodecAdaptorTest {

  @Test
  void canRoundTripBlock() {
    BftExtraData bftExtraData =
        new BftExtraData(Bytes.wrap(new byte[32]), emptyList(), Optional.empty(), 0, emptyList());
    Bytes encodedExtraData = new QbftExtraDataCodec().encode(bftExtraData);
    BlockHeader header = new BlockHeaderTestFixture().extraData(encodedExtraData).buildHeader();
    Block idnBlock = new Block(header, BlockBody.empty());
    QbftBlock block = new QbftBlockAdaptor(idnBlock);

    BytesValueRLPOutput rlpOutput = new BytesValueRLPOutput();
    QbftBlockCodecAdaptor qbftBlockCodec = new QbftBlockCodecAdaptor(new QbftExtraDataCodec());
    qbftBlockCodec.writeTo(block, rlpOutput);

    RLPInput rlpInput = new BytesValueRLPInput(rlpOutput.encoded(), false);
    QbftBlock decodedBlock = qbftBlockCodec.readFrom(rlpInput);
    assertThat(decodedBlock).isEqualTo(block);
  }
}
