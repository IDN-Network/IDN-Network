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
package org.idnecology.idn.ethereum.eth.sync;

import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.sync.tasks.CompleteBlocksTask;
import org.idnecology.idn.ethereum.eth.sync.tasks.CompleteBlocksWithPeerTask;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DownloadBodiesStep
    implements Function<List<BlockHeader>, CompletableFuture<List<Block>>> {

  private final ProtocolSchedule protocolSchedule;
  private final EthContext ethContext;
  private final MetricsSystem metricsSystem;
  private final SynchronizerConfiguration synchronizerConfiguration;

  public DownloadBodiesStep(
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final SynchronizerConfiguration synchronizerConfiguration,
      final MetricsSystem metricsSystem) {
    this.protocolSchedule = protocolSchedule;
    this.ethContext = ethContext;
    this.synchronizerConfiguration = synchronizerConfiguration;
    this.metricsSystem = metricsSystem;
  }

  @Override
  public CompletableFuture<List<Block>> apply(final List<BlockHeader> blockHeaders) {
    if (synchronizerConfiguration.isPeerTaskSystemEnabled()) {
      return ethContext
          .getScheduler()
          .scheduleServiceTask(() -> getBodiesWithPeerTaskSystem(blockHeaders));
    } else {
      return CompleteBlocksTask.forHeaders(
              protocolSchedule, ethContext, blockHeaders, metricsSystem)
          .run();
    }
  }

  private CompletableFuture<List<Block>> getBodiesWithPeerTaskSystem(
      final List<BlockHeader> headers) {

    final CompleteBlocksWithPeerTask completeBlocksWithPeerTask =
        new CompleteBlocksWithPeerTask(protocolSchedule, headers, ethContext.getPeerTaskExecutor());
    final List<Block> blocks = completeBlocksWithPeerTask.retrieveBlocksFromPeers();
    return CompletableFuture.completedFuture(blocks);
  }
}
