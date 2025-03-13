/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.core.encoding;

import static org.idnecology.idn.ethereum.core.encoding.TransactionEncoder.writeSignatureAndV;

import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.rlp.RLPOutput;

import org.apache.tuweni.bytes.Bytes;

public class FrontierTransactionEncoder {
  public static void encode(final Transaction transaction, final RLPOutput out) {
    out.startList();
    out.writeLongScalar(transaction.getNonce());
    out.writeUInt256Scalar(transaction.getGasPrice().orElseThrow());
    out.writeLongScalar(transaction.getGasLimit());
    out.writeBytes(transaction.getTo().map(Bytes::copy).orElse(Bytes.EMPTY));
    out.writeUInt256Scalar(transaction.getValue());
    out.writeBytes(transaction.getPayload());
    writeSignatureAndV(transaction, out);
    out.endList();
  }
}
