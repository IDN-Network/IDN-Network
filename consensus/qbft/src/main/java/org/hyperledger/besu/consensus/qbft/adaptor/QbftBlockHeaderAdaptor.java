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

import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.util.Objects;

/** Adaptor class to allow a {@link BlockHeader} to be used as a {@link QbftBlockHeader}. */
public class QbftBlockHeaderAdaptor implements QbftBlockHeader {

  private final BlockHeader blockHeader;

  /**
   * Construct a new QbftBlockHeader
   *
   * @param blockHeader the Idn block header
   */
  public QbftBlockHeaderAdaptor(final BlockHeader blockHeader) {
    this.blockHeader = blockHeader;
  }

  @Override
  public long getNumber() {
    return blockHeader.getNumber();
  }

  @Override
  public long getTimestamp() {
    return blockHeader.getTimestamp();
  }

  @Override
  public Address getCoinbase() {
    return blockHeader.getCoinbase();
  }

  @Override
  public Hash getHash() {
    return blockHeader.getHash();
  }

  /**
   * Returns the Idn block header.
   *
   * @return the Idn block header.
   */
  public BlockHeader getIdnBlockHeader() {
    return blockHeader;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof QbftBlockHeaderAdaptor that)) return false;
    return Objects.equals(blockHeader, that.blockHeader);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(blockHeader);
  }
}
