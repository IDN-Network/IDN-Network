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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.processor;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes32;

public class Tracer {

  public static <TRACE> Optional<TRACE> processTracing(
      final BlockchainQueries blockchainQueries,
      final Hash blockHash,
      final Function<TraceableState, ? extends Optional<TRACE>> mapper) {
    return processTracing(
        blockchainQueries, blockchainQueries.getBlockHeaderByHash(blockHash), mapper);
  }

  public static <TRACE> Optional<TRACE> processTracing(
      final BlockchainQueries blockchainQueries,
      final Optional<BlockHeader> blockHeader,
      final Function<TraceableState, ? extends Optional<TRACE>> mapper) {
    return blockHeader
        .map(BlockHeader::getParentHash)
        .flatMap(
            parentHash ->
                blockchainQueries.getAndMapWorldState(
                    parentHash,
                    mutableWorldState -> mapper.apply(new TraceableState(mutableWorldState))));
  }

  /**
   * This class force the use of the processTracing method to do tracing. processTracing allows you
   * to cleanly manage the worldstate, to close it etc
   */
  public static class TraceableState implements MutableWorldState {
    private final MutableWorldState mutableWorldState;

    private TraceableState(final MutableWorldState mutableWorldState) {
      this.mutableWorldState = mutableWorldState;
    }

    @Override
    public void persist(final BlockHeader blockHeader) {
      mutableWorldState.persist(blockHeader);
    }

    @Override
    public WorldUpdater updater() {
      return mutableWorldState.updater();
    }

    @Override
    public Hash rootHash() {
      return mutableWorldState.rootHash();
    }

    @Override
    public Hash frontierRootHash() {
      return mutableWorldState.rootHash();
    }

    @Override
    public Stream<StreamableAccount> streamAccounts(final Bytes32 startKeyHash, final int limit) {
      return mutableWorldState.streamAccounts(startKeyHash, limit);
    }

    @Override
    public Account get(final Address address) {
      return mutableWorldState.get(address);
    }

    @Override
    public void close() throws Exception {
      mutableWorldState.close();
    }
  }
}
