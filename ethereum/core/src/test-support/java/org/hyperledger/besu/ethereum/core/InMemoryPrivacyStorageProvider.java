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
package org.idnecology.idn.ethereum.core;

import org.idnecology.idn.ethereum.privacy.storage.LegacyPrivateStateKeyValueStorage;
import org.idnecology.idn.ethereum.privacy.storage.LegacyPrivateStateStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivacyStorageProvider;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateKeyValueStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.ForestWorldStateArchive;
import org.idnecology.idn.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.forest.worldview.ForestMutableWorldState;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;
import org.idnecology.idn.ethereum.worldstate.WorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStatePreimageStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.services.kvstore.InMemoryKeyValueStorage;

public class InMemoryPrivacyStorageProvider implements PrivacyStorageProvider {

  public static WorldStateArchive createInMemoryWorldStateArchive() {
    return new ForestWorldStateArchive(
        new WorldStateStorageCoordinator(
            new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage())),
        new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage()),
        EvmConfiguration.DEFAULT);
  }

  public static MutableWorldState createInMemoryWorldState() {
    final InMemoryPrivacyStorageProvider provider = new InMemoryPrivacyStorageProvider();
    return new ForestMutableWorldState(
        provider.createWorldStateStorage(),
        provider.createWorldStatePreimageStorage(),
        EvmConfiguration.DEFAULT);
  }

  @Override
  public WorldStateKeyValueStorage createWorldStateStorage() {
    return new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
  }

  @Override
  public WorldStateStorageCoordinator createWorldStateStorageCoordinator() {
    return new WorldStateStorageCoordinator(createWorldStateStorage());
  }

  @Override
  public WorldStatePreimageStorage createWorldStatePreimageStorage() {
    return new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage());
  }

  @Override
  public PrivateStateStorage createPrivateStateStorage() {
    return new PrivateStateKeyValueStorage(new InMemoryKeyValueStorage());
  }

  @Override
  public LegacyPrivateStateStorage createLegacyPrivateStateStorage() {
    return new LegacyPrivateStateKeyValueStorage(new InMemoryKeyValueStorage());
  }

  @Override
  public int getFactoryVersion() {
    return 1;
  }

  @Override
  public void close() {
    // no cleanup for in-memory data storage
  }
}
