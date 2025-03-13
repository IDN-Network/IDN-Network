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
package org.idnecology.idn.ethereum.api.handlers;

import org.idnecology.idn.ethereum.api.jsonrpc.authentication.AuthenticationService;
import org.idnecology.idn.ethereum.api.jsonrpc.authentication.AuthenticationUtils;
import org.idnecology.idn.ethereum.api.jsonrpc.context.ContextKey;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;

import java.util.Collection;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class AuthenticationHandler {

  private AuthenticationHandler() {}

  public static Handler<RoutingContext> handler(
      final AuthenticationService authenticationService, final Collection<String> noAuthRpcApis) {
    return ctx -> {
      // first check token if authentication is required
      final String token = getAuthToken(ctx);
      if (token == null && noAuthRpcApis.isEmpty()) {
        // no auth token when auth required
        handleJsonRpcUnauthorizedError(ctx);
      } else {
        authenticationService.authenticate(
            token, user -> ctx.put(ContextKey.AUTHENTICATED_USER.name(), user));
        ctx.next();
      }
    };
  }

  private static String getAuthToken(final RoutingContext routingContext) {
    return AuthenticationUtils.getJwtTokenFromAuthorizationHeaderValue(
        routingContext.request().getHeader("Authorization"));
  }

  private static void handleJsonRpcUnauthorizedError(final RoutingContext routingContext) {
    final HttpServerResponse response = routingContext.response();
    if (!response.closed()) {
      response
          .setStatusCode(HttpResponseStatus.UNAUTHORIZED.code())
          .end(Json.encode(new JsonRpcErrorResponse(null, RpcErrorType.UNAUTHORIZED)));
    }
  }
}
