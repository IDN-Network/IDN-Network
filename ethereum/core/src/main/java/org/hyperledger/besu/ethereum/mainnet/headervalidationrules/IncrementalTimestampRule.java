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
package org.idnecology.idn.ethereum.mainnet.headervalidationrules;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.mainnet.AttachedBlockHeaderValidationRule;

public class IncrementalTimestampRule implements AttachedBlockHeaderValidationRule {

  @Override
  public boolean validate(
      final BlockHeader header, final BlockHeader parent, final ProtocolContext protocolContext) {

    final long blockTimestamp = header.getTimestamp();
    final long parentTimestamp = parent.getTimestamp();

    return Long.compareUnsigned(blockTimestamp, parentTimestamp) > 0;
  }
}
