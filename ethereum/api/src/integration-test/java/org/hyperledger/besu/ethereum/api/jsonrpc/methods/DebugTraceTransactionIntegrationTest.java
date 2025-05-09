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

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.BlockchainImporter;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcTestMethodsFactory;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionDetails;
import org.idnecology.idn.plugin.services.rpc.RpcResponseType;
import org.idnecology.idn.testutil.BlockTestUtil;

import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DebugTraceTransactionIntegrationTest {
  private static final String DEBUG_TRACE_TRANSACTION = "debug_traceTransaction";
  private static JsonRpcTestMethodsFactory blockchain;
  private JsonRpcMethod method;

  @BeforeAll
  public static void setUpOnce() throws Exception {
    final String genesisJson =
        Resources.toString(BlockTestUtil.getTestGenesisUrl(), Charsets.UTF_8);

    blockchain =
        new JsonRpcTestMethodsFactory(
            new BlockchainImporter(BlockTestUtil.getTestBlockchainUrl(), genesisJson));
  }

  @BeforeEach
  public void setUp() {
    final Map<String, JsonRpcMethod> methods = blockchain.methods();
    method = methods.get(DEBUG_TRACE_TRANSACTION);
  }

  @Test
  public void debugTraceTransactionSuccessTest() {
    final Map<String, Boolean> map = Map.of("disableStorage", true);
    final Hash trxHash =
        Hash.fromHexString("0xcef53f2311d7c80e9086d661e69ac11a5f3d081e28e02a9ba9b66749407ac310");
    final Object[] params = new Object[] {trxHash, map};
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", DEBUG_TRACE_TRANSACTION, params));

    final JsonRpcResponse response = method.response(request);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    DebugTraceTransactionDetails debugTraceTransactionDetails =
        (DebugTraceTransactionDetails) ((JsonRpcSuccessResponse) response).getResult();
    assertThat(debugTraceTransactionDetails.getGas()).isEqualTo(23705L);
    assertThat(debugTraceTransactionDetails.getReturnValue()).isEmpty();
    assertThat(debugTraceTransactionDetails.failed()).isFalse();
    assertThat(debugTraceTransactionDetails.getStructLogs()).hasSize(106);
  }

  @Test
  public void debugTraceTransactionMissingTest() {
    final Map<String, Boolean> map = Map.of("disableStorage", true);
    final Object[] params =
        new Object[] {
          Hash.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"),
          map
        };
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", DEBUG_TRACE_TRANSACTION, params));
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, null);

    final JsonRpcResponse response = method.response(request);
    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }
}
