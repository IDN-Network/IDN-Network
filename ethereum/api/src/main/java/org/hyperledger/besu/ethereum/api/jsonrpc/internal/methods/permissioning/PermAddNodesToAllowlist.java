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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.StringListParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.p2p.network.exceptions.P2PDisabledException;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController.NodesAllowlistResult;

import java.util.List;
import java.util.Optional;

public class PermAddNodesToAllowlist implements JsonRpcMethod {

  private final Optional<NodeLocalConfigPermissioningController>
      nodeAllowlistPermissioningController;

  public PermAddNodesToAllowlist(
      final Optional<NodeLocalConfigPermissioningController> nodeAllowlistPermissioningController) {
    this.nodeAllowlistPermissioningController = nodeAllowlistPermissioningController;
  }

  @Override
  public String getName() {
    return RpcMethod.PERM_ADD_NODES_TO_ALLOWLIST.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final StringListParameter enodeListParam;
    try {
      enodeListParam = requestContext.getRequiredParameter(0, StringListParameter.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid enode list parameter (index 0)", RpcErrorType.INVALID_ENODE_PARAMS, e);
    }

    try {
      if (nodeAllowlistPermissioningController.isPresent()) {
        try {
          final List<String> enodeURLs = enodeListParam.getStringList();
          final NodesAllowlistResult nodesAllowlistResult =
              nodeAllowlistPermissioningController.get().addNodes(enodeURLs);

          switch (nodesAllowlistResult.result()) {
            case SUCCESS:
              return new JsonRpcSuccessResponse(requestContext.getRequest().getId());
            case ERROR_EMPTY_ENTRY:
              return new JsonRpcErrorResponse(
                  requestContext.getRequest().getId(), RpcErrorType.NODE_ALLOWLIST_EMPTY_ENTRY);
            case ERROR_EXISTING_ENTRY:
              return new JsonRpcErrorResponse(
                  requestContext.getRequest().getId(), RpcErrorType.NODE_ALLOWLIST_EXISTING_ENTRY);
            case ERROR_DUPLICATED_ENTRY:
              return new JsonRpcErrorResponse(
                  requestContext.getRequest().getId(),
                  RpcErrorType.NODE_ALLOWLIST_DUPLICATED_ENTRY);
            case ERROR_ALLOWLIST_PERSIST_FAIL:
              return new JsonRpcErrorResponse(
                  requestContext.getRequest().getId(), RpcErrorType.ALLOWLIST_PERSIST_FAILURE);
            case ERROR_ALLOWLIST_FILE_SYNC:
              return new JsonRpcErrorResponse(
                  requestContext.getRequest().getId(), RpcErrorType.ALLOWLIST_FILE_SYNC);
            default:
              throw new Exception();
          }
        } catch (IllegalArgumentException e) {
          return new JsonRpcErrorResponse(
              requestContext.getRequest().getId(), RpcErrorType.NODE_ALLOWLIST_INVALID_ENTRY);
        } catch (Exception e) {
          return new JsonRpcErrorResponse(
              requestContext.getRequest().getId(), RpcErrorType.INTERNAL_ERROR);
        }
      } else {
        return new JsonRpcErrorResponse(
            requestContext.getRequest().getId(), RpcErrorType.NODE_ALLOWLIST_NOT_ENABLED);
      }
    } catch (P2PDisabledException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), RpcErrorType.P2P_DISABLED);
    }
  }
}
