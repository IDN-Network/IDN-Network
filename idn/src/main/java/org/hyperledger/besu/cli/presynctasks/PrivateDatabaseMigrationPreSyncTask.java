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
package org.idnecology.idn.cli.presynctasks;

import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.privacy.storage.migration.PrivateStorageMigrationService;
import org.idnecology.idn.util.PrivateStorageMigrationBuilder;

/** The Private database migration pre sync task. */
public class PrivateDatabaseMigrationPreSyncTask implements PreSynchronizationTask {

  private final PrivacyParameters privacyParameters;
  private final boolean migratePrivateDatabaseFlag;

  /**
   * Instantiates a new Private database migration pre sync task.
   *
   * @param privacyParameters the privacy parameters
   * @param migratePrivateDatabaseFlag the migrate private database flag
   */
  public PrivateDatabaseMigrationPreSyncTask(
      final PrivacyParameters privacyParameters, final boolean migratePrivateDatabaseFlag) {
    this.privacyParameters = privacyParameters;
    this.migratePrivateDatabaseFlag = migratePrivateDatabaseFlag;
  }

  @Override
  public void run(final IdnController idnController) {
    final PrivateStorageMigrationBuilder privateStorageMigrationBuilder =
        new PrivateStorageMigrationBuilder(idnController, privacyParameters);
    final PrivateStorageMigrationService privateStorageMigrationService =
        new PrivateStorageMigrationService(
            privacyParameters.getPrivateStateStorage(),
            migratePrivateDatabaseFlag,
            privateStorageMigrationBuilder::build);

    privateStorageMigrationService.runMigrationIfRequired();
  }
}
