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

import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.Block;

import java.util.Objects;

/** Adaptor class to allow a {@link Block} to be used as a {@link QbftBlock}. */
public class QbftBlockAdaptor implements QbftBlock {

  private final Block idnBlock;
  private final QbftBlockHeader qbftBlockHeader;

  /**
   * Constructs a QbftBlock from a Idn Block.
   *
   * @param idnBlock the Idn Block
   */
  public QbftBlockAdaptor(final Block idnBlock) {
    this.idnBlock = idnBlock;
    this.qbftBlockHeader = new QbftBlockHeaderAdaptor(idnBlock.getHeader());
  }

  @Override
  public QbftBlockHeader getHeader() {
    return qbftBlockHeader;
  }

  @Override
  public boolean isEmpty() {
    return idnBlock.getHeader().getTransactionsRoot().equals(Hash.EMPTY_TRIE_HASH);
  }

  /**
   * Returns the Idn Block associated with this QbftBlock. Used to convert a QbftBlock back to a
   * Idn block.
   *
   * @return the Idn Block
   */
  public Block getIdnBlock() {
    return idnBlock;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof QbftBlockAdaptor qbftBlock)) return false;
    return Objects.equals(idnBlock, qbftBlock.idnBlock)
        && Objects.equals(qbftBlockHeader, qbftBlock.qbftBlockHeader);
  }

  @Override
  public int hashCode() {
    return Objects.hash(idnBlock, qbftBlockHeader);
  }
}
