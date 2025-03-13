/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.ethereum.eth.manager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.EthPeers;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResponseCode;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutorResult;
import org.idnecology.idn.ethereum.eth.transactions.PeerTransactionTracker;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.metrics.StubMetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.netty.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BufferedGetPooledTransactionsFromPeerFetcherUsingPeerTaskTest {

  private @Mock EthPeer ethPeer;
  private @Mock TransactionPool transactionPool;
  private @Mock EthContext ethContext;
  private @Mock EthPeers ethPeers;
  private @Mock PeerTaskExecutor peerTaskExecutor;

  private final BlockDataGenerator generator = new BlockDataGenerator();
  private final EthScheduler ethScheduler = new DeterministicEthScheduler();

  private BufferedGetPooledTransactionsFromPeerFetcher fetcher;
  private StubMetricsSystem metricsSystem;
  private PeerTransactionTracker transactionTracker;

  @BeforeEach
  public void setup() {
    metricsSystem = new StubMetricsSystem();
    when(ethContext.getEthPeers()).thenReturn(ethPeers);
    transactionTracker = new PeerTransactionTracker(ethPeers);
    when(ethContext.getScheduler()).thenReturn(ethScheduler);
    when(ethContext.getPeerTaskExecutor()).thenReturn(peerTaskExecutor);
    ScheduledFuture<?> mock = mock(ScheduledFuture.class);
    fetcher =
        new BufferedGetPooledTransactionsFromPeerFetcher(
            ethContext,
            mock,
            ethPeer,
            transactionPool,
            transactionTracker,
            new TransactionPoolMetrics(metricsSystem),
            "new_pooled_transaction_hashes",
            true);
  }

  @Test
  public void requestTransactionShouldStartTaskWhenUnknownTransaction() {
    final Transaction transaction = generator.transaction();
    final List<Transaction> taskResult = List.of(transaction);
    final PeerTaskExecutorResult<List<Transaction>> peerTaskResult =
        new PeerTaskExecutorResult<List<Transaction>>(
            Optional.of(taskResult), PeerTaskExecutorResponseCode.SUCCESS, Optional.of(ethPeer));

    when(peerTaskExecutor.executeAgainstPeer(
            any(
                org.idnecology.idn.ethereum.eth.manager.peertask.task
                    .GetPooledTransactionsFromPeerTask.class),
            eq(ethPeer)))
        .thenReturn(peerTaskResult);

    fetcher.addHashes(List.of(transaction.getHash()));
    fetcher.requestTransactions();

    verify(peerTaskExecutor)
        .executeAgainstPeer(
            any(
                org.idnecology.idn.ethereum.eth.manager.peertask.task
                    .GetPooledTransactionsFromPeerTask.class),
            eq(ethPeer));
    verifyNoMoreInteractions(peerTaskExecutor);
    verify(transactionPool, times(1)).addRemoteTransactions(taskResult);

    assertThat(transactionTracker.hasSeenTransaction(transaction.getHash())).isTrue();
  }

  @Test
  public void requestTransactionShouldSplitRequestIntoSeveralTasks() {
    final Map<Hash, Transaction> transactionsByHash =
        IntStream.range(0, 257)
            .mapToObj(unused -> generator.transaction())
            .collect(Collectors.toMap((t) -> t.getHash(), (t) -> t));
    fetcher.addHashes(transactionsByHash.keySet());

    when(peerTaskExecutor.executeAgainstPeer(
            any(
                org.idnecology.idn.ethereum.eth.manager.peertask.task
                    .GetPooledTransactionsFromPeerTask.class),
            eq(ethPeer)))
        .thenAnswer(
            (invocationOnMock) -> {
              org.idnecology.idn.ethereum.eth.manager.peertask.task
                      .GetPooledTransactionsFromPeerTask
                  task =
                      invocationOnMock.getArgument(
                          0,
                          org.idnecology.idn.ethereum.eth.manager.peertask.task
                              .GetPooledTransactionsFromPeerTask.class);
              List<Transaction> resultTransactions =
                  task.getHashes().stream().map(transactionsByHash::get).toList();
              return new PeerTaskExecutorResult<List<Transaction>>(
                  Optional.of(resultTransactions),
                  PeerTaskExecutorResponseCode.SUCCESS,
                  Optional.of(ethPeer));
            });

    fetcher.requestTransactions();

    verify(peerTaskExecutor, times(2))
        .executeAgainstPeer(
            any(
                org.idnecology.idn.ethereum.eth.manager.peertask.task
                    .GetPooledTransactionsFromPeerTask.class),
            eq(ethPeer));
    verifyNoMoreInteractions(peerTaskExecutor);
  }

  @Test
  public void requestTransactionShouldNotStartTaskWhenTransactionAlreadySeen() {

    final Transaction transaction = generator.transaction();
    final Hash hash = transaction.getHash();
    transactionTracker.markTransactionHashesAsSeen(ethPeer, List.of(hash));

    fetcher.addHashes(List.of(hash));
    fetcher.requestTransactions();

    verifyNoInteractions(peerTaskExecutor);
    verify(transactionPool, never()).addRemoteTransactions(List.of(transaction));
    assertThat(
            metricsSystem.getCounterValue(
                "remote_transactions_already_seen_total", "new_pooled_transaction_hashes"))
        .isEqualTo(1);
  }
}
