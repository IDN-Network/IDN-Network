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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EthGetBlockByHashTest {

  @Mock private BlockchainQueries blockchainQueries;
  private final BlockResultFactory blockResult = new BlockResultFactory();
  private EthGetBlockByHash method;
  private final String JSON_RPC_VERSION = "2.0";
  private final String ETH_METHOD = "eth_getBlockByHash";
  private final String ZERO_HASH = String.valueOf(Hash.ZERO);

  @BeforeEach
  public void setUp() {
    method = new EthGetBlockByHash(blockchainQueries, blockResult);
  }

  @Test
  public void returnsCorrectMethodName() {
    assertThat(method.getName()).isEqualTo(ETH_METHOD);
  }

  @Test
  public void exceptionWhenNoParamsSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams()))
        .isInstanceOf(InvalidJsonRpcParameters.class);
    verifyNoMoreInteractions(blockchainQueries);
  }

  @Test
  public void exceptionWhenNoHashSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams("false")))
        .isInstanceOf(InvalidJsonRpcParameters.class);
    verifyNoMoreInteractions(blockchainQueries);
  }

  @Test
  public void exceptionWhenNoBoolSupplied() {
    assertThatThrownBy(() -> method.response(requestWithParams(ZERO_HASH)))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid return complete transaction parameter (index 1)");
    verifyNoMoreInteractions(blockchainQueries);
  }

  @Test
  public void exceptionWhenHashParamInvalid() {
    assertThatThrownBy(() -> method.response(requestWithParams("hash", "true")))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid block hash parameter (index 0)");
    verifyNoMoreInteractions(blockchainQueries);
  }

  @Test
  public void exceptionWhenBoolParamInvalid() {
    assertThatThrownBy(() -> method.response(requestWithParams(ZERO_HASH, "maybe")))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid return complete transaction parameter (index 1)");
    verifyNoMoreInteractions(blockchainQueries);
  }

  private JsonRpcRequestContext requestWithParams(final Object... params) {
    return new JsonRpcRequestContext(new JsonRpcRequest(JSON_RPC_VERSION, ETH_METHOD, params));
  }
}
