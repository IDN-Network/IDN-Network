/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.execution;

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.INVALID_REQUEST;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestId;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcNoResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(JsonRpcExecutor.class);

  private final JsonRpcProcessor rpcProcessor;
  private final Map<String, JsonRpcMethod> rpcMethods;

  public JsonRpcExecutor(
      final JsonRpcProcessor rpcProcessor, final Map<String, JsonRpcMethod> rpcMethods) {
    this.rpcProcessor = rpcProcessor;
    this.rpcMethods = rpcMethods;
  }

  public JsonRpcResponse execute(
      final Optional<User> optionalUser,
      final Tracer tracer,
      final Context spanContext,
      final Supplier<Boolean> alive,
      final JsonObject jsonRpcRequest,
      final Function<JsonObject, JsonRpcRequest> requestBodyProvider) {
    try {
      final JsonRpcRequest requestBody = requestBodyProvider.apply(jsonRpcRequest);
      final JsonRpcRequestId id = new JsonRpcRequestId(requestBody.getId());
      // Handle notifications
      if (requestBody.isNotification()) {
        // Notifications aren't handled so create empty result for now.
        return new JsonRpcNoResponse();
      }
      final Span span;
      if (tracer != null) {
        span =
            tracer
                .spanBuilder(requestBody.getMethod())
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(spanContext)
                .startSpan();
      } else {
        span = Span.getInvalid();
      }
      final Optional<RpcErrorType> unavailableMethod = validateMethodAvailability(requestBody);
      if (unavailableMethod.isPresent()) {
        span.setStatus(StatusCode.ERROR, "method unavailable");
        return new JsonRpcErrorResponse(id, unavailableMethod.get());
      }

      final JsonRpcMethod method = rpcMethods.get(requestBody.getMethod());

      return rpcProcessor.process(
          id, method, span, new JsonRpcRequestContext(requestBody, optionalUser, alive));
    } catch (final IllegalArgumentException e) {
      try {
        final Integer id = jsonRpcRequest.getInteger("id", null);
        return new JsonRpcErrorResponse(id, INVALID_REQUEST);
      } catch (final ClassCastException idNotIntegerException) {
        return new JsonRpcErrorResponse(null, INVALID_REQUEST);
      }
    }
  }

  private Optional<RpcErrorType> validateMethodAvailability(final JsonRpcRequest request) {
    final String name = request.getMethod();

    if (LOG.isTraceEnabled()) {
      final JsonArray params = JsonObject.mapFrom(request).getJsonArray("params");
      LOG.trace("JSON-RPC request -> {} {}", name, params);
    }

    final JsonRpcMethod method = rpcMethods.get(name);

    if (method == null) {
      if (!RpcMethod.rpcMethodExists(name)) {
        return Optional.of(RpcErrorType.METHOD_NOT_FOUND);
      }
      if (!rpcMethods.containsKey(name)) {
        return Optional.of(RpcErrorType.METHOD_NOT_ENABLED);
      }
    }

    return Optional.empty();
  }
}
