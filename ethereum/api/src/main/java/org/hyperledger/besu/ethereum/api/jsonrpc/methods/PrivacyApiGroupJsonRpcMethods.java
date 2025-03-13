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

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.DisabledPrivacyRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.MultiTenancyRpcMethodDecorator;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.query.PrivacyQueries;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.privacy.ChainHeadPrivateNonceProvider;
import org.idnecology.idn.ethereum.privacy.FlexiblePrivacyController;
import org.idnecology.idn.ethereum.privacy.MultiTenancyPrivacyController;
import org.idnecology.idn.ethereum.privacy.PluginPrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivateNonceProvider;
import org.idnecology.idn.ethereum.privacy.PrivateTransactionSimulator;
import org.idnecology.idn.ethereum.privacy.RestrictedDefaultPrivacyController;
import org.idnecology.idn.ethereum.privacy.markertransaction.FixedKeySigningPrivateMarkerTransactionFactory;
import org.idnecology.idn.ethereum.privacy.markertransaction.RandomSigningPrivateMarkerTransactionFactory;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.plugin.services.privacy.PrivateMarkerTransactionFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated(since = "24.12.0")
public abstract class PrivacyApiGroupJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final BlockchainQueries blockchainQueries;
  private final ProtocolSchedule protocolSchedule;
  private final TransactionPool transactionPool;
  private final PrivacyParameters privacyParameters;
  private final PrivateNonceProvider privateNonceProvider;
  private final PrivacyQueries privacyQueries;

  protected PrivacyApiGroupJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final PrivacyParameters privacyParameters) {
    this.blockchainQueries = blockchainQueries;
    this.protocolSchedule = protocolSchedule;
    this.transactionPool = transactionPool;
    this.privacyParameters = privacyParameters;

    this.privateNonceProvider =
        new ChainHeadPrivateNonceProvider(
            blockchainQueries.getBlockchain(),
            privacyParameters.getPrivateStateRootResolver(),
            privacyParameters.getPrivateWorldStateArchive());

    this.privacyQueries =
        new PrivacyQueries(blockchainQueries, privacyParameters.getPrivateWorldStateReader());
  }

  public BlockchainQueries getBlockchainQueries() {
    return blockchainQueries;
  }

  public ProtocolSchedule getProtocolSchedule() {
    return protocolSchedule;
  }

  public TransactionPool getTransactionPool() {
    return transactionPool;
  }

  public PrivacyParameters getPrivacyParameters() {
    return privacyParameters;
  }

  public GasCalculator getGasCalculator() {
    return protocolSchedule
        .getByBlockHeader(blockchainQueries.headBlockHeader())
        .getGasCalculator();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final PrivateMarkerTransactionFactory markerTransactionFactory =
        createPrivateMarkerTransactionFactory();
    final PrivacyIdProvider enclavePublicProvider = PrivacyIdProvider.build(privacyParameters);
    final PrivacyController privacyController = createPrivacyController();
    return create(privacyController, enclavePublicProvider, markerTransactionFactory)
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> createPrivacyMethod(privacyParameters, entry.getValue())));
  }

  protected abstract Map<String, JsonRpcMethod> create(
      final PrivacyController privacyController,
      final PrivacyIdProvider privacyIdProvider,
      PrivateMarkerTransactionFactory markerTransactionFactory);

  private PrivateMarkerTransactionFactory createPrivateMarkerTransactionFactory() {
    if (privacyParameters.getPrivacyService() != null
        && privacyParameters.getPrivacyService().getPrivateMarkerTransactionFactory() != null) {
      return privacyParameters.getPrivacyService().getPrivateMarkerTransactionFactory();
    } else if (privacyParameters.getSigningKeyPair().isPresent()) {
      return new FixedKeySigningPrivateMarkerTransactionFactory(
          privacyParameters.getSigningKeyPair().get());
    }
    return new RandomSigningPrivateMarkerTransactionFactory();
  }

  private PrivacyController createPrivacyController() {
    final Optional<BigInteger> chainId = protocolSchedule.getChainId();

    if (privacyParameters.isPrivacyPluginEnabled()) {
      return new PluginPrivacyController(
          getBlockchainQueries().getBlockchain(),
          privacyParameters,
          chainId,
          createPrivateTransactionSimulator(),
          privateNonceProvider,
          privacyParameters.getPrivateWorldStateReader());
    } else {
      final PrivacyController privacyController;
      if (privacyParameters.isFlexiblePrivacyGroupsEnabled()) {
        privacyController =
            new FlexiblePrivacyController(
                getBlockchainQueries().getBlockchain(),
                privacyParameters,
                chainId,
                createPrivateTransactionSimulator(),
                privateNonceProvider,
                privacyParameters.getPrivateWorldStateReader());
      } else {
        privacyController =
            new RestrictedDefaultPrivacyController(
                getBlockchainQueries().getBlockchain(),
                privacyParameters,
                chainId,
                createPrivateTransactionSimulator(),
                privateNonceProvider,
                privacyParameters.getPrivateWorldStateReader());
      }
      return privacyParameters.isMultiTenancyEnabled()
          ? new MultiTenancyPrivacyController(privacyController)
          : privacyController;
    }
  }

  PrivacyQueries getPrivacyQueries() {
    return privacyQueries;
  }

  private JsonRpcMethod createPrivacyMethod(
      final PrivacyParameters privacyParameters, final JsonRpcMethod rpcMethod) {
    final String methodName = rpcMethod.getName();
    if (methodName.equals(RpcMethod.ETH_SEND_RAW_PRIVATE_TRANSACTION.getMethodName())) {
      return rpcMethod;
    } else if (privacyParameters.isEnabled() && privacyParameters.isMultiTenancyEnabled()) {
      return new MultiTenancyRpcMethodDecorator(rpcMethod);
    } else if (!privacyParameters.isEnabled()) {
      return new DisabledPrivacyRpcMethod(methodName);
    } else {
      return rpcMethod;
    }
  }

  private PrivateTransactionSimulator createPrivateTransactionSimulator() {
    return new PrivateTransactionSimulator(
        getBlockchainQueries().getBlockchain(),
        getBlockchainQueries().getWorldStateArchive(),
        getProtocolSchedule(),
        getPrivacyParameters());
  }
}
