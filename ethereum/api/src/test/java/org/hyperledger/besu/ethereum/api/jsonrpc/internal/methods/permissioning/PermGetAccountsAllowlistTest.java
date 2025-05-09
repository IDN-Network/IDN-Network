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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.permissioning.AccountLocalConfigPermissioningController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PermGetAccountsAllowlistTest {

  private static final JsonRpcRequestContext request =
      new JsonRpcRequestContext(new JsonRpcRequest("2.0", "perm_getAccountsAllowlist", null));

  @Mock private AccountLocalConfigPermissioningController accountAllowlist;
  private PermGetAccountsAllowlist method;

  @BeforeEach
  public void before() {
    method = new PermGetAccountsAllowlist(java.util.Optional.of(accountAllowlist));
  }

  @Test
  public void getNameShouldReturnExpectedName() {
    assertThat(method.getName()).isEqualTo("perm_getAccountsAllowlist");
  }

  @Test
  public void shouldReturnExpectedListOfAccountsWhenAllowlistHasBeenSet() {
    List<String> accountsList = Arrays.asList("0x0", "0x1");
    when(accountAllowlist.getAccountAllowlist()).thenReturn(accountsList);
    JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, accountsList);

    JsonRpcResponse actualResponse = method.response(request);

    assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  @Test
  public void shouldReturnEmptyListOfAccountsWhenAllowlistHasBeenSetAndIsEmpty() {
    List<String> emptyAccountsList = new ArrayList<>();
    when(accountAllowlist.getAccountAllowlist()).thenReturn(emptyAccountsList);
    JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, emptyAccountsList);

    JsonRpcResponse actualResponse = method.response(request);

    assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
  }
}
