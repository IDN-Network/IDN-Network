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
package org.idnecology.idn.ethereum.mainnet;

import static org.idnecology.idn.crypto.Hash.keccak256;
import static org.idnecology.idn.crypto.Hash.sha256;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Request;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.core.encoding.EncodingContext;
import org.idnecology.idn.ethereum.core.encoding.TransactionEncoder;
import org.idnecology.idn.ethereum.core.encoding.WithdrawalEncoder;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.trie.MerkleTrie;
import org.idnecology.idn.ethereum.trie.patricia.SimpleMerklePatriciaTrie;
import org.idnecology.idn.evm.log.LogsBloomFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

/** A utility class for body validation tasks. */
public final class BodyValidation {

  private BodyValidation() {
    // Utility Class
  }

  private static Bytes indexKey(final int i) {
    return RLP.encodeOne(UInt256.valueOf(i).trimLeadingZeros());
  }

  private static MerkleTrie<Bytes, Bytes> trie() {
    return new SimpleMerklePatriciaTrie<>(b -> b);
  }

  /**
   * Generates the transaction root for a list of transactions
   *
   * @param transactions the transactions
   * @return the transaction root
   */
  public static Hash transactionsRoot(final List<Transaction> transactions) {
    final MerkleTrie<Bytes, Bytes> trie = trie();

    IntStream.range(0, transactions.size())
        .forEach(
            i ->
                trie.put(
                    indexKey(i),
                    TransactionEncoder.encodeOpaqueBytes(
                        transactions.get(i), EncodingContext.BLOCK_BODY)));

    return Hash.wrap(trie.getRootHash());
  }

  /**
   * Generates the withdrawals root for a list of withdrawals
   *
   * @param withdrawals the transactions
   * @return the transaction root
   */
  public static Hash withdrawalsRoot(final List<Withdrawal> withdrawals) {
    final MerkleTrie<Bytes, Bytes> trie = trie();

    IntStream.range(0, withdrawals.size())
        .forEach(
            i -> trie.put(indexKey(i), WithdrawalEncoder.encodeOpaqueBytes(withdrawals.get(i))));

    return Hash.wrap(trie.getRootHash());
  }

  /**
   * Generates the requests hash for a list of requests
   *
   * @param requests list of request (must be sorted by request type ascending)
   * @return the requests hash
   */
  public static Hash requestsHash(final List<Request> requests) {
    List<Bytes> requestHashes = new ArrayList<>();
    requests.forEach(
        request -> {
          // empty requests are excluded from the hash
          if (!request.getData().isEmpty()) {
            requestHashes.add(sha256(request.getEncodedRequest()));
          }
        });

    return Hash.wrap(sha256(Bytes.wrap(requestHashes)));
  }

  /**
   * Generates the receipt root for a list of receipts
   *
   * @param receipts the receipts
   * @return the receipt root
   */
  public static Hash receiptsRoot(final List<TransactionReceipt> receipts) {
    final MerkleTrie<Bytes, Bytes> trie = trie();

    IntStream.range(0, receipts.size())
        .forEach(
            i ->
                trie.put(
                    indexKey(i),
                    RLP.encode(
                        rlpOutput ->
                            receipts.get(i).writeToForReceiptTrie(rlpOutput, false, false))));

    return Hash.wrap(trie.getRootHash());
  }

  /**
   * Generates the ommers hash for a list of ommer block headers
   *
   * @param ommers the ommer block headers
   * @return the ommers hash
   */
  public static Hash ommersHash(final List<BlockHeader> ommers) {
    return Hash.wrap(keccak256(RLP.encode(out -> out.writeList(ommers, BlockHeader::writeTo))));
  }

  /**
   * Generates the logs bloom filter for a list of transaction receipts
   *
   * @param receipts the transaction receipts
   * @return the logs bloom filter
   */
  public static LogsBloomFilter logsBloom(final List<TransactionReceipt> receipts) {
    final LogsBloomFilter.Builder filterBuilder = LogsBloomFilter.builder();

    receipts.forEach(receipt -> filterBuilder.insertFilter(receipt.getBloomFilter()));

    return filterBuilder.build();
  }
}
