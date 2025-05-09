/*
 * Copyright 2019 ConsenSys AG.
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
package org.idnecology.idn.ethereum.worldstate;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.rlp.RLPInput;
import org.idnecology.idn.ethereum.trie.common.PmtStateTrieAccountValue;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

public class StateTrieAccountValueTest {

  @Test
  public void roundTripMainNetAccountValue() {
    final long nonce = 0;
    final Wei balance = Wei.ZERO;
    // Have the storageRoot and codeHash as different values to ensure the encode / decode
    // doesn't cross the values over.
    final Hash storageRoot = Hash.EMPTY_TRIE_HASH;
    final Hash codeHash = Hash.EMPTY_LIST_HASH;

    roundTripMainNetAccountValue(nonce, balance, storageRoot, codeHash);
  }

  private void roundTripMainNetAccountValue(
      final long nonce, final Wei balance, final Hash storageRoot, final Hash codeHash) {

    PmtStateTrieAccountValue accountValue =
        new PmtStateTrieAccountValue(nonce, balance, storageRoot, codeHash);
    Bytes encoded = RLP.encode(accountValue::writeTo);
    final RLPInput in = RLP.input(encoded);
    PmtStateTrieAccountValue roundTripAccountValue = PmtStateTrieAccountValue.readFrom(in);

    assertThat(nonce).isEqualTo(roundTripAccountValue.getNonce());
    assertThat(balance).isEqualTo(roundTripAccountValue.getBalance());
    assertThat(storageRoot).isEqualTo(roundTripAccountValue.getStorageRoot());
    assertThat(codeHash).isEqualTo(roundTripAccountValue.getCodeHash());
  }
}
