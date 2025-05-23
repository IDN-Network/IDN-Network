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
package org.idnecology.idn.evm.blockhash;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.evm.frame.MessageFrame;

import java.util.function.BiFunction;

/**
 * Function that gets the block hash, passed in as part of TxValues.
 *
 * <p>Arg is the current executing message frame. The Result is the Hash, which may be zero based on
 * lookup rules.
 */
public interface BlockHashLookup extends BiFunction<MessageFrame, Long, Hash> {

  /**
   * How far back from the current block are hash queries valid for? Default is 256.
   *
   * @return The number of blocks before the current that should return a hash value.
   */
  default long getLookback() {
    return 256L;
  }
}
