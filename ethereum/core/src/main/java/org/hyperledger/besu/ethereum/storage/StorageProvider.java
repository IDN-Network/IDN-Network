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
package org.idnecology.idn.ethereum.storage;

import org.idnecology.idn.ethereum.chain.BlockchainStorage;
import org.idnecology.idn.ethereum.chain.VariablesStorage;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.DataStorageConfiguration;
import org.idnecology.idn.ethereum.worldstate.WorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;
import org.idnecology.idn.plugin.services.storage.SegmentIdentifier;
import org.idnecology.idn.plugin.services.storage.SegmentedKeyValueStorage;

import java.io.Closeable;
import java.util.List;

public interface StorageProvider extends Closeable {

  VariablesStorage createVariablesStorage();

  BlockchainStorage createBlockchainStorage(
      ProtocolSchedule protocolSchedule,
      VariablesStorage variablesStorage,
      DataStorageConfiguration storageConfiguration);

  WorldStateKeyValueStorage createWorldStateStorage(
      DataStorageConfiguration dataStorageConfiguration);

  WorldStateStorageCoordinator createWorldStateStorageCoordinator(
      DataStorageConfiguration dataStorageConfiguration);

  WorldStatePreimageStorage createWorldStatePreimageStorage();

  KeyValueStorage getStorageBySegmentIdentifier(SegmentIdentifier segment);

  SegmentedKeyValueStorage getStorageBySegmentIdentifiers(List<SegmentIdentifier> segment);
}
