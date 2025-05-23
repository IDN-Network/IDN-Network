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
package org.idnecology.idn.ethereum.eth.transactions.layered;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;
import static org.idnecology.idn.ethereum.eth.transactions.TransactionAddedResult.ALREADY_KNOWN;
import static org.idnecology.idn.ethereum.eth.transactions.TransactionAddedResult.INTERNAL_ERROR;
import static org.idnecology.idn.ethereum.eth.transactions.TransactionAddedResult.NONCE_TOO_FAR_IN_FUTURE_FOR_SENDER;
import static org.idnecology.idn.ethereum.eth.transactions.layered.AddReason.NEW;
import static org.idnecology.idn.ethereum.eth.transactions.layered.LayeredRemovalReason.PoolRemovalReason.INVALIDATED;
import static org.idnecology.idn.ethereum.eth.transactions.layered.LayeredRemovalReason.PoolRemovalReason.RECONCILED;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactionAddedListener;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactionDroppedListener;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactions;
import org.idnecology.idn.ethereum.eth.transactions.TransactionAddedResult;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.account.AccountState;
import org.idnecology.idn.plugin.data.TransactionSelectionResult;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import kotlin.ranges.LongRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class LayeredPendingTransactions implements PendingTransactions {
  private static final Logger LOG = LoggerFactory.getLogger(LayeredPendingTransactions.class);
  private static final Logger LOG_FOR_REPLAY = LoggerFactory.getLogger("LOG_FOR_REPLAY");
  private static final Marker INVALID_TX_REMOVED = MarkerFactory.getMarker("INVALID_TX_REMOVED");
  private final TransactionPoolConfiguration poolConfig;
  private final AbstractPrioritizedTransactions prioritizedTransactions;
  private final EthScheduler ethScheduler;

  public LayeredPendingTransactions(
      final TransactionPoolConfiguration poolConfig,
      final AbstractPrioritizedTransactions prioritizedTransactions,
      final EthScheduler ethScheduler) {
    this.poolConfig = poolConfig;
    this.prioritizedTransactions = prioritizedTransactions;
    this.ethScheduler = ethScheduler;
  }

  @Override
  public synchronized void reset() {
    prioritizedTransactions.reset();
  }

  @Override
  public synchronized TransactionAddedResult addTransaction(
      final PendingTransaction pendingTransaction, final Optional<Account> maybeSenderAccount) {

    final long stateSenderNonce = maybeSenderAccount.map(AccountState::getNonce).orElse(0L);

    logTransactionForReplayAdd(pendingTransaction, stateSenderNonce);

    if (hasAccountNonceDisparity(pendingTransaction, stateSenderNonce)) {
      reconcileSender(pendingTransaction.getSender(), stateSenderNonce);
    }

    final long nonceDistance = pendingTransaction.getNonce() - stateSenderNonce;

    final TransactionAddedResult nonceChecksResult =
        nonceChecks(pendingTransaction, stateSenderNonce, nonceDistance);
    if (nonceChecksResult != null) {
      return nonceChecksResult;
    }

    try {
      return prioritizedTransactions.add(pendingTransaction, (int) nonceDistance, NEW);
    } catch (final Throwable throwable) {
      return reconcileAndRetryAdd(
          pendingTransaction, stateSenderNonce, (int) nonceDistance, throwable);
    }
  }

  private TransactionAddedResult reconcileAndRetryAdd(
      final PendingTransaction pendingTransaction,
      final long stateSenderNonce,
      final int nonceDistance,
      final Throwable throwable) {
    // in case something unexpected happened, log this sender txs, force a reconcile and retry
    // another time
    LOG.atDebug()
        .setMessage(
            "Unexpected error when adding transaction {}, current sender status {}, force a reconcile and retry")
        .setCause(throwable)
        .addArgument(pendingTransaction::toTraceLog)
        .addArgument(() -> prioritizedTransactions.logSender(pendingTransaction.getSender()))
        .log();
    reconcileSender(pendingTransaction.getSender(), stateSenderNonce);
    try {
      return prioritizedTransactions.add(pendingTransaction, nonceDistance, NEW);
    } catch (final Throwable throwable2) {
      // the error should have been solved by the reconcile, logging at higher level now
      LOG.atWarn()
          .setCause(throwable2)
          .setMessage(
              "Unexpected error when adding transaction {} after reconciliation, current sender status {}")
          .addArgument(pendingTransaction.toTraceLog())
          .addArgument(prioritizedTransactions.logSender(pendingTransaction.getSender()))
          .log();
      return INTERNAL_ERROR;
    }
  }

  /**
   * Detect a disparity between account nonce has seen by the world state and the txpool, that could
   * happen during the small amount of time during block import when the world state is updated
   * while the txpool still does not process the confirmed txs, or when there is a reorg and the
   * sender nonce goes back.
   *
   * @param pendingTransaction the incoming transaction to check
   * @param stateSenderNonce account nonce from the world state
   * @return false if the nonce for the sender has seen by the txpool matches the value of the
   *     account nonce in the world state, true if they differ
   */
  private boolean hasAccountNonceDisparity(
      final PendingTransaction pendingTransaction, final long stateSenderNonce) {
    final OptionalLong maybeTxPoolSenderNonce =
        prioritizedTransactions.getCurrentNonceFor(pendingTransaction.getSender());
    if (maybeTxPoolSenderNonce.isPresent()) {
      final long txPoolSenderNonce = maybeTxPoolSenderNonce.getAsLong();
      if (stateSenderNonce != txPoolSenderNonce) {
        LOG.atDebug()
            .setMessage(
                "Nonce disparity detected when adding pending transaction {}. "
                    + "Account nonce from world state is {} while current txpool nonce is {}")
            .addArgument(pendingTransaction::toTraceLog)
            .addArgument(stateSenderNonce)
            .addArgument(txPoolSenderNonce)
            .log();
        return true;
      }
    }
    return false;
  }

  /**
   * Rebuild the txpool for a sender according to the specified nonce. This is used in case the
   * account nonce has seen by the txpool is not the correct one (see {@link
   * LayeredPendingTransactions#hasAccountNonceDisparity(PendingTransaction, long)} for when this
   * could happen). It works by removing all the txs for the sender and re-adding them using the
   * passed nonce.
   *
   * @param sender the sender for which rebuild the txpool
   * @param stateSenderNonce the world state account nonce to use in the txpool for the sender
   */
  private void reconcileSender(final Address sender, final long stateSenderNonce) {
    final var existingSenderTxs = prioritizedTransactions.getAllFor(sender);
    if (existingSenderTxs.isEmpty()) {
      LOG.debug("Sender {} has no transactions to reconcile", sender);
      return;
    }

    LOG.atDebug()
        .setMessage("Sender {} with nonce {} has {} transaction(s) to reconcile {}")
        .addArgument(sender)
        .addArgument(stateSenderNonce)
        .addArgument(existingSenderTxs::size)
        .addArgument(() -> prioritizedTransactions.logSender(sender))
        .log();

    final var reAddTxs = new ArrayDeque<PendingTransaction>(existingSenderTxs.size());

    // it is more performant to invalidate backward
    for (int i = existingSenderTxs.size() - 1; i >= 0; --i) {
      final var ptx = existingSenderTxs.get(i);
      prioritizedTransactions.remove(ptx, RECONCILED);
      if (ptx.getNonce() >= stateSenderNonce) {
        reAddTxs.addFirst(ptx);
      }
    }

    if (!reAddTxs.isEmpty()) {
      // re-add all the previous txs
      final long lowestNonce = reAddTxs.getFirst().getNonce();
      final int newNonceDistance = (int) Math.max(0, lowestNonce - stateSenderNonce);

      reAddTxs.forEach(ptx -> prioritizedTransactions.add(ptx, newNonceDistance, NEW));
    }

    LOG.atDebug()
        .setMessage("Sender {} with nonce {} status after reconciliation {}")
        .addArgument(sender)
        .addArgument(stateSenderNonce)
        .addArgument(() -> prioritizedTransactions.logSender(sender))
        .log();
  }

  private void logTransactionForReplayAdd(
      final PendingTransaction pendingTransaction, final long senderNonce) {
    // csv fields: sequence, addedAt, sender, sender_nonce, nonce, type, hash, rlp
    LOG_FOR_REPLAY
        .atTrace()
        .setMessage("T,{},{},{},{},{},{},{},{}")
        .addArgument(pendingTransaction.getSequence())
        .addArgument(pendingTransaction.getAddedAt())
        .addArgument(pendingTransaction.getSender())
        .addArgument(senderNonce)
        .addArgument(pendingTransaction.getNonce())
        .addArgument(pendingTransaction.getTransaction().getType())
        .addArgument(pendingTransaction::getHash)
        .addArgument(
            () -> {
              final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
              pendingTransaction.getTransaction().writeTo(rlp);
              return rlp.encoded().toHexString();
            })
        .log();
  }

  private void logDiscardedTransaction(
      final PendingTransaction pendingTransaction, final TransactionSelectionResult result) {
    // csv fields: sequence, addedAt, sender, nonce, type, hash, rlp
    LOG_FOR_REPLAY
        .atTrace()
        .setMessage("D,{},{},{},{},{},{},{}")
        .addArgument(pendingTransaction.getSequence())
        .addArgument(pendingTransaction.getAddedAt())
        .addArgument(pendingTransaction.getSender())
        .addArgument(pendingTransaction.getNonce())
        .addArgument(pendingTransaction.getTransaction().getType())
        .addArgument(pendingTransaction::getHash)
        .addArgument(
            () -> {
              final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
              pendingTransaction.getTransaction().writeTo(rlp);
              return rlp.encoded().toHexString();
            })
        .log();
    LOG.atInfo()
        .addMarker(INVALID_TX_REMOVED)
        .addKeyValue("txhash", pendingTransaction::getHash)
        .addKeyValue("txlog", pendingTransaction::toTraceLog)
        .addKeyValue("reason", result)
        .addKeyValue(
            "txrlp",
            () -> {
              final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
              pendingTransaction.getTransaction().writeTo(rlp);
              return rlp.encoded().toHexString();
            })
        .log();
  }

  private TransactionAddedResult nonceChecks(
      final PendingTransaction pendingTransaction,
      final long senderNonce,
      final long nonceDistance) {
    if (nonceDistance < 0) {
      LOG.atTrace()
          .setMessage("Drop already confirmed transaction {}, since current sender nonce is {}")
          .addArgument(pendingTransaction::toTraceLog)
          .addArgument(senderNonce)
          .log();
      return ALREADY_KNOWN;
    } else if (nonceDistance >= poolConfig.getMaxFutureBySender()) {
      LOG.atTrace()
          .setMessage(
              "Drop too much in the future transaction {}, since current sender nonce is {}")
          .addArgument(pendingTransaction::toTraceLog)
          .addArgument(senderNonce)
          .log();
      return NONCE_TOO_FAR_IN_FUTURE_FOR_SENDER;
    }
    return null;
  }

  @Override
  public void evictOldTransactions() {}

  @Override
  public synchronized List<Transaction> getLocalTransactions() {
    return prioritizedTransactions.getAllLocal();
  }

  @Override
  public synchronized List<Transaction> getPriorityTransactions() {
    return prioritizedTransactions.getAllPriority();
  }

  @Override
  public void selectTransactions(final PendingTransactions.TransactionSelector selector) {
    final Map<Byte, List<SenderPendingTransactions>> candidateTxsByScore;
    synchronized (this) {
      // since selecting transactions for block creation is a potential long operation
      // we want to avoid to keep the lock for all the process, but we just lock to get
      // the candidate transactions
      candidateTxsByScore = prioritizedTransactions.getByScore();
    }

    selection:
    for (final var entry : candidateTxsByScore.entrySet()) {
      LOG.trace("Evaluating txs with score {}", entry.getKey());

      for (final var senderTxs : entry.getValue()) {
        LOG.trace("Evaluating sender txs {}", senderTxs);

        for (final var candidatePendingTx : senderTxs.pendingTransactions()) {
          final var selectionResult = selector.evaluateTransaction(candidatePendingTx);

          LOG.atTrace()
              .setMessage("Selection result {} for transaction {}")
              .addArgument(selectionResult)
              .addArgument(candidatePendingTx::toTraceLog)
              .log();

          if (selectionResult.discard()) {
            ethScheduler.scheduleTxWorkerTask(
                () -> {
                  synchronized (this) {
                    prioritizedTransactions.remove(candidatePendingTx, INVALIDATED);
                  }
                });
            logDiscardedTransaction(candidatePendingTx, selectionResult);
          } else if (selectionResult.penalize()) {
            ethScheduler.scheduleTxWorkerTask(
                () -> {
                  synchronized (this) {
                    prioritizedTransactions.penalize(candidatePendingTx);
                  }
                });
            LOG.atTrace()
                .setMessage("Transaction {} penalized")
                .addArgument(candidatePendingTx::toTraceLog)
                .log();
          }

          if (selectionResult.stop()) {
            LOG.trace("Stopping selection");
            break selection;
          }
        }
      }
    }
  }

  @Override
  public long maxSize() {
    return -1;
  }

  @Override
  public synchronized int size() {
    return prioritizedTransactions.count();
  }

  @Override
  public synchronized boolean containsTransaction(final Transaction transaction) {
    return prioritizedTransactions.contains(transaction);
  }

  @Override
  public synchronized Optional<Transaction> getTransactionByHash(final Hash transactionHash) {
    return prioritizedTransactions.getByHash(transactionHash);
  }

  @Override
  public synchronized List<PendingTransaction> getPendingTransactions() {
    return prioritizedTransactions.getAll();
  }

  @Override
  public long subscribePendingTransactions(final PendingTransactionAddedListener listener) {
    return prioritizedTransactions.subscribeToAdded(listener);
  }

  @Override
  public void unsubscribePendingTransactions(final long id) {
    prioritizedTransactions.unsubscribeFromAdded(id);
  }

  @Override
  public long subscribeDroppedTransactions(final PendingTransactionDroppedListener listener) {
    return prioritizedTransactions.subscribeToDropped(listener);
  }

  @Override
  public void unsubscribeDroppedTransactions(final long id) {
    prioritizedTransactions.unsubscribeFromDropped(id);
  }

  @Override
  public OptionalLong getNextNonceForSender(final Address sender) {
    return prioritizedTransactions.getNextNonceFor(sender);
  }

  @Override
  public void manageBlockAdded(
      final BlockHeader blockHeader,
      final List<Transaction> confirmedTransactions,
      final List<Transaction> reorgTransactions,
      final FeeMarket feeMarket) {
    LOG.atTrace()
        .setMessage("Managing new added block {}")
        .addArgument(blockHeader::toLogString)
        .log();

    final var maxConfirmedNonceBySender = maxNonceBySender(confirmedTransactions);

    final var reorgNonceRangeBySender = nonceRangeBySender(reorgTransactions);

    synchronized (this) {
      try {
        prioritizedTransactions.blockAdded(feeMarket, blockHeader, maxConfirmedNonceBySender);
      } catch (final Throwable throwable) {
        LOG.warn(
            "Unexpected error {} when managing added block {}, maxNonceBySender {}, reorgNonceRangeBySender {}",
            throwable,
            blockHeader.toLogString(),
            maxConfirmedNonceBySender,
            reorgTransactions);
        LOG.warn("Stack trace", throwable);
      }

      logBlockHeaderForReplay(blockHeader, maxConfirmedNonceBySender, reorgNonceRangeBySender);
    }
  }

  private void logBlockHeaderForReplay(
      final BlockHeader blockHeader,
      final Map<Address, Long> maxConfirmedNonceBySender,
      final Map<Address, LongRange> reorgNonceRangeBySender) {
    // block number, block hash, sender, max nonce ..., rlp
    LOG_FOR_REPLAY
        .atTrace()
        .setMessage("B,{},{},{},R,{},{}")
        .addArgument(blockHeader.getNumber())
        .addArgument(blockHeader.getBlockHash())
        .addArgument(
            () ->
                maxConfirmedNonceBySender.entrySet().stream()
                    .map(e -> e.getKey().toHexString() + "," + e.getValue())
                    .collect(Collectors.joining(",")))
        .addArgument(
            () ->
                reorgNonceRangeBySender.entrySet().stream()
                    .map(
                        e ->
                            e.getKey().toHexString()
                                + ","
                                + e.getValue().getStart()
                                + ","
                                + e.getValue().getEndInclusive())
                    .collect(Collectors.joining(",")))
        .addArgument(
            () -> {
              final BytesValueRLPOutput rlp = new BytesValueRLPOutput();
              blockHeader.writeTo(rlp);
              return rlp.encoded().toHexString();
            })
        .log();
  }

  private Map<Address, Long> maxNonceBySender(final List<Transaction> confirmedTransactions) {
    record SenderNonce(Address sender, long nonce) {}

    return confirmedTransactions.stream()
        .<SenderNonce>mapMulti(
            (transaction, consumer) -> {
              // always consider the sender
              consumer.accept(new SenderNonce(transaction.getSender(), transaction.getNonce()));

              // and if a code delegation tx also the authorities
              if (transaction.getType().supportsDelegateCode()) {
                transaction.getCodeDelegationList().get().stream()
                    .map(cd -> cd.authorizer().map(address -> new SenderNonce(address, cd.nonce())))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(consumer);
              }
            })
        .collect(
            groupingBy(SenderNonce::sender, mapping(SenderNonce::nonce, reducing(0L, Math::max))));
  }

  private Map<Address, LongRange> nonceRangeBySender(
      final List<Transaction> confirmedTransactions) {

    class MutableLongRange {
      long start = Long.MAX_VALUE;
      long end = 0;

      void update(final long nonce) {
        if (nonce < start) {
          start = nonce;
        }
        if (nonce > end) {
          end = nonce;
        }
      }

      MutableLongRange combine(final MutableLongRange other) {
        update(other.start);
        update(other.end);
        return this;
      }

      LongRange toImmutable() {
        return new LongRange(start, end);
      }
    }

    return confirmedTransactions.stream()
        .collect(
            groupingBy(
                Transaction::getSender,
                mapping(
                    Transaction::getNonce,
                    Collector.of(
                        MutableLongRange::new,
                        MutableLongRange::update,
                        MutableLongRange::combine,
                        MutableLongRange::toImmutable))));
  }

  @Override
  public synchronized String toTraceLog() {
    return "";
  }

  @Override
  public synchronized String logStats() {
    return prioritizedTransactions.logStats();
  }

  @Override
  public Optional<Transaction> restoreBlob(final Transaction transaction) {
    return prioritizedTransactions.getBlobCache().restoreBlob(transaction);
  }
}
