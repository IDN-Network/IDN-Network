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
package org.idnecology.idn.ethereum.eth.sync.fastsync.checkpoint;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.Difficulty;

import org.immutables.value.Value;

@Value.Immutable
public interface Checkpoint {

  long blockNumber();

  Hash blockHash();

  Difficulty totalDifficulty();
}
