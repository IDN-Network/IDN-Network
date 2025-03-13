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
package org.idnecology.idn.consensus.qbft.adaptor;

import org.idnecology.idn.consensus.qbft.core.types.QbftBlockHeader;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockImporter;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockValidator;
import org.idnecology.idn.consensus.qbft.core.types.QbftProtocolSchedule;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;

/**
 * Adaptor class to allow a {@link ProtocolSchedule} to be used as a {@link QbftProtocolSchedule}.
 */
public class QbftProtocolScheduleAdaptor implements QbftProtocolSchedule {

  private final ProtocolSchedule idnProtocolSchedule;
  private final ProtocolContext context;

  /**
   * Constructs a new Qbft protocol schedule.
   *
   * @param idnProtocolSchedule The Idn protocol schedule.
   * @param context The protocol context.
   */
  public QbftProtocolScheduleAdaptor(
      final ProtocolSchedule idnProtocolSchedule, final ProtocolContext context) {
    this.idnProtocolSchedule = idnProtocolSchedule;
    this.context = context;
  }

  @Override
  public QbftBlockImporter getBlockImporter(final QbftBlockHeader header) {
    return new QbftBlockImporterAdaptor(
        getProtocolSpecByBlockHeader(header).getBlockImporter(), context);
  }

  @Override
  public QbftBlockValidator getBlockValidator(final QbftBlockHeader header) {
    return new QbftBlockValidatorAdaptor(
        getProtocolSpecByBlockHeader(header).getBlockValidator(), context);
  }

  private ProtocolSpec getProtocolSpecByBlockHeader(final QbftBlockHeader header) {
    return idnProtocolSchedule.getByBlockHeader(BlockUtil.toIdnBlockHeader(header));
  }
}
