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
package org.idnecology.idn.controller;

import org.idnecology.idn.ethereum.core.MiningConfiguration;

/**
 * This interface wraps the provided MiningParameters to enable controller-specific parameter
 * overrides.
 */
public interface MiningParameterOverrides {
  /**
   * Overrides MiningParameter.
   *
   * @param fromCli The mining parameters that contains original values.
   * @return MiningParameters constructed from provided param with additional overridden parameters.
   */
  default MiningConfiguration getMiningParameterOverrides(final MiningConfiguration fromCli) {
    return fromCli;
  }
}
