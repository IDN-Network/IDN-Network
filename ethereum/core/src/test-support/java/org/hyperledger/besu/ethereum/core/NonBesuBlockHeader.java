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
package org.idnecology.idn.ethereum.core;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Quantity;
import org.idnecology.idn.plugin.data.BlockHeader;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class NonIdnBlockHeader implements BlockHeader {

  final Hash blockHash;
  final Bytes extraData;

  public NonIdnBlockHeader(final Hash blockHash, final Bytes extraData) {
    this.blockHash = blockHash;
    this.extraData = extraData;
  }

  @Override
  public Hash getParentHash() {
    return null;
  }

  @Override
  public Hash getOmmersHash() {
    return null;
  }

  @Override
  public Address getCoinbase() {
    return null;
  }

  @Override
  public Hash getStateRoot() {
    return null;
  }

  @Override
  public Hash getTransactionsRoot() {
    return null;
  }

  @Override
  public Hash getReceiptsRoot() {
    return null;
  }

  @Override
  public Bytes getLogsBloom() {
    return null;
  }

  @Override
  public Quantity getDifficulty() {
    return null;
  }

  @Override
  public long getNumber() {
    return 0;
  }

  @Override
  public long getGasLimit() {
    return 0;
  }

  @Override
  public long getGasUsed() {
    return 0;
  }

  @Override
  public long getTimestamp() {
    return 0;
  }

  @Override
  public Bytes getExtraData() {
    return extraData;
  }

  @Override
  public Hash getMixHash() {
    return null;
  }

  @Override
  public long getNonce() {
    return 0;
  }

  @Override
  public Optional<? extends Hash> getWithdrawalsRoot() {
    return Optional.empty();
  }

  @Override
  public Optional<? extends Hash> getRequestsHash() {
    return Optional.empty();
  }

  @Override
  public Hash getBlockHash() {
    return blockHash;
  }

  @Override
  public Optional<? extends Quantity> getExcessBlobGas() {
    return Optional.empty();
  }

  @Override
  public Optional<? extends Long> getBlobGasUsed() {
    return Optional.empty();
  }

  @Override
  public Optional<? extends Bytes32> getParentBeaconBlockRoot() {
    return Optional.empty();
  }
}
