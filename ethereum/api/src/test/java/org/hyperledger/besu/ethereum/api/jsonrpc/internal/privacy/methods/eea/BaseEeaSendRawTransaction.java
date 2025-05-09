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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.eea;

import static org.idnecology.idn.ethereum.core.PrivacyParameters.DEFAULT_PRIVACY;
import static org.idnecology.idn.ethereum.core.PrivacyParameters.FLEXIBLE_PRIVACY;
import static org.idnecology.idn.ethereum.core.PrivacyParameters.PLUGIN_PRIVACY;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivateTransaction;
import org.idnecology.idn.ethereum.privacy.markertransaction.FixedKeySigningPrivateMarkerTransactionFactory;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.evm.gascalculator.BerlinGasCalculator;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.plugin.data.Restriction;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import java.math.BigInteger;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.mockito.Mock;

public class BaseEeaSendRawTransaction {

  final String MOCK_ORION_KEY = "bW9ja2tleQ==";

  final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM_SUPPLIER =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);

  final KeyPair keyPair =
      SIGNATURE_ALGORITHM_SUPPLIER
          .get()
          .createKeyPair(
              SIGNATURE_ALGORITHM_SUPPLIER
                  .get()
                  .createPrivateKey(
                      new BigInteger(
                          "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63", 16)));

  final PrivateMarkerTransactionFactory privateMarkerTransactionFactory =
      new FixedKeySigningPrivateMarkerTransactionFactory(keyPair);

  final GasCalculator gasCalculator = new BerlinGasCalculator();

  final Transaction PUBLIC_FLEXIBLE_TRANSACTION =
      new Transaction.Builder()
          .nonce(0)
          .gasPrice(Wei.ONE)
          .gasLimit(21000)
          .value(Wei.ZERO)
          .to(FLEXIBLE_PRIVACY)
          .signature(
              SIGNATURE_ALGORITHM_SUPPLIER
                  .get()
                  .createSignature(
                      new BigInteger(
                          "104310573331543561412661001400556426894275857431274618344686100036716947434951"),
                      new BigInteger(
                          "33080506591748900530090726168809539464160321639149722208454899701475015405641"),
                      Byte.parseByte("1")))
          .payload(Bytes.fromBase64String(MOCK_ORION_KEY))
          .sender(Address.wrap(Bytes.fromHexString("0x8411b12666f68ef74cace3615c9d5a377729d03f")))
          .build();

  final Transaction PUBLIC_PLUGIN_TRANSACTION =
      new Transaction.Builder()
          .nonce(0)
          .gasPrice(Wei.ONE)
          .gasLimit(21112)
          .value(Wei.ZERO)
          .to(PLUGIN_PRIVACY)
          .signature(
              SIGNATURE_ALGORITHM_SUPPLIER
                  .get()
                  .createSignature(
                      new BigInteger(
                          "111331907905663242841915789134040957461022579868467291368609335839524284474080"),
                      new BigInteger(
                          "16338460226177675602590882211136457396059831699034102939076916361204709826919"),
                      Byte.parseByte("0")))
          .payload(Bytes.fromBase64String(MOCK_ORION_KEY))
          .sender(Address.wrap(Bytes.fromHexString("0x8411b12666f68ef74cace3615c9d5a377729d03f")))
          .build();

  final Transaction PUBLIC_OFF_CHAIN_TRANSACTION =
      new Transaction.Builder()
          .nonce(0)
          .gasPrice(Wei.ONE)
          .gasLimit(21000)
          .value(Wei.ZERO)
          .to(DEFAULT_PRIVACY)
          .signature(
              SIGNATURE_ALGORITHM_SUPPLIER
                  .get()
                  .createSignature(
                      new BigInteger(
                          "45331864585825234947874751069766983839005678711670143534492294352090223768785"),
                      new BigInteger(
                          "32813839561238589140263096892921088101761344639911577803805398248765156383629"),
                      Byte.parseByte("1")))
          .payload(Bytes.fromBase64String(MOCK_ORION_KEY))
          .sender(Address.wrap(Bytes.fromHexString("0x8411b12666f68ef74cace3615c9d5a377729d03f")))
          .build();

  final JsonRpcRequestContext validPrivateForTransactionRequest =
      new JsonRpcRequestContext(
          new JsonRpcRequest(
              "2.0", "eea_sendRawTransaction", new String[] {validPrivateForTransaction()}));

  final JsonRpcRequestContext validPrivacyGroupTransactionRequest =
      new JsonRpcRequestContext(
          new JsonRpcRequest(
              "2.0",
              "eea_sendRawTransaction",
              new String[] {validPrivatePrivacyGroupTransaction(Restriction.RESTRICTED)}));

  final JsonRpcRequestContext validUnrestrictedPrivacyGroupTransactionRequest =
      new JsonRpcRequestContext(
          new JsonRpcRequest(
              "2.0",
              "eea_sendRawTransaction",
              new String[] {validPrivatePrivacyGroupTransaction(Restriction.UNRESTRICTED)}));

  final JsonRpcRequestContext validUnsuportedPrivacyGroupTransactionRequest =
      new JsonRpcRequestContext(
          new JsonRpcRequest(
              "2.0",
              "eea_sendRawTransaction",
              new String[] {validPrivatePrivacyGroupTransaction(Restriction.UNSUPPORTED)}));

  @Mock TransactionPool transactionPool;
  @Mock PrivacyController privacyController;

  private String validPrivateForTransaction() {
    final PrivateTransaction.Builder privateTransactionBuilder =
        PrivateTransaction.builder()
            .nonce(0)
            .gasPrice(Wei.of(1))
            .gasLimit(21000)
            .value(Wei.ZERO)
            .payload(Bytes.EMPTY)
            .to(Address.fromHexString("0x095e7baea6a6c7c4c2dfeb977efac326af552d87"))
            .chainId(BigInteger.ONE)
            .privateFrom(Bytes.fromBase64String("S28yYlZxRCtuTmxOWUw1RUU3eTNJZE9udmlmdGppaXp="))
            .privateFor(
                List.of(
                    Bytes.fromBase64String("S28yYlZxRCtuTmxOWUw1RUU3eTNJZE9udmlmdGppaXp="),
                    Bytes.fromBase64String("QTFhVnRNeExDVUhtQlZIWG9aenpCZ1BiVy93ajVheER=")))
            .restriction(Restriction.RESTRICTED);
    return rlpEncodeTransaction(privateTransactionBuilder);
  }

  private String validPrivatePrivacyGroupTransaction(final Restriction restriction) {
    final PrivateTransaction.Builder privateTransactionBuilder =
        PrivateTransaction.builder()
            .nonce(0)
            .gasPrice(Wei.of(1))
            .gasLimit(21000)
            .value(Wei.ZERO)
            .payload(Bytes.EMPTY)
            .to(Address.fromHexString("0x095e7baea6a6c7c4c2dfeb977efac326af552d87"))
            .chainId(BigInteger.ONE)
            .privateFrom(Bytes.fromBase64String("S28yYlZxRCtuTmxOWUw1RUU3eTNJZE9udmlmdGppaXp="))
            .privacyGroupId(Bytes.fromBase64String("DyAOiF/ynpc+JXa2YAGB0bCitSlOMNm+ShmB/7M6C4w="))
            .restriction(restriction);
    return rlpEncodeTransaction(privateTransactionBuilder);
  }

  private String rlpEncodeTransaction(final PrivateTransaction.Builder privateTransactionBuilder) {
    final PrivateTransaction privateTransaction = privateTransactionBuilder.signAndBuild(keyPair);
    final BytesValueRLPOutput bvrlp = new BytesValueRLPOutput();
    privateTransaction.writeTo(bvrlp);
    return bvrlp.encoded().toHexString();
  }
}
