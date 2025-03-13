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

import static org.idnecology.idn.consensus.qbft.adaptor.BlockUtil.toIdnBlock;

import org.idnecology.idn.consensus.common.bft.BftBlockHeaderFunctions;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockInterface;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;

/**
 * Adaptor class to allow a {@link BftBlockInterface} to be used as a {@link QbftBlockInterface}.
 */
public class QbftBlockInterfaceAdaptor implements QbftBlockInterface {
  private final QbftExtraDataCodec bftExtraDataCodec = new QbftExtraDataCodec();
  private final BftBlockInterface bftBlockInterface;

  /**
   * Constructs a new QbftBlockInterface
   *
   * @param bftBlockInterface the BFT block interface
   */
  public QbftBlockInterfaceAdaptor(final BftBlockInterface bftBlockInterface) {
    this.bftBlockInterface = bftBlockInterface;
  }

  @Override
  public QbftBlock replaceRoundInBlock(final QbftBlock proposalBlock, final int roundNumber) {
    final Block idnBlock = toIdnBlock(proposalBlock);
    final BlockHeaderFunctions blockHeaderFunctions =
        BftBlockHeaderFunctions.forCommittedSeal(bftExtraDataCodec);
    final Block updatedRoundBlock =
        bftBlockInterface.replaceRoundInBlock(idnBlock, roundNumber, blockHeaderFunctions);
    return new QbftBlockAdaptor(updatedRoundBlock);
  }
}
