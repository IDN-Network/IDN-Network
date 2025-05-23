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
package org.idnecology.idn.ethereum.referencetests;

import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.BlobGas;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.ConsensusContextFixture;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.ParsedExtraData;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPInput;
import org.idnecology.idn.ethereum.rlp.RLPInput;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.evm.log.LogsBloomFilter;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockchainReferenceTestCaseSpec {

  private final String network;

  private final CandidateBlock[] candidateBlocks;

  private final ReferenceTestBlockHeader genesisBlockHeader;

  private final Hash lastBlockHash;

  private final WorldStateArchive worldStateArchive;

  private final MutableBlockchain blockchain;
  private final String sealEngine;

  private final ProtocolContext protocolContext;

  private static WorldStateArchive buildWorldStateArchive(
      final Map<String, ReferenceTestWorldState.AccountMock> accounts) {
    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();

    final MutableWorldState worldState = worldStateArchive.getWorldState();
    final WorldUpdater updater = worldState.updater();

    for (final Map.Entry<String, ReferenceTestWorldState.AccountMock> entry : accounts.entrySet()) {
      ReferenceTestWorldState.insertAccount(
          updater, Address.fromHexString(entry.getKey()), entry.getValue());
    }

    updater.commit();
    worldState.persist(null);

    return worldStateArchive;
  }

  private static MutableBlockchain buildBlockchain(final BlockHeader genesisBlockHeader) {
    final Block genesisBlock = new Block(genesisBlockHeader, BlockBody.empty());
    return InMemoryKeyValueStorageProvider.createInMemoryBlockchain(genesisBlock);
  }

  @JsonCreator
  public BlockchainReferenceTestCaseSpec(
      @JsonProperty("network") final String network,
      @JsonProperty("blocks") final CandidateBlock[] candidateBlocks,
      @JsonProperty("genesisBlockHeader") final ReferenceTestBlockHeader genesisBlockHeader,
      @SuppressWarnings("unused") @JsonProperty("genesisRLP") final String genesisRLP,
      @JsonProperty("pre") final Map<String, ReferenceTestWorldState.AccountMock> accounts,
      @JsonProperty("lastblockhash") final String lastBlockHash,
      @JsonProperty("sealEngine") final String sealEngine) {
    this.network = network;
    this.candidateBlocks = candidateBlocks;
    this.genesisBlockHeader = genesisBlockHeader;
    this.lastBlockHash = Hash.fromHexString(lastBlockHash);
    this.worldStateArchive = buildWorldStateArchive(accounts);
    this.blockchain = buildBlockchain(genesisBlockHeader);
    this.sealEngine = sealEngine;
    this.protocolContext =
        new ProtocolContext(
            this.blockchain,
            this.worldStateArchive,
            new ConsensusContextFixture(),
            new BadBlockManager());
  }

  public String getNetwork() {
    return network;
  }

  public CandidateBlock[] getCandidateBlocks() {
    return candidateBlocks;
  }

  public WorldStateArchive getWorldStateArchive() {
    return worldStateArchive;
  }

  public BlockHeader getGenesisBlockHeader() {
    return genesisBlockHeader;
  }

  public MutableBlockchain getBlockchain() {
    return blockchain;
  }

  public ProtocolContext getProtocolContext() {
    return protocolContext;
  }

  public Hash getLastBlockHash() {
    return lastBlockHash;
  }

  public String getSealEngine() {
    return sealEngine;
  }

  public static class ReferenceTestBlockHeader extends BlockHeader {

    @JsonCreator
    public ReferenceTestBlockHeader(
        @JsonProperty("parentHash") final String parentHash,
        @JsonProperty("uncleHash") final String uncleHash,
        @JsonProperty("coinbase") final String coinbase,
        @JsonProperty("stateRoot") final String stateRoot,
        @JsonProperty("transactionsTrie") final String transactionsTrie,
        @JsonProperty("receiptTrie") final String receiptTrie,
        @JsonProperty("bloom") final String bloom,
        @JsonProperty("difficulty") final String difficulty,
        @JsonProperty("number") final String number,
        @JsonProperty("gasLimit") final String gasLimit,
        @JsonProperty("gasUsed") final String gasUsed,
        @JsonProperty("timestamp") final String timestamp,
        @JsonProperty("extraData") final String extraData,
        @JsonProperty("baseFeePerGas") final String baseFee,
        @JsonProperty("mixHash") final String mixHash,
        @JsonProperty("nonce") final String nonce,
        @JsonProperty("withdrawalsRoot") final String withdrawalsRoot,
        @JsonProperty("requestsHash") final String requestsHash,
        @JsonProperty("blobGasUsed") final String blobGasUsed,
        @JsonProperty("excessBlobGas") final String excessBlobGas,
        @JsonProperty("parentBeaconBlockRoot") final String parentBeaconBlockRoot,
        @JsonProperty("hash") final String hash) {
      super(
          Hash.fromHexString(parentHash), // parentHash
          uncleHash == null ? Hash.EMPTY_LIST_HASH : Hash.fromHexString(uncleHash), // ommersHash
          Address.fromHexString(coinbase), // coinbase
          Hash.fromHexString(stateRoot), // stateRoot
          transactionsTrie == null
              ? Hash.EMPTY_TRIE_HASH
              : Hash.fromHexString(transactionsTrie), // transactionsRoot
          receiptTrie == null
              ? Hash.EMPTY_TRIE_HASH
              : Hash.fromHexString(receiptTrie), // receiptTrie
          LogsBloomFilter.fromHexString(bloom), // bloom
          Difficulty.fromHexString(difficulty), // difficulty
          Long.decode(number), // number
          Long.decode(gasLimit), // gasLimit
          Long.decode(gasUsed), // gasUsed
          Long.decode(timestamp), // timestamp
          Bytes.fromHexString(extraData), // extraData
          baseFee != null ? Wei.fromHexString(baseFee) : null, // baseFee
          Hash.fromHexString(mixHash), // mixHash
          Bytes.fromHexStringLenient(nonce).toLong(),
          withdrawalsRoot != null ? Hash.fromHexString(withdrawalsRoot) : null,
          blobGasUsed != null ? Long.decode(blobGasUsed) : 0,
          excessBlobGas != null ? BlobGas.fromHexString(excessBlobGas) : null,
          parentBeaconBlockRoot != null ? Bytes32.fromHexString(parentBeaconBlockRoot) : null,
          requestsHash != null ? Hash.fromHexString(requestsHash) : null,
          new BlockHeaderFunctions() {
            @Override
            public Hash hash(final BlockHeader header) {
              return hash == null ? null : Hash.fromHexString(hash);
            }

            @Override
            public ParsedExtraData parseExtraData(final BlockHeader header) {
              return null;
            }
          });
    }
  }

  @JsonIgnoreProperties({
    "blocknumber",
    "chainname",
    "chainnetwork",
    "expectException",
    "expectExceptionByzantium",
    "expectExceptionConstantinople",
    "expectExceptionConstantinopleFix",
    "expectExceptionIstanbul",
    "expectExceptionEIP150",
    "expectExceptionEIP158",
    "expectExceptionFrontier",
    "expectExceptionHomestead",
    "expectExceptionALL",
    "hasBigInt",
    "rlp_decoded"
  })
  public static class CandidateBlock {

    private final Bytes rlp;

    private final Boolean valid;
    private final List<TransactionSequence> transactionSequence;

    @JsonCreator
    public CandidateBlock(
        @JsonProperty("rlp") final String rlp,
        @JsonProperty("blockHeader") final Object blockHeader,
        @JsonProperty("transactions") final Object transactions,
        @JsonProperty("uncleHeaders") final Object uncleHeaders,
        @JsonProperty("withdrawals") final Object withdrawals,
        @JsonProperty("depositRequests") final Object depositRequests,
        @JsonProperty("withdrawalRequests") final Object withdrawalRequests,
        @JsonProperty("consolidationRequests") final Object consolidationRequests,
        @JsonProperty("transactionSequence") final List<TransactionSequence> transactionSequence) {
      boolean blockValid = true;
      // The BLOCK__WrongCharAtRLP_0 test has an invalid character in its rlp string.
      Bytes rlpAttempt = null;
      try {
        rlpAttempt = Bytes.fromHexString(rlp);
      } catch (final IllegalArgumentException e) {
        blockValid = false;
      }
      this.rlp = rlpAttempt;

      if (blockHeader == null
          && transactions == null
          && uncleHeaders == null
          && withdrawals == null) {
        blockValid = false;
      }

      this.valid = blockValid;
      this.transactionSequence = transactionSequence;
    }

    public boolean isValid() {
      return valid;
    }

    public boolean areAllTransactionsValid() {
      return transactionSequence == null
          || transactionSequence.stream().filter(t -> !t.valid()).count() == 0;
    }

    public boolean isExecutable() {
      return rlp != null;
    }

    public Block getBlock() {
      final RLPInput input = new BytesValueRLPInput(rlp, false);
      input.enterList();
      final MainnetBlockHeaderFunctions blockHeaderFunctions = new MainnetBlockHeaderFunctions();
      final BlockHeader header = BlockHeader.readFrom(input, blockHeaderFunctions);
      final BlockBody body =
          new BlockBody(
              input.readList(Transaction::readFrom),
              input.readList(inputData -> BlockHeader.readFrom(inputData, blockHeaderFunctions)),
              input.isEndOfCurrentList()
                  ? Optional.empty()
                  : Optional.of(input.readList(Withdrawal::readFrom)));
      return new Block(header, body);
    }
  }
}
