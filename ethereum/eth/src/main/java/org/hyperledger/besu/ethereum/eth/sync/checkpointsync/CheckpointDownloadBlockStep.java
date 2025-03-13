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
package org.idnecology.idn.ethereum.eth.sync.checkpointsync;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockWithReceipts;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResponseCode;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResult;
import org.idnecology.idn.ethereum.eth.manager.peertask.task.GetReceiptsFromPeerTask;
import org.idnecology.idn.ethereum.eth.manager.task.AbstractPeerTask.PeerTaskResult;
import org.idnecology.idn.ethereum.eth.manager.task.GetBlockFromPeerTask;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.eth.sync.fastsync.checkpoint.Checkpoint;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CheckpointDownloadBlockStep {

  private final ProtocolSchedule protocolSchedule;
  private final EthContext ethContext;
  private final Checkpoint checkpoint;
  private final SynchronizerConfiguration synchronizerConfiguration;
  private final MetricsSystem metricsSystem;

  public CheckpointDownloadBlockStep(
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final Checkpoint checkpoint,
      final SynchronizerConfiguration synchronizerConfiguration,
      final MetricsSystem metricsSystem) {
    this.protocolSchedule = protocolSchedule;
    this.ethContext = ethContext;
    this.checkpoint = checkpoint;
    this.synchronizerConfiguration = synchronizerConfiguration;
    this.metricsSystem = metricsSystem;
  }

  public CompletableFuture<Optional<BlockWithReceipts>> downloadBlock(final Hash hash) {
    final GetBlockFromPeerTask getBlockFromPeerTask =
        GetBlockFromPeerTask.create(
            protocolSchedule,
            ethContext,
            synchronizerConfiguration,
            Optional.of(hash),
            checkpoint.blockNumber(),
            metricsSystem);
    return getBlockFromPeerTask
        .run()
        .thenCompose(this::downloadReceipts)
        .exceptionally(throwable -> Optional.empty());
  }

  private CompletableFuture<Optional<BlockWithReceipts>> downloadReceipts(
      final PeerTaskResult<Block> peerTaskResult) {
    final Block block = peerTaskResult.getResult();
    if (synchronizerConfiguration.isPeerTaskSystemEnabled()) {
      return ethContext
          .getScheduler()
          .scheduleServiceTask(
              () -> {
                GetReceiptsFromPeerTask task =
                    new GetReceiptsFromPeerTask(List.of(block.getHeader()), protocolSchedule);
                PeerTaskExecutorResult<Map<BlockHeader, List<TransactionReceipt>>> executorResult =
                    ethContext.getPeerTaskExecutor().execute(task);

                if (executorResult.responseCode() == PeerTaskExecutorResponseCode.SUCCESS) {
                  List<TransactionReceipt> transactionReceipts =
                      executorResult
                          .result()
                          .map((map) -> map.get(block.getHeader()))
                          .orElseThrow(
                              () ->
                                  new IllegalStateException(
                                      "PeerTask response code was success, but empty"));
                  if (block.getBody().getTransactions().size() != transactionReceipts.size()) {
                    throw new IllegalStateException(
                        "PeerTask response code was success, but incorrect number of receipts returned");
                  }
                  BlockWithReceipts blockWithReceipts =
                      new BlockWithReceipts(block, transactionReceipts);
                  return CompletableFuture.completedFuture(Optional.of(blockWithReceipts));
                } else {
                  return CompletableFuture.completedFuture(Optional.empty());
                }
              });

    } else {
      final org.idnecology.idn.ethereum.eth.manager.task.GetReceiptsFromPeerTask
          getReceiptsFromPeerTask =
              org.idnecology.idn.ethereum.eth.manager.task.GetReceiptsFromPeerTask.forHeaders(
                  ethContext, List.of(block.getHeader()), metricsSystem);
      return getReceiptsFromPeerTask
          .run()
          .thenCompose(
              receiptTaskResult -> {
                final Optional<List<TransactionReceipt>> transactionReceipts =
                    Optional.ofNullable(receiptTaskResult.getResult().get(block.getHeader()));
                return CompletableFuture.completedFuture(
                    transactionReceipts.map(receipts -> new BlockWithReceipts(block, receipts)));
              })
          .exceptionally(throwable -> Optional.empty());
    }
  }
}
