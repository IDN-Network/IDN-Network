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
package org.idnecology.idn.ethereum.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.InMemoryPrivacyStorageProvider;
import org.idnecology.idn.ethereum.privacy.storage.PrivacyGroupHeadBlockMap;
import org.idnecology.idn.ethereum.privacy.storage.PrivateBlockMetadata;
import org.idnecology.idn.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.privacy.storage.PrivateTransactionMetadata;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrivateMetadataUpdaterTest {

  private PrivateMetadataUpdater updater;
  private BlockHeader blockHeader;
  private PrivateStateStorage privateStateStorage;
  private Hash hashBlockOne;
  private Bytes32 privacyGroupId;
  private Hash stateRoot;
  private Hash pmtHash;

  @BeforeEach
  public void before() {
    blockHeader = mock(BlockHeader.class);
    privateStateStorage = new InMemoryPrivacyStorageProvider().createPrivateStateStorage();
    final Hash hashBlockZero = Hash.ZERO;
    when(blockHeader.getParentHash()).thenReturn(hashBlockZero);
    updater = new PrivateMetadataUpdater(blockHeader, privateStateStorage);
    hashBlockOne =
        Hash.fromHexString("1111111111111111111111111111111111111111111111111111111111111111");
    stateRoot =
        Hash.fromHexString("2222222222222222222222222222222222222222222222222222222222222222");
    privacyGroupId =
        Bytes32.fromHexString("3333333333333333333333333333333333333333333333333333333333333333");
  }

  @Test
  public void returnsEmptyPrivateGroupHeadBlockMapForUnknownBlock() {
    assertThat(updater.getPrivacyGroupHeadBlockMap()).isEqualTo(PrivacyGroupHeadBlockMap.empty());
  }

  @Test
  public void addingMetadataSuccessfull() {
    when(blockHeader.getHash()).thenReturn(hashBlockOne);
    pmtHash = Hash.ZERO;
    final PrivateTransactionMetadata expected = new PrivateTransactionMetadata(pmtHash, stateRoot);
    updater.addPrivateTransactionMetadata(privacyGroupId, expected);
    updater.commit();
    final Optional<PrivateBlockMetadata> privateBlockMetadata =
        privateStateStorage.getPrivateBlockMetadata(hashBlockOne, privacyGroupId);
    assertThat(privateBlockMetadata.get().getLatestStateRoot().get()).isEqualTo(stateRoot);
  }

  @Test
  public void updatesPrivacyGroupHeadBlockMap() {
    when(blockHeader.getHash()).thenReturn(hashBlockOne);
    updater.updatePrivacyGroupHeadBlockMap(privacyGroupId);
    updater.commit();
    final PrivacyGroupHeadBlockMap actual =
        privateStateStorage.getPrivacyGroupHeadBlockMap(hashBlockOne).get();
    assertThat(actual.get(privacyGroupId)).isEqualTo(hashBlockOne);
  }
}
