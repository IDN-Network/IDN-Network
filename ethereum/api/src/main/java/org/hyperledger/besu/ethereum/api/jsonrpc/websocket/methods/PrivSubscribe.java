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

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.Quantity;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.InvalidSubscriptionRequestException;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.PrivateSubscribeRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;
import org.idnecology.idn.ethereum.privacy.MultiTenancyPrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

public class PrivSubscribe extends AbstractPrivateSubscriptionMethod {

  public PrivSubscribe(
      final SubscriptionManager subscriptionManager,
      final SubscriptionRequestMapper mapper,
      final PrivacyController privacyController,
      final PrivacyIdProvider privacyIdProvider) {
    super(subscriptionManager, mapper, privacyController, privacyIdProvider);
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_SUBSCRIBE.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    try {
      final String privacyUserId = privacyIdProvider.getPrivacyUserId(requestContext.getUser());
      final PrivateSubscribeRequest subscribeRequest =
          getMapper().mapPrivateSubscribeRequest(requestContext, privacyUserId);
      if (privacyController instanceof MultiTenancyPrivacyController) {
        checkIfPrivacyGroupMatchesAuthenticatedPrivacyUserId(
            requestContext, subscribeRequest.getPrivacyGroupId());
      }

      final Long subscriptionId = subscriptionManager().subscribe(subscribeRequest);

      return new JsonRpcSuccessResponse(
          requestContext.getRequest().getId(), Quantity.create(subscriptionId));
    } catch (final InvalidSubscriptionRequestException isEx) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVALID_REQUEST);
    }
  }
}
