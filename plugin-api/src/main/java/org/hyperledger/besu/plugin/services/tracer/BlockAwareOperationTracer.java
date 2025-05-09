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
package org.idnecology.idn.plugin.services.tracer;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.plugin.data.BlockBody;
import org.idnecology.idn.plugin.data.BlockHeader;
import org.idnecology.idn.plugin.data.ProcessableBlockHeader;

/**
 * An extended operation tracer that can trace the start and end of a block.
 *
 * <p>In both methods, the block header and body are provided.
 */
public interface BlockAwareOperationTracer extends OperationTracer {

  /**
   * BlockAwareOperationTracer object with no tracing functionality. This serves as a default for
   * scenarios where no specific tracing operation is required.
   */
  BlockAwareOperationTracer NO_TRACING = new BlockAwareOperationTracer() {};

  /**
   * Trace the start of a block.
   *
   * @param blockHeader the header of the block which is traced
   * @param blockBody the body of the block which is traced
   * @param miningBeneficiary the address of miner building the block
   */
  default void traceStartBlock(
      final BlockHeader blockHeader, final BlockBody blockBody, final Address miningBeneficiary) {}

  /**
   * Trace the end of a block.
   *
   * @param blockHeader the header of the block which is traced
   * @param blockBody the body of the block which is traced
   */
  default void traceEndBlock(final BlockHeader blockHeader, final BlockBody blockBody) {}

  /**
   * When building a block this API is called at the start of the process
   *
   * @param processableBlockHeader the processable header
   * @param miningBeneficiary the address of miner building the block
   */
  default void traceStartBlock(
      final ProcessableBlockHeader processableBlockHeader, final Address miningBeneficiary) {}

  @Override
  default boolean isExtendedTracing() {
    return true;
  }
}
