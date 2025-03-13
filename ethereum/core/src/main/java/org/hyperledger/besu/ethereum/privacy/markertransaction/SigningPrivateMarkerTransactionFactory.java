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
package org.idnecology.idn.ethereum.privacy.markertransaction;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.plugin.data.UnsignedPrivateMarkerTransaction;

import org.apache.tuweni.bytes.Bytes;

public class SigningPrivateMarkerTransactionFactory {

  protected Bytes signAndBuild(
      final UnsignedPrivateMarkerTransaction unsignedPrivateMarkerTransaction,
      final KeyPair signingKey) {
    final Transaction transaction =
        Transaction.builder()
            .type(TransactionType.FRONTIER)
            .nonce(unsignedPrivateMarkerTransaction.getNonce())
            .gasPrice(
                unsignedPrivateMarkerTransaction.getGasPrice().map(Wei::fromQuantity).orElse(null))
            .gasLimit(unsignedPrivateMarkerTransaction.getGasLimit())
            .to(unsignedPrivateMarkerTransaction.getTo().get())
            .value(Wei.fromQuantity(unsignedPrivateMarkerTransaction.getValue()))
            .payload(unsignedPrivateMarkerTransaction.getPayload())
            .signAndBuild(signingKey);

    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    transaction.writeTo(out);
    return out.encoded();
  }
}
