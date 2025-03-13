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
package org.idnecology.idn.tests.acceptance.plugins;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.ServiceManager;
import org.idnecology.idn.plugin.data.BlockContext;
import org.idnecology.idn.plugin.services.BlockchainService;
import org.idnecology.idn.plugin.services.RpcEndpointService;
import org.idnecology.idn.plugin.services.exception.PluginRpcEndpointException;
import org.idnecology.idn.plugin.services.rpc.PluginRpcRequest;

import java.util.Optional;

import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(IdnPlugin.class)
public class TestBlockchainServiceFinalizedPlugin implements IdnPlugin {
  private static final Logger LOG =
      LoggerFactory.getLogger(TestBlockchainServiceFinalizedPlugin.class);
  private static final String RPC_NAMESPACE = "updater";
  private static final String RPC_METHOD_FINALIZED_BLOCK = "updateFinalizedBlockV1";
  private static final String RPC_METHOD_SAFE_BLOCK = "updateSafeBlockV1";

  @Override
  public void register(final ServiceManager serviceManager) {
    LOG.trace("Registering plugin ...");

    final RpcEndpointService rpcEndpointService =
        serviceManager
            .getService(RpcEndpointService.class)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Failed to obtain RpcEndpointService from the IdnContext."));

    final BlockchainService blockchainService =
        serviceManager
            .getService(BlockchainService.class)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Failed to obtain BlockchainService from the IdnContext."));

    final FinalizationUpdaterRpcMethod rpcMethod =
        new FinalizationUpdaterRpcMethod(blockchainService);
    rpcEndpointService.registerRPCEndpoint(
        RPC_NAMESPACE, RPC_METHOD_FINALIZED_BLOCK, rpcMethod::setFinalizedBlock);
    rpcEndpointService.registerRPCEndpoint(
        RPC_NAMESPACE, RPC_METHOD_SAFE_BLOCK, rpcMethod::setSafeBlock);
  }

  @Override
  public void start() {
    LOG.trace("Starting plugin ...");
  }

  @Override
  public void stop() {
    LOG.trace("Stopping plugin ...");
  }

  static class FinalizationUpdaterRpcMethod {
    private final BlockchainService blockchainService;
    private final JsonRpcParameter parameterParser = new JsonRpcParameter();

    FinalizationUpdaterRpcMethod(final BlockchainService blockchainService) {
      this.blockchainService = blockchainService;
    }

    Boolean setFinalizedBlock(final PluginRpcRequest request) {
      return setFinalizedOrSafeBlock(request, true);
    }

    Boolean setSafeBlock(final PluginRpcRequest request) {
      return setFinalizedOrSafeBlock(request, false);
    }

    private Boolean setFinalizedOrSafeBlock(
        final PluginRpcRequest request, final boolean isFinalized) {
      final Long blockNumberToSet = parseResult(request);

      // lookup finalized block by number in local chain
      final Optional<BlockContext> finalizedBlock =
          blockchainService.getBlockByNumber(blockNumberToSet);
      if (finalizedBlock.isEmpty()) {
        throw new PluginRpcEndpointException(
            RpcErrorType.BLOCK_NOT_FOUND,
            "Block not found in the local chain: " + blockNumberToSet);
      }

      try {
        final Hash blockHash = finalizedBlock.get().getBlockHeader().getBlockHash();
        if (isFinalized) {
          blockchainService.setFinalizedBlock(blockHash);
        } else {
          blockchainService.setSafeBlock(blockHash);
        }
      } catch (final IllegalArgumentException e) {
        throw new PluginRpcEndpointException(
            RpcErrorType.BLOCK_NOT_FOUND,
            "Block not found in the local chain: " + blockNumberToSet);
      } catch (final UnsupportedOperationException e) {
        throw new PluginRpcEndpointException(
            RpcErrorType.METHOD_NOT_ENABLED,
            "Method not enabled for PoS network: setFinalizedBlock");
      } catch (final Exception e) {
        throw new PluginRpcEndpointException(
            RpcErrorType.INTERNAL_ERROR, "Error setting finalized block: " + blockNumberToSet);
      }

      return Boolean.TRUE;
    }

    private Long parseResult(final PluginRpcRequest request) {
      Long blockNumber;
      try {
        final Object[] params = request.getParams();
        blockNumber = parameterParser.required(params, 0, Long.class);
      } catch (final Exception e) {
        throw new PluginRpcEndpointException(RpcErrorType.INVALID_PARAMS, e.getMessage());
      }

      if (blockNumber <= 0) {
        throw new PluginRpcEndpointException(
            RpcErrorType.INVALID_PARAMS, "Block number must be greater than 0");
      }

      return blockNumber;
    }
  }
}
