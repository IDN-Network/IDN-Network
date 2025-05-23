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
package org.idnecology.idn.ethereum.eth.sync.tasks;

import static com.google.common.base.Preconditions.checkArgument;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.task.AbstractEthTask;
import org.idnecology.idn.ethereum.eth.sync.tasks.exceptions.InvalidBlockException;
import org.idnecology.idn.ethereum.mainnet.BlockImportResult;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistBlockTask extends AbstractEthTask<Block> {

  private static final Logger LOG = LoggerFactory.getLogger(PersistBlockTask.class);

  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final EthContext ethContext;
  private final Block block;
  private final HeaderValidationMode validateHeaders;
  private BlockImportResult blockImportResult;

  private PersistBlockTask(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final Block block,
      final HeaderValidationMode headerValidationMode,
      final MetricsSystem metricsSystem) {
    super(metricsSystem);
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
    this.block = block;
    this.validateHeaders = headerValidationMode;
    blockImportResult = new BlockImportResult(false);
  }

  public static PersistBlockTask create(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final Block block,
      final HeaderValidationMode headerValidationMode,
      final MetricsSystem metricsSystem) {
    return new PersistBlockTask(
        protocolSchedule, protocolContext, ethContext, block, headerValidationMode, metricsSystem);
  }

  public static Supplier<CompletableFuture<List<Block>>> forSequentialBlocks(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final List<Block> blocks,
      final HeaderValidationMode headerValidationMode,
      final MetricsSystem metricsSystem) {
    checkArgument(!blocks.isEmpty(), "No blocks to import provided");
    return () -> {
      final List<Block> successfulImports = new ArrayList<>();
      final Iterator<Block> blockIterator = blocks.iterator();
      CompletableFuture<Block> future =
          importBlockAndAddToList(
              protocolSchedule,
              protocolContext,
              ethContext,
              blockIterator.next(),
              successfulImports,
              headerValidationMode,
              metricsSystem);
      while (blockIterator.hasNext()) {
        final Block block = blockIterator.next();
        future =
            future.thenCompose(
                b ->
                    importBlockAndAddToList(
                        protocolSchedule,
                        protocolContext,
                        ethContext,
                        block,
                        successfulImports,
                        headerValidationMode,
                        metricsSystem));
      }
      return future.thenApply(r -> successfulImports);
    };
  }

  private static CompletableFuture<Block> importBlockAndAddToList(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final Block block,
      final List<Block> list,
      final HeaderValidationMode headerValidationMode,
      final MetricsSystem metricsSystem) {
    return PersistBlockTask.create(
            protocolSchedule,
            protocolContext,
            ethContext,
            block,
            headerValidationMode,
            metricsSystem)
        .run()
        .whenComplete(
            (r, t) -> {
              if (r != null) {
                list.add(r);
              }
            });
  }

  public static Supplier<CompletableFuture<List<Block>>> forUnorderedBlocks(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final List<Block> blocks,
      final HeaderValidationMode headerValidationMode,
      final MetricsSystem metricsSystem) {
    checkArgument(!blocks.isEmpty(), "No blocks to import provided");
    return () -> {
      final CompletableFuture<List<Block>> finalResult = new CompletableFuture<>();
      final List<Block> successfulImports = new ArrayList<>();
      final Iterator<PersistBlockTask> tasks =
          blocks.stream()
              .map(
                  block ->
                      PersistBlockTask.create(
                          protocolSchedule,
                          protocolContext,
                          ethContext,
                          block,
                          headerValidationMode,
                          metricsSystem))
              .iterator();

      CompletableFuture<Block> future = tasks.next().run();
      while (tasks.hasNext()) {
        final PersistBlockTask task = tasks.next();
        future =
            future
                .handle((r, t) -> r)
                .thenCompose(
                    r -> {
                      if (r != null) {
                        successfulImports.add(r);
                      }
                      return task.run();
                    });
      }
      future.whenComplete(
          (r, t) -> {
            if (r != null) {
              successfulImports.add(r);
            }
            if (successfulImports.size() > 0) {
              finalResult.complete(successfulImports);
            } else {
              finalResult.completeExceptionally(t);
            }
          });

      return finalResult;
    };
  }

  @Override
  protected void executeTask() {
    try {
      final ProtocolSpec protocolSpec = protocolSchedule.getByBlockHeader(block.getHeader());
      final BlockImporter blockImporter = protocolSpec.getBlockImporter();
      LOG.atDebug()
          .setMessage("Running import task for block {}")
          .addArgument(block::toLogString)
          .log();
      blockImportResult = blockImporter.importBlock(protocolContext, block, validateHeaders);
      if (!blockImportResult.isImported()) {
        result.completeExceptionally(InvalidBlockException.fromInvalidBlock(block.getHeader()));
        return;
      }
      result.complete(block);
    } catch (final Exception e) {
      result.completeExceptionally(e);
    }
  }

  @Override
  protected void cleanup() {
    final double timeInS = getTaskTimeInSec();
    switch (blockImportResult.getStatus()) {
      case IMPORTED:
        LOG.info(
            String.format(
                "Imported %s #%,d / %d tx / %d om / %,d (%01.1f%%) gas / (%s) in %01.3fs. Peers: %d",
                block.getBody().getTransactions().size() == 0 ? "empty block" : "block",
                block.getHeader().getNumber(),
                block.getBody().getTransactions().size(),
                block.getBody().getOmmers().size(),
                block.getHeader().getGasUsed(),
                (block.getHeader().getGasUsed() * 100.0) / block.getHeader().getGasLimit(),
                block.getHash().toHexString(),
                timeInS,
                ethContext.getEthPeers().peerCount()));
        break;
      case ALREADY_IMPORTED:
        LOG.info("Block {} is already imported", block.toLogString());
        break;
      default:
        break;
    }
  }
}
