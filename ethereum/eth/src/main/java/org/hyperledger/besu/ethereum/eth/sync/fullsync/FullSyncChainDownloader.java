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
package org.idnecology.idn.ethereum.eth.sync.fullsync;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.sync.ChainDownloader;
import org.idnecology.idn.ethereum.eth.sync.PipelineChainDownloader;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.SyncDurationMetrics;
import org.idnecology.idn.plugin.services.MetricsSystem;

public class FullSyncChainDownloader {
  private FullSyncChainDownloader() {}

  public static ChainDownloader create(
      final SynchronizerConfiguration config,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final MetricsSystem metricsSystem,
      final SyncTerminationCondition terminationCondition,
      final SyncDurationMetrics syncDurationMetrics,
      final PeerTaskExecutor peerTaskExecutor) {

    final FullSyncTargetManager syncTargetManager =
        new FullSyncTargetManager(
            config,
            protocolSchedule,
            protocolContext,
            ethContext,
            metricsSystem,
            terminationCondition);

    return new PipelineChainDownloader(
        syncState,
        syncTargetManager,
        new FullSyncDownloadPipelineFactory(
            config,
            protocolSchedule,
            protocolContext,
            ethContext,
            metricsSystem,
            terminationCondition),
        ethContext.getScheduler(),
        metricsSystem,
        syncDurationMetrics);
  }
}
