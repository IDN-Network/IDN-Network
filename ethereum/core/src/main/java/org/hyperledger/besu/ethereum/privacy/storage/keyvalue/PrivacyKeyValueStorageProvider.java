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
package org.idnecology.idn.ethereum.privacy.storage.keyvalue;

import org.idnecology.idn.ethereum.privacy.storage.LegacyPrivateStateKeyValueStorage;
import org.idnecology.idn.ethereum.privacy.storage.LegacyPrivateStateStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivacyStorageProvider;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateKeyValueStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.plugin.services.storage.KeyValueStorage;

import java.io.IOException;

public class PrivacyKeyValueStorageProvider implements PrivacyStorageProvider {

  private final KeyValueStorage privateWorldStateKeyValueStorage;
  private final KeyValueStorage privateWorldStatePreimageKeyValueStorage;
  private final KeyValueStorage privateStateKeyValueStorage;

  private final int factoryVersion;

  public PrivacyKeyValueStorageProvider(
      final KeyValueStorage privateWorldStateKeyValueStorage,
      final KeyValueStorage privateWorldStatePreimageKeyValueStorage,
      final KeyValueStorage privateStateKeyValueStorage,
      final int factoryVersion) {
    this.privateWorldStateKeyValueStorage = privateWorldStateKeyValueStorage;
    this.privateWorldStatePreimageKeyValueStorage = privateWorldStatePreimageKeyValueStorage;
    this.privateStateKeyValueStorage = privateStateKeyValueStorage;
    this.factoryVersion = factoryVersion;
  }

  @Override
  public WorldStateKeyValueStorage createWorldStateStorage() {
    return new ForestWorldStateKeyValueStorage(privateWorldStateKeyValueStorage);
  }

  @Override
  public WorldStateStorageCoordinator createWorldStateStorageCoordinator() {
    return new WorldStateStorageCoordinator(createWorldStateStorage());
  }

  @Override
  public WorldStatePreimageStorage createWorldStatePreimageStorage() {
    return new WorldStatePreimageKeyValueStorage(privateWorldStatePreimageKeyValueStorage);
  }

  @Override
  public PrivateStateStorage createPrivateStateStorage() {
    return new PrivateStateKeyValueStorage(privateStateKeyValueStorage);
  }

  @Override
  public LegacyPrivateStateStorage createLegacyPrivateStateStorage() {
    return new LegacyPrivateStateKeyValueStorage(privateStateKeyValueStorage);
  }

  @Override
  public int getFactoryVersion() {
    return factoryVersion;
  }

  @Override
  public void close() throws IOException {
    privateWorldStateKeyValueStorage.close();
    privateWorldStatePreimageKeyValueStorage.close();
    privateStateKeyValueStorage.close();
  }
}
