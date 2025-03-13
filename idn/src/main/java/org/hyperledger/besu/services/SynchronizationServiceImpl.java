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
package org.idnecology.idn.services;

import static org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams.withBlockHeaderAndUpdateNodeHead;

import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.common.provider.DiffBasedWorldStateProvider;
import org.idnecology.idn.ethereum.trie.diffbased.common.storage.DiffBasedWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.plugin.data.BlockBody;
import org.idnecology.idn.plugin.data.BlockHeader;
import org.idnecology.idn.plugin.services.sync.SynchronizationService;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Synchronization service. */
public class SynchronizationServiceImpl implements SynchronizationService {

  private static final Logger LOG = LoggerFactory.getLogger(SynchronizationServiceImpl.class);

  private final ProtocolContext protocolContext;
  private final ProtocolSchedule protocolSchedule;
  private final Synchronizer synchronizer;

  private final SyncState syncState;
  private final Optional<DiffBasedWorldStateProvider> worldStateArchive;

  /**
   * Constructor for SynchronizationServiceImpl.
   *
   * @param synchronizer synchronizer
   * @param protocolContext protocol context
   * @param protocolSchedule protocol schedule
   * @param syncState sync state
   * @param worldStateArchive world state archive
   */
  public SynchronizationServiceImpl(
      final Synchronizer synchronizer,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final SyncState syncState,
      final WorldStateArchive worldStateArchive) {
    this.synchronizer = synchronizer;
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.syncState = syncState;
    this.worldStateArchive =
        Optional.ofNullable(worldStateArchive)
            .filter(z -> z instanceof DiffBasedWorldStateProvider)
            .map(DiffBasedWorldStateProvider.class::cast);
  }

  @Override
  public void fireNewUnverifiedForkchoiceEvent(
      final Hash head, final Hash safeBlock, final Hash finalizedBlock) {
    protocolContext
        .safeConsensusContext(MergeContext.class)
        .ifPresent(mc -> mc.fireNewUnverifiedForkchoiceEvent(head, safeBlock, finalizedBlock));
    protocolContext.getBlockchain().setFinalized(finalizedBlock);
    protocolContext.getBlockchain().setSafeBlock(safeBlock);
  }

  @Override
  public boolean setHead(final BlockHeader blockHeader, final BlockBody blockBody) {
    final BlockImporter blockImporter =
        protocolSchedule
            .getByBlockHeader((org.idnecology.idn.ethereum.core.BlockHeader) blockHeader)
            .getBlockImporter();
    return blockImporter
        .importBlock(
            protocolContext,
            new Block(
                (org.idnecology.idn.ethereum.core.BlockHeader) blockHeader,
                (org.idnecology.idn.ethereum.core.BlockBody) blockBody),
            HeaderValidationMode.SKIP_DETACHED)
        .isImported();
  }

  @Override
  public boolean setHeadUnsafe(final BlockHeader blockHeader, final BlockBody blockBody) {
    final org.idnecology.idn.ethereum.core.BlockHeader coreHeader =
        (org.idnecology.idn.ethereum.core.BlockHeader) blockHeader;

    final MutableBlockchain blockchain = protocolContext.getBlockchain();

    if (worldStateArchive
        .flatMap(archive -> archive.getWorldState(withBlockHeaderAndUpdateNodeHead(coreHeader)))
        .isPresent()) {
      if (coreHeader.getParentHash().equals(blockchain.getChainHeadHash())) {
        LOG.atDebug()
            .setMessage(
                "Forwarding chain head to the block {} saved from a previous newPayload invocation")
            .addArgument(coreHeader::toLogString)
            .log();
        return blockchain.forwardToBlock(coreHeader);
      } else {
        LOG.atDebug()
            .setMessage("New head {} is a chain reorg, rewind chain head to it")
            .addArgument(coreHeader::toLogString)
            .log();
        return blockchain.rewindToBlock(coreHeader.getBlockHash());
      }
    } else {
      LOG.atWarn()
          .setMessage("The world state is unavailable, setting of head cannot be performed.")
          .log();
    }
    return false;
  }

  @Override
  public boolean isInitialSyncPhaseDone() {
    return syncState.isInitialSyncPhaseDone();
  }

  @Override
  public void disableWorldStateTrie() {
    // TODO maybe find a best way in the future to delete and disable trie
    worldStateArchive.ifPresent(
        archive -> {
          archive.getWorldStateSharedSpec().setTrieDisabled(true);
          final DiffBasedWorldStateKeyValueStorage worldStateStorage =
              archive.getWorldStateKeyValueStorage();
          final Optional<Hash> worldStateBlockHash = worldStateStorage.getWorldStateBlockHash();
          final Optional<Bytes> worldStateRootHash = worldStateStorage.getWorldStateRootHash();
          if (worldStateRootHash.isPresent() && worldStateBlockHash.isPresent()) {
            worldStateStorage.clearTrie();
            // keep root and block hash in the trie branch
            final DiffBasedWorldStateKeyValueStorage.Updater updater = worldStateStorage.updater();
            updater.saveWorldState(
                worldStateBlockHash.get(), Bytes32.wrap(worldStateRootHash.get()), Bytes.EMPTY);
            updater.commit();

            // currently only bonsai needs an explicit upgrade to full flat db
            if (worldStateStorage instanceof BonsaiWorldStateKeyValueStorage bonsaiStorage) {
              bonsaiStorage.upgradeToFullFlatDbMode();
            }
          }
        });
  }

  @Override
  public void stop() {
    synchronizer.stop();
  }

  @Override
  public void start() {
    synchronizer.start();
  }
}
