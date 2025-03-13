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
package org.idnecology.idn.ethereum.eth.manager;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.core.encoding.EncodingContext;
import org.idnecology.idn.ethereum.core.encoding.TransactionEncoder;
import org.idnecology.idn.ethereum.eth.EthProtocolConfiguration;
import org.idnecology.idn.ethereum.eth.messages.BlockBodiesMessage;
import org.idnecology.idn.ethereum.eth.messages.BlockHeadersMessage;
import org.idnecology.idn.ethereum.eth.messages.EthPV62;
import org.idnecology.idn.ethereum.eth.messages.EthPV63;
import org.idnecology.idn.ethereum.eth.messages.EthPV65;
import org.idnecology.idn.ethereum.eth.messages.GetBlockBodiesMessage;
import org.idnecology.idn.ethereum.eth.messages.GetBlockHeadersMessage;
import org.idnecology.idn.ethereum.eth.messages.GetNodeDataMessage;
import org.idnecology.idn.ethereum.eth.messages.GetPooledTransactionsMessage;
import org.idnecology.idn.ethereum.eth.messages.GetReceiptsMessage;
import org.idnecology.idn.ethereum.eth.messages.NodeDataMessage;
import org.idnecology.idn.ethereum.eth.messages.PooledTransactionsMessage;
import org.idnecology.idn.ethereum.eth.messages.ReceiptsMessage;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.MessageData;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

class EthServer {
  private final Blockchain blockchain;
  private final WorldStateArchive worldStateArchive;
  private final TransactionPool transactionPool;
  private final EthMessages ethMessages;
  private final EthProtocolConfiguration ethereumWireProtocolConfiguration;

  EthServer(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final TransactionPool transactionPool,
      final EthMessages ethMessages,
      final EthProtocolConfiguration ethereumWireProtocolConfiguration) {
    this.blockchain = blockchain;
    this.worldStateArchive = worldStateArchive;
    this.transactionPool = transactionPool;
    this.ethMessages = ethMessages;
    this.ethereumWireProtocolConfiguration = ethereumWireProtocolConfiguration;
    this.registerResponseConstructors();
  }

  private void registerResponseConstructors() {
    final int maxMessageSize = ethereumWireProtocolConfiguration.getMaxMessageSize();

    ethMessages.registerResponseConstructor(
        EthPV62.GET_BLOCK_HEADERS,
        messageData ->
            constructGetHeadersResponse(
                blockchain,
                messageData,
                ethereumWireProtocolConfiguration.getMaxGetBlockHeaders(),
                maxMessageSize));
    ethMessages.registerResponseConstructor(
        EthPV62.GET_BLOCK_BODIES,
        messageData ->
            constructGetBodiesResponse(
                blockchain,
                messageData,
                ethereumWireProtocolConfiguration.getMaxGetBlockBodies(),
                maxMessageSize));
    ethMessages.registerResponseConstructor(
        EthPV63.GET_RECEIPTS,
        messageData ->
            constructGetReceiptsResponse(
                blockchain,
                messageData,
                ethereumWireProtocolConfiguration.getMaxGetReceipts(),
                maxMessageSize));
    ethMessages.registerResponseConstructor(
        EthPV63.GET_NODE_DATA,
        messageData ->
            constructGetNodeDataResponse(
                worldStateArchive,
                messageData,
                ethereumWireProtocolConfiguration.getMaxGetNodeData(),
                maxMessageSize));
    ethMessages.registerResponseConstructor(
        EthPV65.GET_POOLED_TRANSACTIONS,
        messageData ->
            constructGetPooledTransactionsResponse(
                transactionPool,
                messageData,
                ethereumWireProtocolConfiguration.getMaxGetPooledTransactions(),
                maxMessageSize));
  }

  static MessageData constructGetHeadersResponse(
      final Blockchain blockchain,
      final MessageData message,
      final int requestLimit,
      final int maxMessageSize) {
    // Extract parameters from request
    final GetBlockHeadersMessage getHeaders = GetBlockHeadersMessage.readFrom(message);
    final Optional<Hash> hash = getHeaders.hash();
    final int skip = getHeaders.skip();
    final int maxHeaders = Math.min(requestLimit, getHeaders.maxHeaders());
    final boolean reversed = getHeaders.reverse();
    final BlockHeader firstHeader;
    // Query first header by hash or number depending on request arguments
    if (hash.isPresent()) {
      final Hash startHash = hash.get();
      firstHeader = blockchain.getBlockHeader(startHash).orElse(null);
    } else {
      final long firstNumber = getHeaders.blockNumber().getAsLong();
      firstHeader = blockchain.getBlockHeader(firstNumber).orElse(null);
    }

    // The initial header was not found, nothing to return
    if (firstHeader == null) {
      return BlockHeadersMessage.create(Collections.emptyList());
    }

    // Encode the first header
    int responseSizeEstimate = RLP.MAX_PREFIX_SIZE;
    final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
    rlp.startList();
    final Bytes firstEncodedHeader = RLP.encode(firstHeader::writeTo);
    if (responseSizeEstimate + firstEncodedHeader.size() > maxMessageSize) {
      return BlockHeadersMessage.create(Collections.emptyList());
    }
    responseSizeEstimate += firstEncodedHeader.size();
    rlp.writeRaw(firstEncodedHeader);
    // Collect and encode the remaining headers
    final long numberDelta = reversed ? -(skip + 1) : (skip + 1);
    for (int i = 1; i < maxHeaders; i++) {
      final long blockNumber = firstHeader.getNumber() + i * numberDelta;
      if (blockNumber < BlockHeader.GENESIS_BLOCK_NUMBER) {
        break;
      }
      final Optional<BlockHeader> maybeHeader = blockchain.getBlockHeader(blockNumber);
      if (maybeHeader.isEmpty()) {
        break;
      }
      final BytesValueRLPOutput headerRlp = new BytesValueRLPOutput();
      maybeHeader.get().writeTo(headerRlp);
      final int encodedSize = headerRlp.encodedSize();
      if (responseSizeEstimate + encodedSize > maxMessageSize) {
        break;
      }
      responseSizeEstimate += encodedSize;
      rlp.writeRaw(headerRlp.encoded());
    }
    rlp.endList();

    return BlockHeadersMessage.createUnsafe(rlp.encoded());
  }

