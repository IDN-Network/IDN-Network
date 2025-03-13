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
package org.idnecology.idn.ethereum.eth.transactions.layered;

import static org.idnecology.idn.ethereum.eth.transactions.layered.LayeredRemovalReason.PoolRemovalReason.DROPPED;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactionAddedListener;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactionDroppedListener;
import org.idnecology.idn.ethereum.eth.transactions.TransactionAddedResult;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.layered.LayeredRemovalReason.PoolRemovalReason;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.util.Subscribers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;

public class EndLayer implements TransactionsLayer {

  private final TransactionPoolMetrics metrics;
  private final Subscribers<PendingTransactionAddedListener> onAddedListeners =
      Subscribers.create();

  private final Subscribers<PendingTransactionDroppedListener> onDroppedListeners =
      Subscribers.create();

  private long droppedCount = 0;

  public EndLayer(final TransactionPoolMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public String name() {
    return "end";
  }

  @Override
  public void reset() {
    droppedCount = 0;
  }

  @Override
  public Optional<Transaction> getByHash(final Hash transactionHash) {
    return Optional.empty();
  }

  @Override
  public boolean contains(final Transaction transaction) {
    return false;
  }

  @Override
  public List<PendingTransaction> getAll() {
    return List.of();
  }

  @Override
  public TransactionAddedResult add(
      final PendingTransaction pendingTransaction, final int gap, final AddReason reason) {
    notifyTransactionDropped(pendingTransaction, DROPPED);
    metrics.incrementRemoved(pendingTransaction, DROPPED.label(), name());
    ++droppedCount;
    return TransactionAddedResult.DROPPED;
  }

  @Override
  public void remove(final PendingTransaction pendingTransaction, final PoolRemovalReason reason) {}

  @Override
  public void penalize(final PendingTransaction penalizedTx) {}

  @Override
  public void blockAdded(
      final FeeMarket feeMarket,
      final BlockHeader blockHeader,
      final Map<Address, Long> maxConfirmedNonceBySender) {
    // no-op
  }

  @Override
  public List<Transaction> getAllLocal() {
    return List.of();
  }

  @Override
  public List<Transaction> getAllPriority() {
    return List.of();
  }

  @Override
  public int count() {
    return 0;
  }

  @Override
  public OptionalLong getNextNonceFor(final Address sender) {
    return OptionalLong.empty();
  }

  @Override
  public OptionalLong getCurrentNonceFor(final Address sender) {
    return OptionalLong.empty();
  }

  @Override
  public List<PendingTransaction> promote(
      final Predicate<PendingTransaction> promotionFilter,
      final long freeSpace,
      final int freeSlots,
      final int[] remainingPromotionsPerType) {
    return List.of();
  }

  @Override
  public long subscribeToAdded(final PendingTransactionAddedListener listener) {
    return onAddedListeners.subscribe(listener);
  }

  @Override
  public void unsubscribeFromAdded(final long id) {
    onAddedListeners.unsubscribe(id);
  }

  @Override
  public long subscribeToDropped(final PendingTransactionDroppedListener listener) {
    return onDroppedListeners.subscribe(listener);
  }

  @Override
  public void unsubscribeFromDropped(final long id) {
    onDroppedListeners.unsubscribe(id);
  }

  protected void notifyTransactionDropped(
      final PendingTransaction pendingTransaction, final LayeredRemovalReason reason) {
    onDroppedListeners.forEach(
        listener -> listener.onTransactionDropped(pendingTransaction.getTransaction(), reason));
  }

  @Override
  public PendingTransaction promoteFor(
      final Address sender, final long nonce, final int[] remainingPromotionsPerType) {
    return null;
  }

  @Override
  public void notifyAdded(final PendingTransaction pendingTransaction) {
    // no-op
  }

  @Override
  public long getCumulativeUsedSpace() {
    return 0;
  }

  @Override
  public String logStats() {
    return "Dropped: " + droppedCount;
  }

  @Override
  public String logSender(final Address sender) {
    return "";
  }

  @Override
  public List<PendingTransaction> getAllFor(final Address sender) {
    return List.of();
  }
}
