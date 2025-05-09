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
package org.idnecology.idn.ethereum.mainnet;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.evm.account.MutableAccount;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainnetBlockProcessor extends AbstractBlockProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(MainnetBlockProcessor.class);

  public MainnetBlockProcessor(
      final MainnetTransactionProcessor transactionProcessor,
      final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory,
      final Wei blockReward,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final boolean skipZeroBlockRewards,
      final ProtocolSchedule protocolSchedule) {
    super(
        transactionProcessor,
        transactionReceiptFactory,
        blockReward,
        miningBeneficiaryCalculator,
        skipZeroBlockRewards,
        protocolSchedule);
  }

  @Override
  protected boolean rewardCoinbase(
      final MutableWorldState worldState,
      final BlockHeader header,
      final List<BlockHeader> ommers,
      final boolean skipZeroBlockRewards) {
    if (skipZeroBlockRewards && blockReward.isZero()) {
      return true;
    }

    final Wei coinbaseReward = getCoinbaseReward(blockReward, header.getNumber(), ommers.size());
    final WorldUpdater updater = worldState.updater();
    final Address miningBeneficiary = getMiningBeneficiaryCalculator().calculateBeneficiary(header);
    final MutableAccount miningBeneficiaryAccount = updater.getOrCreate(miningBeneficiary);

    miningBeneficiaryAccount.incrementBalance(coinbaseReward);
    for (final BlockHeader ommerHeader : ommers) {
      if (ommerHeader.getNumber() - header.getNumber() > MAX_GENERATION) {
        LOG.info(
            "Block processing error: ommer block number {} more than {} generations. Block {}",
            ommerHeader.getNumber(),
            MAX_GENERATION,
            header.getHash().toHexString());
        return false;
      }

      final MutableAccount ommerCoinbase = updater.getOrCreate(ommerHeader.getCoinbase());
      final Wei ommerReward =
          getOmmerReward(blockReward, header.getNumber(), ommerHeader.getNumber());
      ommerCoinbase.incrementBalance(ommerReward);
    }

    updater.commit();

    return true;
  }
}
