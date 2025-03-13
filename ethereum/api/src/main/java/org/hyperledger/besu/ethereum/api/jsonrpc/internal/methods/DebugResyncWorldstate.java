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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.Synchronizer;

public class DebugResyncWorldstate implements JsonRpcMethod {
  private final Synchronizer synchronizer;
  private final BadBlockManager badBlockManager;

  public DebugResyncWorldstate(
      final ProtocolContext protocolContext, final Synchronizer synchronizer) {
    this.synchronizer = synchronizer;
    this.badBlockManager = protocolContext.getBadBlockManager();
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_RESYNC_WORLDSTATE.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext request) {
    badBlockManager.reset();
    return new JsonRpcSuccessResponse(
        request.getRequest().getId(), synchronizer.resyncWorldState());
  }
}
