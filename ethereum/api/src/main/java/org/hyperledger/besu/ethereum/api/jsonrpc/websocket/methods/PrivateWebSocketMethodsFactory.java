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
package org.idnecology.idn.ethereum.api.jsonrpc.websocket.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.privacy.ChainHeadPrivateNonceProvider;
import org.idnecology.idn.ethereum.privacy.FlexiblePrivacyController;
import org.idnecology.idn.ethereum.privacy.MultiTenancyPrivacyController;
import org.idnecology.idn.ethereum.privacy.PluginPrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivateNonceProvider;
import org.idnecology.idn.ethereum.privacy.PrivateTransactionSimulator;
import org.idnecology.idn.ethereum.privacy.RestrictedDefaultPrivacyController;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class PrivateWebSocketMethodsFactory {

  private final PrivacyParameters privacyParameters;
  private final SubscriptionManager subscriptionManager;
  private final ProtocolSchedule protocolSchedule;
  private final BlockchainQueries blockchainQueries;

  public PrivateWebSocketMethodsFactory(
      final PrivacyParameters privacyParameters,
      final SubscriptionManager subscriptionManager,
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries) {
    this.privacyParameters = privacyParameters;
    this.subscriptionManager = subscriptionManager;
    this.protocolSchedule = protocolSchedule;
    this.blockchainQueries = blockchainQueries;
  }

  public Collection<JsonRpcMethod> methods() {
    final SubscriptionRequestMapper subscriptionRequestMapper = new SubscriptionRequestMapper();
    final PrivacyIdProvider privacyIdProvider = PrivacyIdProvider.build(privacyParameters);
    final PrivacyController privacyController = createPrivacyController();

    return Set.of(
        new PrivSubscribe(
            subscriptionManager, subscriptionRequestMapper, privacyController, privacyIdProvider),
        new PrivUnsubscribe(
            subscriptionManager, subscriptionRequestMapper, privacyController, privacyIdProvider));
  }

  private PrivacyController createPrivacyController() {
    final Optional<BigInteger> chainId = protocolSchedule.getChainId();
    if (privacyParameters.isPrivacyPluginEnabled()) {
      return new PluginPrivacyController(
          blockchainQueries.getBlockchain(),
          privacyParameters,
          chainId,
          createPrivateTransactionSimulator(),
          createPrivateNonceProvider(),
          privacyParameters.getPrivateWorldStateReader());
    } else {
      final PrivacyController restrictedPrivacyController;
      if (privacyParameters.isFlexiblePrivacyGroupsEnabled()) {
        restrictedPrivacyController =
            new FlexiblePrivacyController(
                blockchainQueries.getBlockchain(),
                privacyParameters,
                chainId,
                createPrivateTransactionSimulator(),
                createPrivateNonceProvider(),
                privacyParameters.getPrivateWorldStateReader());
      } else {
        restrictedPrivacyController =
            new RestrictedDefaultPrivacyController(
                blockchainQueries.getBlockchain(),
                privacyParameters,
                chainId,
                createPrivateTransactionSimulator(),
                createPrivateNonceProvider(),
                privacyParameters.getPrivateWorldStateReader());
      }
      return privacyParameters.isMultiTenancyEnabled()
          ? new MultiTenancyPrivacyController(restrictedPrivacyController)
          : restrictedPrivacyController;
    }
  }

  private PrivateTransactionSimulator createPrivateTransactionSimulator() {
    return new PrivateTransactionSimulator(
        blockchainQueries.getBlockchain(),
        blockchainQueries.getWorldStateArchive(),
        protocolSchedule,
        privacyParameters);
  }

  private PrivateNonceProvider createPrivateNonceProvider() {
    return new ChainHeadPrivateNonceProvider(
        blockchainQueries.getBlockchain(),
        privacyParameters.getPrivateStateRootResolver(),
        privacyParameters.getPrivateWorldStateArchive());
  }
}
