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

import static org.idnecology.idn.ethereum.eth.transactions.layered.AddReason.MOVE;
import static org.idnecology.idn.ethereum.eth.transactions.layered.LayeredRemovalReason.LayerMoveReason.DEMOTED;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransaction;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.mainnet.feemarket.BaseFeeMarket;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFeePrioritizedTransactions extends AbstractPrioritizedTransactions {

  private static final Logger LOG = LoggerFactory.getLogger(BaseFeePrioritizedTransactions.class);
  private Optional<Wei> nextBlockBaseFee;

  public BaseFeePrioritizedTransactions(
      final TransactionPoolConfiguration poolConfig,
      final Supplier<BlockHeader> chainHeadHeaderSupplier,
      final EthScheduler ethScheduler,
      final TransactionsLayer nextLayer,
      final TransactionPoolMetrics metrics,
      final BiFunction<PendingTransaction, PendingTransaction, Boolean>
          transactionReplacementTester,
      final FeeMarket feeMarket,
      final BlobCache blobCache,
      final MiningConfiguration miningConfiguration) {
    super(
        poolConfig,
        ethScheduler,
        nextLayer,
        metrics,
        transactionReplacementTester,
        blobCache,
        miningConfiguration);
    this.nextBlockBaseFee =
        Optional.of(calculateNextBlockBaseFee(feeMarket, chainHeadHeaderSupplier.get()));
  }

  @Override
  protected int compareByFee(final PendingTransaction pt1, final PendingTransaction pt2) {
    return Comparator.comparing(PendingTransaction::getScore)
        .thenComparing(PendingTransaction::hasPriority)
        .thenComparing(
            (PendingTransaction pendingTransaction) ->
                pendingTransaction.getTransaction().getEffectivePriorityFeePerGas(nextBlockBaseFee))
        .thenComparing(
            (PendingTransaction pendingTransaction) ->
                pendingTransaction.getTransaction().getMaxGasPrice())
        .thenComparing(Comparator.comparing(PendingTransaction::getNonce).reversed())
        .thenComparing(PendingTransaction::getSequence)
        .compare(pt1, pt2);
  }

  /**
   * On base fee markets when a new block is added we can calculate the base fee for the next block
   * and use it to keep only pending transactions willing to pay at least that fee in the
   * prioritized layer, since only these transactions are executable, while all the other can be
   * demoted to the next layer.
   *
   * @param blockHeader the header of the added block
   * @param feeMarket the fee market
   */
  @Override
  protected void internalBlockAdded(final BlockHeader blockHeader, final FeeMarket feeMarket) {
    final Wei newNextBlockBaseFee = calculateNextBlockBaseFee(feeMarket, blockHeader);

    LOG.atTrace()
        .setMessage("Updating base fee from {} to {}")
        .addArgument(nextBlockBaseFee.get()::toHumanReadableString)
        .addArgument(newNextBlockBaseFee::toHumanReadableString)
        .log();

    nextBlockBaseFee = Optional.of(newNextBlockBaseFee);
    orderByFee.clear();

    final var itTxsBySender = txsBySender.entrySet().iterator();
    while (itTxsBySender.hasNext()) {
      final var senderTxs = itTxsBySender.next().getValue();

      Optional<Long> maybeFirstDemotedNonce = Optional.empty();

      for (final var e : senderTxs.entrySet()) {
        final PendingTransaction tx = e.getValue();
        // it must pass the promotion filter to be prioritized
        if (promotionFilter(tx)) {
          orderByFee.add(tx);
        } else {
          // otherwise sender txs starting from this nonce need to be demoted to next layer,
          // and we can go to next sender
          maybeFirstDemotedNonce = Optional.of(e.getKey());
          break;
        }
      }

      maybeFirstDemotedNonce.ifPresent(
          nonce -> {
            // demote all txs after the first demoted to the next layer, because none of them is
            // executable now, and we can avoid sorting them until they are candidate for execution
            // again
            final var demoteTxs = senderTxs.tailMap(nonce, true);
            while (!demoteTxs.isEmpty()) {
              final PendingTransaction demoteTx = demoteTxs.pollLastEntry().getValue();
              LOG.atTrace()
                  .setMessage(
                      "Demoting tx {} since it does not respect anymore the requisites to stay in this layer."
                          + " Next block base fee {}")
                  .addArgument(demoteTx::toTraceLog)
                  .addArgument(newNextBlockBaseFee::toHumanReadableString)
                  .log();
              processEvict(senderTxs, demoteTx, DEMOTED);
              addToNextLayer(senderTxs, demoteTx, 0, MOVE);
            }
          });

      if (senderTxs.isEmpty()) {
        itTxsBySender.remove();
      }
    }
  }

  private Wei calculateNextBlockBaseFee(final FeeMarket feeMarket, final BlockHeader blockHeader) {
    if (feeMarket.implementsBaseFee()) {
      final var baseFeeMarket = (BaseFeeMarket) feeMarket;
      return baseFeeMarket.computeBaseFee(
          blockHeader.getNumber() + 1,
          blockHeader.getBaseFee().orElse(Wei.ZERO),
          blockHeader.getGasUsed(),
          baseFeeMarket.targetGasUsed(blockHeader));
    }
    return Wei.ZERO;
  }

  @Override
  protected boolean promotionFilter(final PendingTransaction pendingTransaction) {

    // check if the tx is willing to pay at least the base fee
    if (nextBlockBaseFee
        .map(pendingTransaction.getTransaction().getMaxGasPrice()::lessThan)
        .orElse(true)) {
      return false;
    }

    // priority txs are promoted even if they pay less
    if (!pendingTransaction.hasPriority()) {
      // check if effective gas price is higher than the min gas price
      if (pendingTransaction
          .getTransaction()
          .getEffectiveGasPrice(nextBlockBaseFee)
          .lessThan(miningConfiguration.getMinTransactionGasPrice())) {
        return false;
      }

      // check if enough priority fee is paid
      if (!miningConfiguration.getMinPriorityFeePerGas().equals(Wei.ZERO)) {
        final Wei priorityFeePerGas =
            pendingTransaction.getTransaction().getEffectivePriorityFeePerGas(nextBlockBaseFee);
        if (priorityFeePerGas.lessThan(miningConfiguration.getMinPriorityFeePerGas())) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  protected String internalLogStats() {

    if (orderByFee.isEmpty()) {
      return "Basefee Prioritized: Empty";
    }

    final PendingTransaction highest = orderByFee.last();
    final PendingTransaction lowest = orderByFee.first();

    return "Basefee Prioritized: "
        + "count: "
        + pendingTransactions.size()
        + ", space used: "
        + spaceUsed
        + ", unique senders: "
        + txsBySender.size()
        + ", highest priority tx: [score: "
        + highest.getScore()
        + ", max fee: "
        + highest.getTransaction().getMaxGasPrice().toHumanReadableString()
        + ", curr prio fee: "
        + highest
            .getTransaction()
            .getEffectivePriorityFeePerGas(nextBlockBaseFee)
            .toHumanReadableString()
        + ", hash: "
        + highest.getHash()
        + "], lowest priority tx: [score: "
        + lowest.getScore()
        + ", max fee: "
        + lowest.getTransaction().getMaxGasPrice().toHumanReadableString()
        + ", curr prio fee: "
        + lowest
            .getTransaction()
            .getEffectivePriorityFeePerGas(nextBlockBaseFee)
            .toHumanReadableString()
        + ", hash: "
        + lowest.getHash()
        + "], next block base fee: "
        + nextBlockBaseFee.get().toHumanReadableString();
  }
}
