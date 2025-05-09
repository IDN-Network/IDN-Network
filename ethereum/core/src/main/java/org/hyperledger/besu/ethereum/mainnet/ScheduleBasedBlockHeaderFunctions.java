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
package org.idnecology.idn.ethereum.mainnet;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.ParsedExtraData;
import org.idnecology.idn.ethereum.core.SealableBlockHeader;

/**
 * Looks up the correct {@link BlockHeaderFunctions} to use based on a {@link ProtocolSchedule} to
 * ensure that the correct hash is created given the block number.
 */
public class ScheduleBasedBlockHeaderFunctions implements BlockHeaderFunctions {

  private final ProtocolSchedule protocolSchedule;

  private ScheduleBasedBlockHeaderFunctions(final ProtocolSchedule protocolSchedule) {
    this.protocolSchedule = protocolSchedule;
  }

  public static BlockHeaderFunctions create(final ProtocolSchedule protocolSchedule) {
    return new ScheduleBasedBlockHeaderFunctions(protocolSchedule);
  }

  @Override
  public Hash hash(final BlockHeader header) {
    return getBlockHeaderFunctions(header).hash(header);
  }

  @Override
  public ParsedExtraData parseExtraData(final BlockHeader header) {
    return getBlockHeaderFunctions(header).parseExtraData(header);
  }

  private BlockHeaderFunctions getBlockHeaderFunctions(final SealableBlockHeader header) {
    return protocolSchedule.getByBlockHeader(header).getBlockHeaderFunctions();
  }
}
