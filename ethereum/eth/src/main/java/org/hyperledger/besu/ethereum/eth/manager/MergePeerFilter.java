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
package org.idnecology.idn.ethereum.eth.manager;

import org.idnecology.idn.consensus.merge.ForkchoiceEvent;
import org.idnecology.idn.consensus.merge.MergeStateHandler;
import org.idnecology.idn.consensus.merge.UnverifiedForkchoiceListener;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.eth.messages.EthPV62;
import org.idnecology.idn.ethereum.eth.messages.StatusMessage;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.Message;
import org.idnecology.idn.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergePeerFilter implements MergeStateHandler, UnverifiedForkchoiceListener {

  private Optional<Difficulty> powTerminalDifficulty = Optional.of(Difficulty.MAX_VALUE);
  private final StampedLock powTerminalDifficultyLock = new StampedLock();
  private final AtomicBoolean finalized = new AtomicBoolean(false);
  private static final Logger LOG = LoggerFactory.getLogger(MergePeerFilter.class);

  public boolean disconnectIfPoW(final StatusMessage status, final EthPeer peer) {
    long lockStamp = this.powTerminalDifficultyLock.readLock();
    try {
      if (this.powTerminalDifficulty.isPresent()
          && status.totalDifficulty().greaterThan(this.powTerminalDifficulty.get())) {
        LOG.debug(
            "Disconnecting peer with difficulty {}, likely still on PoW chain",
            status.totalDifficulty());
        peer.disconnect(DisconnectReason.SUBPROTOCOL_TRIGGERED_POW_DIFFICULTY);
        return true;
      } else {
        return false;
      }
    } finally {
      this.powTerminalDifficultyLock.unlockRead(lockStamp);
    }
  }

  public boolean disconnectIfGossipingBlocks(final Message message, final EthPeer peer) {
    final int code = message.getData().getCode();
    if (isFinalized() && (code == EthPV62.NEW_BLOCK || code == EthPV62.NEW_BLOCK_HASHES)) {
      LOG.debug("disconnecting peer for sending new blocks after transition to PoS");
      peer.disconnect(DisconnectReason.SUBPROTOCOL_TRIGGERED_POW_BLOCKS);
      return true;
    } else {
      return false;
    }
  }

  private boolean isFinalized() {
    return this.finalized.get();
  }

  @Override
  public void onNewUnverifiedForkchoice(final ForkchoiceEvent event) {
    if (event
        .hasValidFinalizedBlockHash()) { // forkchoices send finalized as 0 after ttd, but before an
      // epoch is
      // finalized
      this.finalized.set(true);
    }
  }

  @Override
  public void mergeStateChanged(
      final boolean isPoS,
      final Optional<Boolean> oldState,
      final Optional<Difficulty> difficultyStoppedAt) {
    if (isPoS && difficultyStoppedAt.isPresent()) {
      LOG.debug("terminal difficulty set to {}", difficultyStoppedAt.get().getValue());
      long lockStamp = this.powTerminalDifficultyLock.writeLock();
      try {
        this.powTerminalDifficulty = difficultyStoppedAt;
      } finally {
        this.powTerminalDifficultyLock.unlockWrite(lockStamp);
      }
    }
  }
}
