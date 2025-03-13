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
package org.idnecology.idn.ethereum.eth.sync.backwardsync;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.evm.log.Log;
import org.idnecology.idn.evm.log.LogsBloomFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;

public class ChainForTestCreator {

  public static BlockHeader prepareHeader(final long number, final Optional<String> message) {
    final Address testAddress =
        Address.fromHexString(message.orElse(String.format("%02X", number)));
    final Bytes testMessage = Bytes.fromHexString(String.format("%02X", number));
    final Log testLog = new Log(testAddress, testMessage, List.of());
    return new BlockHeader(
        Hash.EMPTY,
        Hash.EMPTY,
        Address.ZERO,
        Hash.EMPTY,
        Hash.EMPTY,
        Hash.EMPTY,
        LogsBloomFilter.builder().insertLog(testLog).build(),
        Difficulty.ZERO,
        number,
        0,
        0,
        0,
        Bytes.EMPTY,
        null,
        Hash.EMPTY,
        0,
        null,
        null,
        null,
        null,
        null,
        new MainnetBlockHeaderFunctions());
  }

  public static BlockHeader prepareWrongParentHash(final BlockHeader blockHeader) {
    BlockHeader fakeHeader =
        prepareHeader(blockHeader.getNumber(), Optional.of("111111111111111111111111"));
    return new BlockHeader(
        fakeHeader.getHash(),
        blockHeader.getOmmersHash(),
        blockHeader.getCoinbase(),
        blockHeader.getStateRoot(),
        blockHeader.getTransactionsRoot(),
        blockHeader.getReceiptsRoot(),
        blockHeader.getLogsBloom(),
        blockHeader.getDifficulty(),
        blockHeader.getNumber(),
        blockHeader.getGasLimit(),
        blockHeader.getGasUsed(),
        blockHeader.getTimestamp(),
        blockHeader.getExtraData(),
        blockHeader.getBaseFee().orElse(null),
        blockHeader.getMixHash(),
        blockHeader.getNonce(),
        blockHeader.getWithdrawalsRoot().orElse(null),
        blockHeader.getBlobGasUsed().orElse(null),
        blockHeader.getExcessBlobGas().orElse(null),
        blockHeader.getParentBeaconBlockRoot().orElse(null),
        blockHeader.getRequestsHash().orElse(null),
        new MainnetBlockHeaderFunctions());
  }

  public static List<Block> prepareChain(final int elements, final long height) {
    List<Block> blockList = new ArrayList<>(elements);

    blockList.add(createEmptyBlock(height));

    for (int i = 1; i < elements; i++) {
      blockList.add(createEmptyBlock(blockList.get(i - 1)));
    }
    return blockList;
  }

  public static Block createEmptyBlock(final Long height) {
    return new Block(prepareEmptyHeader(height), new BlockBody(List.of(), List.of()));
  }

  private static Block createEmptyBlock(final Block parent) {
    return new Block(prepareEmptyHeader(parent.getHeader()), new BlockBody(List.of(), List.of()));
  }

  private static BlockHeader prepareEmptyHeader(final Long number) {
    return prepareHeader(number, Optional.empty());
  }

  @Nonnull
  private static BlockHeader prepareEmptyHeader(final BlockHeader parent) {
    return new BlockHeader(
        parent.getHash(),
        Hash.EMPTY_TRIE_HASH,
        Address.ZERO,
        Hash.EMPTY_TRIE_HASH,
        Hash.EMPTY_TRIE_HASH,
        Hash.EMPTY_TRIE_HASH,
        LogsBloomFilter.builder().build(),
        Difficulty.ONE,
        parent.getNumber() + 1,
        0,
        0,
        0,
        Bytes.EMPTY,
        Wei.ZERO,
        Hash.EMPTY,
        0,
        null,
        null,
        null,
        null,
        null,
        new MainnetBlockHeaderFunctions());
  }
}
