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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.LatestNonceProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.eea.PluginEeaSendRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.eea.RestrictedFlexibleEeaSendRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.eea.RestrictedOffchainEeaSendRawTransaction;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetEeaTransactionCount;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.util.NonceProvider;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import java.util.Map;

public class EeaJsonRpcMethods extends PrivacyApiGroupJsonRpcMethods {

  private final TransactionPool transactionPool;
  private final PrivacyParameters privacyParameters;
  private final NonceProvider nonceProvider;

  public EeaJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final PrivacyParameters privacyParameters) {
    super(blockchainQueries, protocolSchedule, transactionPool, privacyParameters);
    this.transactionPool = transactionPool;
    this.privacyParameters = privacyParameters;
    this.nonceProvider = new LatestNonceProvider(blockchainQueries, transactionPool);
  }

  @Override
  protected Map<String, JsonRpcMethod> create(
      final PrivacyController privacyController,
      final PrivacyIdProvider privacyIdProvider,
      final PrivateMarkerTransactionFactory privateMarkerTransactionFactory) {

    if (privacyParameters.isPrivacyPluginEnabled()) {
      return mapOf(
          new PluginEeaSendRawTransaction(
              transactionPool,
              privacyIdProvider,
              privateMarkerTransactionFactory,
              nonceProvider,
              privacyController,
              getGasCalculator()),
          new PrivGetEeaTransactionCount(privacyController, privacyIdProvider));
    } else if (getPrivacyParameters().isFlexiblePrivacyGroupsEnabled()) {
      return mapOf(
          new RestrictedFlexibleEeaSendRawTransaction(
              transactionPool,
              privacyIdProvider,
              privateMarkerTransactionFactory,
              nonceProvider,
              privacyController),
          new PrivGetEeaTransactionCount(privacyController, privacyIdProvider));
    } else { // off chain privacy
      return mapOf(
          new RestrictedOffchainEeaSendRawTransaction(
              transactionPool,
              privacyIdProvider,
              privateMarkerTransactionFactory,
              nonceProvider,
              privacyController),
          new PrivGetEeaTransactionCount(privacyController, privacyIdProvider));
    }
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.EEA.name();
  }
}
