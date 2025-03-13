/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.parameters.UnsignedLongParameter;
import org.idnecology.idn.ethereum.core.Difficulty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EngineExchangeTransitionConfigurationParameter {
  private final Difficulty terminalTotalDifficulty;
  private final Hash terminalBlockHash;
  private final long terminalBlockNumber;

  @JsonCreator
  public EngineExchangeTransitionConfigurationParameter(
      @JsonProperty("terminalTotalDifficulty") final String terminalTotalDifficulty,
      @JsonProperty("terminalBlockHash") final String terminalBlockHash,
      @JsonProperty("terminalBlockNumber") final UnsignedLongParameter terminalBlockNumber) {
    this.terminalTotalDifficulty = Difficulty.fromHexString(terminalTotalDifficulty);
    this.terminalBlockHash = Hash.fromHexString(terminalBlockHash);
    this.terminalBlockNumber = terminalBlockNumber.getValue();
  }

  public Difficulty getTerminalTotalDifficulty() {
    return terminalTotalDifficulty;
  }

  @JsonProperty("terminalTotalDifficulty")
  public String getTerminalTotalDifficultyAsHexString() {
    return terminalTotalDifficulty.toShortHexString();
  }

  public Hash getTerminalBlockHash() {
    return terminalBlockHash;
  }

  @JsonProperty("terminalBlockHash")
  public String getTerminalBlockHashAsHexString() {
    return terminalBlockHash.toShortHexString();
  }

  public long getTerminalBlockNumber() {
    return terminalBlockNumber;
  }
}
