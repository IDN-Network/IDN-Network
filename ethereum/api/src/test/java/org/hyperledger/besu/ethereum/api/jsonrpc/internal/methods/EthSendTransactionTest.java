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

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EthSendTransactionTest {

  private JsonRpcMethod method;

  @BeforeEach
  public void before() {
    method = new EthSendTransaction();
  }

  @Test
  public void methodReturnExpectedName() {
    assertThat(method.getName()).isEqualTo("eth_sendTransaction");
  }

  @Test
  public void requestIsMissingParameter() {
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("2.0", "eth_sendTransaction", new String[] {}));

    final JsonRpcResponse expectedResponse =
        new JsonRpcErrorResponse(
            request.getRequest().getId(), RpcErrorType.ETH_SEND_TX_NOT_AVAILABLE);

    final JsonRpcResponse actualResponse = method.response(request);

    assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
  }
}
