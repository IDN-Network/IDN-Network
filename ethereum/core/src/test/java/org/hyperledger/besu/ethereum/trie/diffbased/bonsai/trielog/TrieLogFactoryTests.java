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
package org.idnecology.idn.ethereum.trie.diffbased.bonsai.trielog;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.StorageSlotKey;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.BlockchainSetupUtil;
import org.idnecology.idn.ethereum.trie.common.PmtStateTrieAccountValue;
import org.idnecology.idn.ethereum.trie.diffbased.common.trielog.TrieLogLayer;
import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.plugin.services.trielogs.TrieLog;
import org.idnecology.idn.plugin.services.trielogs.TrieLogFactory;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TrieLogFactoryTests {

  final BlockchainSetupUtil setup = BlockchainSetupUtil.forTesting(DataStorageFormat.BONSAI);

  final Address accountFixture = Address.fromHexString("0xdeadbeef");

  final BlockHeader headerFixture =
      new BlockHeaderTestFixture()
          .parentHash(setup.getGenesisState().getBlock().getHash())
          .coinbase(Address.ZERO)
          .buildHeader();

  final TrieLogLayer trieLogFixture =
      new TrieLogLayer()
          .setBlockHash(headerFixture.getBlockHash())
          .addAccountChange(
              accountFixture,
              null,
              new PmtStateTrieAccountValue(0, Wei.fromEth(1), Hash.EMPTY, Hash.EMPTY))
          .addCodeChange(
              Address.ZERO,
              null,
              Bytes.fromHexString("0xfeeddeadbeef"),
              headerFixture.getBlockHash())
          .addStorageChange(Address.ZERO, new StorageSlotKey(UInt256.ZERO), null, UInt256.ONE);

  @Test
  public void testSerializeDeserializeAreEqual() {

    TrieLogFactory factory = new TrieLogFactoryImpl();
    byte[] rlp = factory.serialize(trieLogFixture);

    TrieLog layer = factory.deserialize(rlp);
    assertThat(layer).isEqualTo(trieLogFixture);
  }
}
