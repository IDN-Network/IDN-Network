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
package org.idnecology.idn.consensus.qbft.blockcreation;

import org.idnecology.idn.config.QbftConfigOptions;
import org.idnecology.idn.consensus.common.ConsensusHelpers;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

import java.util.Collections;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

/** Supports contract based voters and validators in extra data */
public class QbftBlockCreatorFactory extends BftBlockCreatorFactory<QbftConfigOptions> {
  /**
   * Instantiates a new Qbft block creator factory.
   *
   * @param transactionPool the pending transactions
   * @param protocolContext the protocol context
   * @param protocolSchedule the protocol schedule
   * @param forksSchedule the forks schedule
   * @param miningParams the mining params
   * @param localAddress the local address
   * @param bftExtraDataCodec the bft extra data codec
   * @param ethScheduler the scheduler for asynchronous block creation tasks
   */
  public QbftBlockCreatorFactory(
      final TransactionPool transactionPool,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final ForksSchedule<QbftConfigOptions> forksSchedule,
      final MiningConfiguration miningParams,
      final Address localAddress,
      final BftExtraDataCodec bftExtraDataCodec,
      final EthScheduler ethScheduler) {
    super(
        transactionPool,
        protocolContext,
        protocolSchedule,
        forksSchedule,
        miningParams,
        localAddress,
        bftExtraDataCodec,
        ethScheduler);
  }

  @Override
  public Bytes createExtraData(final int round, final BlockHeader parentHeader) {
    if (forksSchedule.getFork(parentHeader.getNumber() + 1L).getValue().isValidatorContractMode()) {
      // vote and validators will come from contract instead of block
      final BftExtraData extraData =
          new BftExtraData(
              ConsensusHelpers.zeroLeftPad(
                  miningConfiguration.getExtraData(), BftExtraDataCodec.EXTRA_VANITY_LENGTH),
              Collections.emptyList(),
              Optional.empty(),
              round,
              Collections.emptyList());
      return bftExtraDataCodec.encode(extraData);
    }

    return super.createExtraData(round, parentHeader);
  }
}
