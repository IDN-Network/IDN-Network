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
package org.idnecology.idn.ethereum.eth.peervalidation;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockDataGenerator.BlockOptions;
import org.idnecology.idn.ethereum.core.ProtocolScheduleFixture;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.idnecology.idn.ethereum.eth.manager.RespondingEthPeer;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class RequiredBlocksPeerValidatorTest extends AbstractPeerBlockValidatorTest {

  @Override
  AbstractPeerBlockValidator createValidator(final long blockNumber, final long buffer) {
    return new RequiredBlocksPeerValidator(
        ProtocolScheduleFixture.MAINNET,
        null,
        SynchronizerConfiguration.builder().build(),
        new NoOpMetricsSystem(),
        blockNumber,
        Hash.ZERO,
        buffer);
  }

  @Test
  public void validatePeer_responsivePeerWithRequiredBlock() {
    final EthProtocolManager ethProtocolManager =
        EthProtocolManagerTestBuilder.builder()
            .setProtocolSchedule(ProtocolScheduleFixture.MAINNET)
            .build();
    final BlockDataGenerator gen = new BlockDataGenerator(1);
    final long requiredBlockNumber = 500;
    final Block requiredBlock =
        gen.block(BlockOptions.create().setBlockNumber(requiredBlockNumber));

    final PeerValidator validator =
        new RequiredBlocksPeerValidator(
            ProtocolScheduleFixture.MAINNET,
            null,
            SynchronizerConfiguration.builder().build(),
            new NoOpMetricsSystem(),
            requiredBlockNumber,
            requiredBlock.getHash(),
            0);

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, requiredBlockNumber);

    final CompletableFuture<Boolean> result =
        validator.validatePeer(ethProtocolManager.ethContext(), peer.getEthPeer());

    assertThat(result).isNotDone();

    // Send response for block
    final AtomicBoolean requiredBlockRequested = respondToBlockRequest(peer, requiredBlock);

    assertThat(requiredBlockRequested).isTrue();
    assertThat(result).isDone();
    assertThat(result).isCompletedWithValue(true);
  }

  @Test
  public void validatePeer_responsivePeerWithBadRequiredBlock() {
    final EthProtocolManager ethProtocolManager = EthProtocolManagerTestBuilder.builder().build();
    final BlockDataGenerator gen = new BlockDataGenerator(1);
    final long requiredBlockNumber = 500;
    final Block requiredBlock =
        gen.block(BlockOptions.create().setBlockNumber(requiredBlockNumber));

    final PeerValidator validator =
        new RequiredBlocksPeerValidator(
            ProtocolScheduleFixture.MAINNET,
            null,
            SynchronizerConfiguration.builder().build(),
            new NoOpMetricsSystem(),
            requiredBlockNumber,
            Hash.ZERO,
            0);

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, requiredBlockNumber);

    final CompletableFuture<Boolean> result =
        validator.validatePeer(ethProtocolManager.ethContext(), peer.getEthPeer());

    assertThat(result).isNotDone();

    // Send response for required block
    final AtomicBoolean requiredBlockRequested = respondToBlockRequest(peer, requiredBlock);

    assertThat(requiredBlockRequested).isTrue();
    assertThat(result).isDone();
    assertThat(result).isCompletedWithValue(false);
  }

  @Test
  public void validatePeer_responsivePeerDoesNotHaveBlockWhenPastForkHeight() {
    final EthProtocolManager ethProtocolManager = EthProtocolManagerTestBuilder.builder().build();

    final PeerValidator validator =
        new RequiredBlocksPeerValidator(
            ProtocolScheduleFixture.MAINNET,
            null,
            SynchronizerConfiguration.builder().build(),
            new NoOpMetricsSystem(),
            1,
            Hash.ZERO);

    final RespondingEthPeer peer = EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 1);

    final CompletableFuture<Boolean> result =
        validator.validatePeer(ethProtocolManager.ethContext(), peer.getEthPeer());

    assertThat(result).isNotDone();

    // Respond to block header request with empty
    peer.respond(RespondingEthPeer.emptyResponder());

    assertThat(result).isDone();
    assertThat(result).isCompletedWithValue(false);
  }
}
