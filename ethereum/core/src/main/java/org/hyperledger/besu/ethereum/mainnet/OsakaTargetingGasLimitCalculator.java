/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.mainnet;

import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

public class OsakaTargetingGasLimitCalculator extends PragueTargetingGasLimitCalculator {

  /** The mainnet default maximum number of blobs per block for Osaka */
  private static final int DEFAULT_MAX_BLOBS_PER_BLOCK_OSAKA = 12;

  public OsakaTargetingGasLimitCalculator(
      final long londonForkBlock,
      final BaseFeeMarket feeMarket,
      final GasCalculator gasCalculator) {
    super(londonForkBlock, feeMarket, gasCalculator, DEFAULT_MAX_BLOBS_PER_BLOCK_OSAKA);
  }

  /**
   * Using Osaka mainnet default of 12 blobs for maxBlobsPerBlock:
   * CancunGasCalculator.BLOB_GAS_PER_BLOB * 12 blobs = 131072 * 12 = 1572864 = 0x180000
   */
  public OsakaTargetingGasLimitCalculator(
      final long londonForkBlock,
      final BaseFeeMarket feeMarket,
      final GasCalculator gasCalculator,
      final int maxBlobsPerBlock) {
    super(londonForkBlock, feeMarket, gasCalculator, maxBlobsPerBlock);
  }
}
