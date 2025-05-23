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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.plugin.IdnPlugin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginsReloadConfiguration implements JsonRpcMethod {

  private static final Logger LOG = LoggerFactory.getLogger(PluginsReloadConfiguration.class);
  private final Map<String, IdnPlugin> namedPlugins;

  public PluginsReloadConfiguration(final Map<String, IdnPlugin> namedPlugins) {
    this.namedPlugins = namedPlugins;
  }

  @Override
  public String getName() {
    return RpcMethod.PLUGINS_RELOAD_CONFIG.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    try {
      final String pluginName = requestContext.getRequiredParameter(0, String.class);
      if (!namedPlugins.containsKey(pluginName)) {
        LOG.error(
            "Plugin cannot be reloaded because no plugin has been registered with specified name: {}.",
            pluginName);
        return new JsonRpcErrorResponse(
            requestContext.getRequest().getId(), RpcErrorType.PLUGIN_NOT_FOUND);
      }
      reloadPluginConfig(namedPlugins.get(pluginName));
      return new JsonRpcSuccessResponse(requestContext.getRequest().getId());
    } catch (JsonRpcParameterException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.INVAlID_PLUGIN_NAME_PARAMS);
    }
  }

  private void reloadPluginConfig(final IdnPlugin plugin) {
    final String name = plugin.getName().orElseThrow();
    LOG.info("Reloading plugin configuration: {}.", name);
    final CompletableFuture<Void> future = plugin.reloadConfiguration();
    future.thenAcceptAsync(aVoid -> LOG.info("Plugin {} has been reloaded.", name));
  }
}