  static MessageData constructGetBodiesResponse(
      final Blockchain blockchain,
      final MessageData message,
      final int requestLimit,
      final int maxMessageSize) {
    final GetBlockBodiesMessage getBlockBodiesMessage = GetBlockBodiesMessage.readFrom(message);
    final Iterable<Hash> hashes = getBlockBodiesMessage.hashes();

    int responseSizeEstimate = RLP.MAX_PREFIX_SIZE;
    final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
    rlp.startList();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;
      final Optional<BlockBody> maybeBody = blockchain.getBlockBody(hash);
      if (maybeBody.isEmpty()) {
        continue;
      }

      final BlockBody body = maybeBody.get();
      final BytesValueRLPOutput bodyOutput = new BytesValueRLPOutput();
      body.writeWrappedBodyTo(bodyOutput);
      final int encodedSize = bodyOutput.encodedSize();
      if (responseSizeEstimate + encodedSize > maxMessageSize) {
        break;
      }
      responseSizeEstimate += encodedSize;
      rlp.writeRaw(bodyOutput.encoded());
    }
    rlp.endList();
    return BlockBodiesMessage.createUnsafe(rlp.encoded());
  }

  static MessageData constructGetReceiptsResponse(
      final Blockchain blockchain,
      final MessageData message,
      final int requestLimit,
      final int maxMessageSize) {
    final GetReceiptsMessage getReceipts = GetReceiptsMessage.readFrom(message);
    final Iterable<Hash> hashes = getReceipts.hashes();

    int responseSizeEstimate = RLP.MAX_PREFIX_SIZE;
    final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
    rlp.startList();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;
      final Optional<List<TransactionReceipt>> maybeReceipts = blockchain.getTxReceipts(hash);
      if (maybeReceipts.isEmpty()) {
        continue;
      }
      final BytesValueRLPOutput encodedReceipts = new BytesValueRLPOutput();
      encodedReceipts.startList();
      maybeReceipts.get().forEach(r -> r.writeToForNetwork(encodedReceipts));
      encodedReceipts.endList();
      final int encodedSize = encodedReceipts.encodedSize();
      if (responseSizeEstimate + encodedSize > maxMessageSize) {
        break;
      }

      responseSizeEstimate += encodedSize;
      rlp.writeRaw(encodedReceipts.encoded());
    }
    rlp.endList();

    return ReceiptsMessage.createUnsafe(rlp.encoded());
  }

  static MessageData constructGetPooledTransactionsResponse(
      final TransactionPool transactionPool,
      final MessageData message,
      final int requestLimit,
      final int maxMessageSize) {
    final GetPooledTransactionsMessage getPooledTransactions =
        GetPooledTransactionsMessage.readFrom(message);
    final Iterable<Hash> hashes = getPooledTransactions.pooledTransactions();

    int responseSizeEstimate = RLP.MAX_PREFIX_SIZE;
    final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
    rlp.startList();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;
      final Optional<Transaction> maybeTx = transactionPool.getTransactionByHash(hash);
      if (maybeTx.isEmpty()) {
        continue;
      }

      final BytesValueRLPOutput txRlp = new BytesValueRLPOutput();
      TransactionEncoder.encodeRLP(maybeTx.get(), txRlp, EncodingContext.POOLED_TRANSACTION);
      final int encodedSize = txRlp.encodedSize();
      if (responseSizeEstimate + encodedSize > maxMessageSize) {
        break;
      }

      responseSizeEstimate += encodedSize;
      rlp.writeRaw(txRlp.encoded());
    }
    rlp.endList();

    return PooledTransactionsMessage.createUnsafe(rlp.encoded());
  }

  static MessageData constructGetNodeDataResponse(
      final WorldStateArchive worldStateArchive,
      final MessageData message,
      final int requestLimit,
      final int maxMessageSize) {
    final GetNodeDataMessage getNodeDataMessage = GetNodeDataMessage.readFrom(message);
    final Iterable<Hash> hashes = getNodeDataMessage.hashes();

    int responseSizeEstimate = RLP.MAX_PREFIX_SIZE;
    final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
    rlp.startList();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;

      final Optional<Bytes> maybeNodeData = worldStateArchive.getNodeData(hash);
      if (maybeNodeData.isEmpty()) {
        continue;
      }

      final BytesValueRLPOutput rlpNodeData = new BytesValueRLPOutput();
      rlpNodeData.writeBytes(maybeNodeData.get());
      final int encodedSize = rlpNodeData.encodedSize();
      if (responseSizeEstimate + encodedSize > maxMessageSize) {
        break;
      }

      responseSizeEstimate += encodedSize;
      rlp.writeRaw(rlpNodeData.encoded());
    }
    rlp.endList();

    return NodeDataMessage.createUnsafe(rlp.encoded());
  }
}
