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
package org.idnecology.idn.ethereum.api.util;

import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.AccessListEntry;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.encoding.EncodingContext;
import org.idnecology.idn.ethereum.core.encoding.TransactionEncoder;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;

import java.math.BigInteger;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DomainObjectDecodeUtilsTest {

  static final BlockDataGenerator gen = new BlockDataGenerator();
  private static final SECPSignature signature =
      SignatureAlgorithmFactory.getInstance()
          .createSignature(BigInteger.ONE, BigInteger.TEN, (byte) 1);
  private static final Address sender =
      Address.fromHexString("0x0000000000000000000000000000000000000003");

  private static final Transaction accessListTxn =
      Transaction.builder()
          .chainId(BigInteger.valueOf(2018))
          .accessList(List.of(new AccessListEntry(gen.address(), List.of(gen.bytes32()))))
          .nonce(1)
          .gasPrice(Wei.of(12))
          .gasLimit(43)
          .payload(Bytes.EMPTY)
          .value(Wei.ZERO)
          .signature(signature)
          .sender(sender)
          .guessType()
          .build();

  @Test
  public void testAccessListRLPSerDes() {
    final BytesValueRLPOutput encoded = new BytesValueRLPOutput();
    TransactionEncoder.encodeRLP(accessListTxn, encoded, EncodingContext.POOLED_TRANSACTION);
    Transaction decoded =
        DomainObjectDecodeUtils.decodeRawTransaction(encoded.encoded().toHexString());
    Assertions.assertThat(decoded.getAccessList().isPresent()).isTrue();
    Assertions.assertThat(decoded.getAccessList().map(List::size).get()).isEqualTo(1);
  }

  @Test
  public void testAccessList2718OpaqueSerDes() {
    final Bytes encoded =
        TransactionEncoder.encodeOpaqueBytes(accessListTxn, EncodingContext.POOLED_TRANSACTION);
    Transaction decoded = DomainObjectDecodeUtils.decodeRawTransaction(encoded.toString());
    Assertions.assertThat(decoded.getAccessList().isPresent()).isTrue();
    Assertions.assertThat(decoded.getAccessList().map(List::size).get()).isEqualTo(1);
  }
}
