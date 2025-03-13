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
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.sync.DownloadBodiesStep;
import org.idnecology.idn.ethereum.eth.sync.DownloadHeadersStep;
import org.idnecology.idn.ethereum.eth.sync.DownloadPipelineFactory;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.ValidationPolicy;
import org.idnecology.idn.ethereum.eth.sync.range.RangeHeadersFetcher;
import org.idnecology.idn.ethereum.eth.sync.range.RangeHeadersValidationStep;
import org.idnecology.idn.ethereum.eth.sync.range.SyncTargetRangeSource;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.sync.state.SyncTarget;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.services.pipeline.Pipeline;
import org.idnecology.idn.services.pipeline.PipelineBuilder;

import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullSyncDownloadPipelineFactory implements DownloadPipelineFactory {
  private static final Logger LOG = LoggerFactory.getLogger(FullSyncDownloadPipelineFactory.class);

  private final SynchronizerConfiguration syncConfig;
  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final EthContext ethContext;
  private final MetricsSystem metricsSystem;
  private final ValidationPolicy detachedValidationPolicy =
      () -> HeaderValidationMode.DETACHED_ONLY;
  private final BetterSyncTargetEvaluator betterSyncTargetEvaluator;
  private final SyncTerminationCondition fullSyncTerminationCondition;

  public FullSyncDownloadPipelineFactory(
      final SynchronizerConfiguration syncConfig,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final MetricsSystem metricsSystem,
      final SyncTerminationCondition syncTerminationCondition) {
    this.syncConfig = syncConfig;
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
    this.metricsSystem = metricsSystem;
    this.fullSyncTerminationCondition = syncTerminationCondition;
    this.betterSyncTargetEvaluator =
        new BetterSyncTargetEvaluator(syncConfig, ethContext.getEthPeers());
  }

  @Override
  public CompletionStage<Void> startPipeline(
      final EthScheduler scheduler,
      final SyncState syncState,
      final SyncTarget syncTarget,
      final Pipeline<?> pipeline) {
    return scheduler.startPipeline(pipeline);
  }

  @Override
  public Pipeline<?> createDownloadPipelineForSyncTarget(final SyncTarget target) {
    final int downloaderParallelism = syncConfig.getDownloaderParallelism();
    final int headerRequestSize = syncConfig.getDownloaderHeaderRequestSize();
    final int singleHeaderBufferSize = headerRequestSize * downloaderParallelism;
    final SyncTargetRangeSource checkpointRangeSource =
        new SyncTargetRangeSource(
            new RangeHeadersFetcher(syncConfig, protocolSchedule, ethContext, metricsSystem),
            this::shouldContinueDownloadingFromPeer,
            ethContext.getScheduler(),
            target.peer(),
            target.commonAncestor(),
            syncConfig.getDownloaderCheckpointRetries(),
            fullSyncTerminationCondition);
    final DownloadHeadersStep downloadHeadersStep =
        new DownloadHeadersStep(
            protocolSchedule,
            protocolContext,
            ethContext,
            detachedValidationPolicy,
            syncConfig,
            headerRequestSize,
            metricsSystem);
    final RangeHeadersValidationStep validateHeadersJoinUpStep =
        new RangeHeadersValidationStep(protocolSchedule, protocolContext, detachedValidationPolicy);
    final DownloadBodiesStep downloadBodiesStep =
        new DownloadBodiesStep(protocolSchedule, ethContext, syncConfig, metricsSystem);
    final ExtractTxSignaturesStep extractTxSignaturesStep = new ExtractTxSignaturesStep();
    final FullImportBlockStep importBlockStep =
        new FullImportBlockStep(
            protocolSchedule, protocolContext, ethContext, fullSyncTerminationCondition);

    return PipelineBuilder.createPipelineFrom(
            "fetchCheckpoints",
            checkpointRangeSource,
            downloaderParallelism,
            metricsSystem.createLabelledCounter(
                IdnMetricCategory.SYNCHRONIZER,
                "chain_download_pipeline_processed_total",
                "Number of entries process by each chain download pipeline stage",
                "step",
                "action"),
            true,
            "fullSync")
        .thenProcessAsyncOrdered("downloadHeaders", downloadHeadersStep, downloaderParallelism)
        .thenFlatMap("validateHeadersJoin", validateHeadersJoinUpStep, singleHeaderBufferSize)
        .inBatches(headerRequestSize)
        .thenProcessAsyncOrdered("downloadBodies", downloadBodiesStep, downloaderParallelism)
        .thenFlatMap("extractTxSignatures", extractTxSignaturesStep, singleHeaderBufferSize)
        .andFinishWith("importBlock", importBlockStep);
  }

  private boolean shouldContinueDownloadingFromPeer(
      final EthPeer peer, final BlockHeader lastCheckpointHeader) {
    final boolean shouldTerminate = fullSyncTerminationCondition.shouldStopDownload();
    final boolean caughtUpToPeer =
        peer.chainState().getEstimatedHeight() <= lastCheckpointHeader.getNumber();
    final boolean isDisconnected = peer.isDisconnected();
    final boolean shouldSwitchSyncTarget = betterSyncTargetEvaluator.shouldSwitchSyncTarget(peer);
    LOG.debug(
        "shouldTerminate {}, shouldContinueDownloadingFromPeer? {}, disconnected {}, caughtUp {}, shouldSwitchSyncTarget {}",
        shouldTerminate,
        peer,
        isDisconnected,
        caughtUpToPeer,
        shouldSwitchSyncTarget);

    return !shouldTerminate && !isDisconnected && !caughtUpToPeer && !shouldSwitchSyncTarget;
  }
}
