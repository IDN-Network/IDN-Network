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

import org.idnecology.idn.Runner;
import org.idnecology.idn.cli.IdnCommand;
import org.idnecology.idn.controller.IdnController;

/**
 * All PreSynchronizationTask instances execute after the {@link IdnController} instance in {@link
 * IdnCommand}* is ready and before {@link Runner#startEthereumMainLoop()} is called
 */
public interface PreSynchronizationTask {

  /**
   * Run.
   *
   * @param idnController the idn controller
   */
  void run(final IdnController idnController);
}
