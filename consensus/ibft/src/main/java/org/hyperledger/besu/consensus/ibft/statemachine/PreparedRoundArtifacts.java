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
package org.idnecology.idn.consensus.ibft.statemachine;

import org.idnecology.idn.consensus.ibft.messagewrappers.Prepare;
import org.idnecology.idn.consensus.ibft.messagewrappers.Proposal;
import org.idnecology.idn.consensus.ibft.payload.PreparedCertificate;
import org.idnecology.idn.ethereum.core.Block;

import java.util.Collection;
import java.util.stream.Collectors;

/** The Prepared round artifacts. */
public class PreparedRoundArtifacts {

  private final Proposal proposal;
  private final Collection<Prepare> prepares;

  /**
   * Instantiates a new Prepared round artifacts.
   *
   * @param proposal the proposal
   * @param prepares the prepares
   */
  public PreparedRoundArtifacts(final Proposal proposal, final Collection<Prepare> prepares) {
    this.proposal = proposal;
    this.prepares = prepares;
  }

  /**
   * Gets block.
   *
   * @return the block
   */
  public Block getBlock() {
    return proposal.getBlock();
  }

  /**
   * Gets prepared certificate.
   *
   * @return the prepared certificate
   */
  public PreparedCertificate getPreparedCertificate() {
    return new PreparedCertificate(
        proposal.getSignedPayload(),
        prepares.stream().map(Prepare::getSignedPayload).collect(Collectors.toList()));
  }
}
