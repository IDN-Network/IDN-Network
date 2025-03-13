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

import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermAddAccountsToAllowlist;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermAddNodesToAllowlist;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermGetAccountsAllowlist;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermGetNodesAllowlist;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermReloadPermissionsFromFile;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermRemoveAccountsFromAllowlist;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.permissioning.PermRemoveNodesFromAllowlist;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.idnecology.idn.ethereum.permissioning.NodeLocalConfigPermissioningController;

import java.util.Map;
import java.util.Optional;

public class PermJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final Optional<AccountLocalConfigPermissioningController> accountsAllowlistController;
  private final Optional<NodeLocalConfigPermissioningController> nodeAllowlistController;

  public PermJsonRpcMethods(
      final Optional<AccountLocalConfigPermissioningController> accountsAllowlistController,
      final Optional<NodeLocalConfigPermissioningController> nodeAllowlistController) {
    this.accountsAllowlistController = accountsAllowlistController;
    this.nodeAllowlistController = nodeAllowlistController;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.PERM.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    return mapOf(
        new PermAddNodesToAllowlist(nodeAllowlistController),
        new PermRemoveNodesFromAllowlist(nodeAllowlistController),
        new PermGetNodesAllowlist(nodeAllowlistController),
        new PermGetAccountsAllowlist(accountsAllowlistController),
        new PermAddAccountsToAllowlist(accountsAllowlistController),
        new PermRemoveAccountsFromAllowlist(accountsAllowlistController),
        new PermReloadPermissionsFromFile(accountsAllowlistController, nodeAllowlistController));
  }
}
