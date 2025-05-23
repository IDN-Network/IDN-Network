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
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;

import java.util.HashMap;
import java.util.Map;

public class WebSocketMethodsFactory {

  private final Map<String, JsonRpcMethod> methods = new HashMap<>();

  public WebSocketMethodsFactory(
      final SubscriptionManager subscriptionManager,
      final Map<String, JsonRpcMethod> jsonRpcMethods) {
    this.methods.putAll(jsonRpcMethods);
    buildWebsocketMethods(subscriptionManager);
  }

  private void buildWebsocketMethods(final SubscriptionManager subscriptionManager) {
    addMethods(
        new EthSubscribe(subscriptionManager, new SubscriptionRequestMapper()),
        new EthUnsubscribe(subscriptionManager, new SubscriptionRequestMapper()));
  }

  public Map<String, JsonRpcMethod> methods() {
    return methods;
  }

  public void addMethods(final JsonRpcMethod... rpcMethods) {
    for (final JsonRpcMethod rpcMethod : rpcMethods) {
      methods.put(rpcMethod.getName(), rpcMethod);
    }
  }
}
