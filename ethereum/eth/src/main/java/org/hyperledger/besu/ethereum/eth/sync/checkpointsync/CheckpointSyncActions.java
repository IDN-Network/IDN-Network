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
package org.idnecology.idn.ethereum.eth.sync.checkpointsync;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.sync.ChainDownloader;
import org.idnecology.idn.ethereum.eth.sync.PivotBlockSelector;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncActions;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncState;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.SyncDurationMetrics;
import org.idnecology.idn.plugin.services.MetricsSystem;

public class CheckpointSyncActions extends FastSyncActions {
  public CheckpointSyncActions(
      final SynchronizerConfiguration syncConfig,
      final WorldStateStorageCoordinator worldStateStorageCoordinator,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final PivotBlockSelector pivotBlockSelector,
      final MetricsSystem metricsSystem) {
    super(
        syncConfig,
        worldStateStorageCoordinator,
        protocolSchedule,
        protocolContext,
        ethContext,
        syncState,
        pivotBlockSelector,
        metricsSystem);
  }

  @Override
  public ChainDownloader createChainDownloader(
      final FastSyncState currentState, final SyncDurationMetrics syncDurationMetrics) {
    return CheckpointSyncChainDownloader.create(
        syncConfig,
        worldStateStorageCoordinator,
        protocolSchedule,
        protocolContext,
        ethContext,
        syncState,
        metricsSystem,
        currentState,
        syncDurationMetrics);
  }
}
