/*
 * Copyright 2020 ConsenSys AG.
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
package org.idnecology.idn.consensus.qbft.core.statemachine;

import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.qbft.core.payload.PreparePayload;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlock;

import java.util.List;

/** The Prepared certificate. */
public class PreparedCertificate {

  private final QbftBlock block;
  private final List<SignedData<PreparePayload>> prepares;
  private final int round;

  /**
   * Instantiates a new Prepared certificate.
   *
   * @param block the block
   * @param prepares the prepares
   * @param round the round
   */
  public PreparedCertificate(
      final QbftBlock block, final List<SignedData<PreparePayload>> prepares, final int round) {
    this.block = block;
    this.prepares = prepares;
    this.round = round;
  }

  /**
   * Gets round.
   *
   * @return the round
   */
  public int getRound() {
    return round;
  }

  /**
   * Gets block.
   *
   * @return the block
   */
  public QbftBlock getBlock() {
    return block;
  }

  /**
   * Gets list of Prepare payload messages.
   *
   * @return the prepares
   */
  public List<SignedData<PreparePayload>> getPrepares() {
    return prepares;
  }
}
