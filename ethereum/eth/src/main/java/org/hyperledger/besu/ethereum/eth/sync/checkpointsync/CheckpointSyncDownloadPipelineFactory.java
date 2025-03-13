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

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncDownloadPipelineFactory;
import org.idnecology.idn.ethereum.eth.sync.fastsync.FastSyncState;
import org.idnecology.idn.ethereum.eth.sync.fastsync.checkpoint.Checkpoint;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.sync.state.SyncTarget;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.services.pipeline.Pipeline;
import org.idnecology.idn.services.pipeline.PipelineBuilder;

import java.util.concurrent.CompletionStage;

public class CheckpointSyncDownloadPipelineFactory extends FastSyncDownloadPipelineFactory {

  public CheckpointSyncDownloadPipelineFactory(
      final SynchronizerConfiguration syncConfig,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final FastSyncState fastSyncState,
      final MetricsSystem metricsSystem) {
    super(syncConfig, protocolSchedule, protocolContext, ethContext, fastSyncState, metricsSystem);
  }

  @Override
  public CompletionStage<Void> startPipeline(
      final EthScheduler scheduler,
      final SyncState syncState,
      final SyncTarget syncTarget,
      final Pipeline<?> pipeline) {
    return scheduler
        .startPipeline(createDownloadCheckPointPipeline(syncState, syncTarget))
        .thenCompose(unused -> scheduler.startPipeline(pipeline));
  }

  protected Pipeline<Hash> createDownloadCheckPointPipeline(
      final SyncState syncState, final SyncTarget target) {

    final Checkpoint checkpoint = syncState.getCheckpoint().orElseThrow();

    final BlockHeader checkpointBlockHeader = target.peer().getCheckpointHeader().orElseThrow();
    final CheckpointSource checkPointSource =
        new CheckpointSource(
            syncState,
            checkpointBlockHeader,
            protocolSchedule
                .getByBlockHeader(checkpointBlockHeader)
                .getBlockHeaderFunctions()
                .getCheckPointWindowSize(checkpointBlockHeader));

    final CheckpointBlockImportStep checkPointBlockImportStep =
        new CheckpointBlockImportStep(
            checkPointSource, checkpoint, protocolContext.getBlockchain());

    final CheckpointDownloadBlockStep checkPointDownloadBlockStep =
        new CheckpointDownloadBlockStep(
            protocolSchedule, ethContext, checkpoint, syncConfig, metricsSystem);

    return PipelineBuilder.createPipelineFrom(
            "fetchCheckpoints",
            checkPointSource,
            1,
            metricsSystem.createLabelledCounter(
                IdnMetricCategory.SYNCHRONIZER,
                "chain_download_pipeline_processed_total",
                "Number of header process by each chain download pipeline stage",
                "step",
                "action"),
            true,
            "checkpointSync")
        .thenProcessAsyncOrdered("downloadBlock", checkPointDownloadBlockStep::downloadBlock, 1)
        .andFinishWith("importBlock", checkPointBlockImportStep);
  }

  @Override
  protected BlockHeader getCommonAncestor(final SyncTarget target) {
    return target
        .peer()
        .getCheckpointHeader()
        .filter(checkpoint -> checkpoint.getNumber() > target.commonAncestor().getNumber())
        .orElse(target.commonAncestor());
  }
}
