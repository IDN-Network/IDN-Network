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
package org.idnecology.idn.util;

import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.privacy.PrivateStateRootResolver;
import org.idnecology.idn.ethereum.privacy.storage.LegacyPrivateStateStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.privacy.storage.migration.PrivateMigrationBlockProcessor;
import org.idnecology.idn.ethereum.privacy.storage.migration.PrivateStorageMigration;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

/** The Private storage migration builder. */
public class PrivateStorageMigrationBuilder {

  private final IdnController idnController;
  private final PrivacyParameters privacyParameters;

  /**
   * Instantiates a new Private storage migration builder.
   *
   * @param idnController the idn controller
   * @param privacyParameters the privacy parameters
   */
  public PrivateStorageMigrationBuilder(
      final IdnController idnController, final PrivacyParameters privacyParameters) {
    this.idnController = idnController;
    this.privacyParameters = privacyParameters;
  }

  /**
   * Build private storage migration.
   *
   * @return the private storage migration
   */
  public PrivateStorageMigration build() {
    final Blockchain blockchain = idnController.getProtocolContext().getBlockchain();
    final Address privacyPrecompileAddress = privacyParameters.getPrivacyAddress();
    final ProtocolSchedule protocolSchedule = idnController.getProtocolSchedule();
    final WorldStateArchive publicWorldStateArchive =
        idnController.getProtocolContext().getWorldStateArchive();
    final PrivateStateStorage privateStateStorage = privacyParameters.getPrivateStateStorage();
    final LegacyPrivateStateStorage legacyPrivateStateStorage =
        privacyParameters.getPrivateStorageProvider().createLegacyPrivateStateStorage();
    final PrivateStateRootResolver privateStateRootResolver =
        privacyParameters.getPrivateStateRootResolver();

    return new PrivateStorageMigration(
        blockchain,
        privacyPrecompileAddress,
        protocolSchedule,
        publicWorldStateArchive,
        privateStateStorage,
        privateStateRootResolver,
        legacyPrivateStateStorage,
        PrivateMigrationBlockProcessor::new);
  }
}
