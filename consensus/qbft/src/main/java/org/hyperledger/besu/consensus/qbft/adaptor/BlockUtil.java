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
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;

/** Utility class to convert between Idn and QBFT blocks. */
public class BlockUtil {

  /** Private constructor to prevent instantiation. */
  private BlockUtil() {}

  /**
   * Convert a QBFT block to a Idn block.
   *
   * @param block the QBFT block
   * @return the Idn block
   */
  public static Block toIdnBlock(final QbftBlock block) {
    if (block instanceof QbftBlockAdaptor) {
      return ((QbftBlockAdaptor) block).getIdnBlock();
    } else {
      throw new IllegalArgumentException("Unsupported block type");
    }
  }

  /**
   * Convert a QBFT block header to a Idn block header.
   *
   * @param header the QBFT block header
   * @return the Idn block header
   */
  public static BlockHeader toIdnBlockHeader(final QbftBlockHeader header) {
    if (header instanceof QbftBlockHeaderAdaptor) {
      return ((QbftBlockHeaderAdaptor) header).getIdnBlockHeader();
    } else {
      throw new IllegalArgumentException("Unsupported block header type");
    }
  }
}
