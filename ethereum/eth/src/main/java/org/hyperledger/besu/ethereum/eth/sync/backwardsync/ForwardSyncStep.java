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
package org.idnecology.idn.ethereum.eth.sync.backwardsync;

import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResponseCode;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResult;
import org.idnecology.idn.ethereum.eth.manager.peertask.task.GetBodiesFromPeerTask;
import org.idnecology.idn.ethereum.eth.manager.task.AbstractPeerTask;
import org.idnecology.idn.ethereum.eth.manager.task.RetryingGetBlocksFromPeersTask;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardSyncStep {

  private static final Logger LOG = LoggerFactory.getLogger(ForwardSyncStep.class);
  private final BackwardSyncContext context;
  private final BackwardChain backwardChain;

  public ForwardSyncStep(final BackwardSyncContext context, final BackwardChain backwardChain) {
    this.context = context;
    this.backwardChain = backwardChain;
  }

  public CompletableFuture<Void> executeAsync() {
    return CompletableFuture.supplyAsync(
            () -> backwardChain.getFirstNAncestorHeaders(context.getBatchSize()))
        .thenCompose(this::possibleRequestBodies);
  }

  @VisibleForTesting
  public CompletableFuture<Void> possibleRequestBodies(final List<BlockHeader> blockHeaders) {
    if (blockHeaders.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    } else {
      LOG.atDebug()
          .setMessage("Requesting {} blocks {}->{} ({})")
          .addArgument(blockHeaders::size)
          .addArgument(() -> blockHeaders.getFirst().getNumber())
          .addArgument(() -> blockHeaders.getLast().getNumber())
          .addArgument(() -> blockHeaders.getFirst().getHash().toHexString())
          .log();
      return requestBodies(blockHeaders)
          .thenApply(this::saveBlocks)
          .exceptionally(
              throwable -> {
                context.halveBatchSize();
                LOG.atDebug()
                    .setMessage(
                        "Getting {} blocks from peers failed with reason {}, reducing batch size to {}")
                    .addArgument(blockHeaders::size)
                    .addArgument(throwable::getMessage)
                    .addArgument(context::getBatchSize)
                    .log();
                return null;
              });
    }
  }

  @VisibleForTesting
  protected CompletableFuture<List<Block>> requestBodies(final List<BlockHeader> blockHeaders) {
    CompletableFuture<List<Block>> blocksFuture;
    if (context.getSynchronizerConfiguration().isPeerTaskSystemEnabled()) {
      blocksFuture =
          context
              .getEthContext()
              .getScheduler()
              .scheduleServiceTask(
                  () -> {
                    GetBodiesFromPeerTask task =
                        new GetBodiesFromPeerTask(
                            blockHeaders,
                            context.getProtocolSchedule(),
                            context.getEthContext().getEthPeers().peerCount());
                    PeerTaskExecutorResult<List<Block>> taskResult =
                        context.getEthContext().getPeerTaskExecutor().execute(task);
                    if (taskResult.responseCode() == PeerTaskExecutorResponseCode.SUCCESS
                        && taskResult.result().isPresent()) {
                      return CompletableFuture.completedFuture(taskResult.result().get());
                    } else {
                      return CompletableFuture.failedFuture(
                          new RuntimeException(taskResult.responseCode().toString()));
                    }
                  });
    } else {
      final RetryingGetBlocksFromPeersTask getBodiesFromPeerTask =
          RetryingGetBlocksFromPeersTask.forHeaders(
              context.getProtocolSchedule(),
              context.getEthContext(),
              context.getMetricsSystem(),
              context.getEthContext().getEthPeers().peerCount(),
              blockHeaders);

      blocksFuture =
          getBodiesFromPeerTask.run().thenApply(AbstractPeerTask.PeerTaskResult::getResult);
    }
    return blocksFuture.thenApply(
        blocks -> {
          LOG.debug("Got {} blocks from peers", blocks.size());
          blocks.sort(Comparator.comparing(block -> block.getHeader().getNumber()));
          return blocks;
        });
  }

  @VisibleForTesting
  protected Void saveBlocks(final List<Block> blocks) {
    if (blocks.isEmpty()) {
      context.halveBatchSize();
      LOG.debug("No blocks to save, reducing batch size to {}", context.getBatchSize());
      return null;
    }

    for (Block block : blocks) {
      final Optional<BlockHeader> parent =
          context
              .getProtocolContext()
              .getBlockchain()
              .getBlockHeader(block.getHeader().getParentHash());

      if (parent.isEmpty()) {
        context.halveBatchSize();
        LOG.atDebug()
            .setMessage(
                "Parent block {} not found, while saving block {}, reducing batch size to {}")
            .addArgument(block.getHeader().getParentHash())
            .addArgument(block::toLogString)
            .addArgument(context::getBatchSize)
            .log();
        return null;
      } else {
        context.saveBlock(block);
      }
    }

    if (blocks.size() == context.getBatchSize()) {
      // reset the batch size only if we got a full batch
      context.resetBatchSize();
    }
    return null;
  }
}
