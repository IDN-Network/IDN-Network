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
package org.idnecology.idn.consensus.common;

/** The Epoch manager. */
public class EpochManager {

  private final long epochLengthInBlocks;
  private final long startBlock;

  /**
   * Instantiates a new Epoch manager.
   *
   * @param epochLengthInBlocks the epoch length in blocks
   */
  public EpochManager(final long epochLengthInBlocks) {
    this(epochLengthInBlocks, 0);
  }

  /**
   * Instantiates a new Epoch manager.
   *
   * @param epochLengthInBlocks the epoch length in blocks
   * @param startBlock the block number where the epoch counting starts
   */
  public EpochManager(final long epochLengthInBlocks, final long startBlock) {
    this.epochLengthInBlocks = epochLengthInBlocks;
    this.startBlock = startBlock;
  }

  /**
   * Is epoch block.
   *
   * @param blockNumber the block number
   * @return the boolean
   */
  public boolean isEpochBlock(final long blockNumber) {
    if (blockNumber < 0) {
      throw new IllegalArgumentException("Block number must be 0 or greater.");
    }
    if (blockNumber < startBlock - 1) {
      return false;
    }
    return (blockNumber - (startBlock == 0 ? 0 : startBlock - 1)) % epochLengthInBlocks == 0;
  }

  /**
   * Gets last epoch block.
   *
   * @param blockNumber the block number
   * @return the last epoch block
   */
  public long getLastEpochBlock(final long blockNumber) {
    if (blockNumber < startBlock) {
      throw new IllegalArgumentException("Block number is before start block.");
    }
    return startBlock + ((blockNumber - startBlock) / epochLengthInBlocks) * epochLengthInBlocks;
  }
}
