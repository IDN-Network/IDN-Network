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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivGetFilterLogs;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.LogsResult;
import org.idnecology.idn.ethereum.core.LogWithMetadata;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrivGetFilterLogsTest {

  private final String FILTER_ID = "0xdbdb02abb65a2ba57a1cc0336c17ef75";
  private final String PRIVACY_GROUP_ID = "B1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";

  @Mock private FilterManager filterManager;
  @Mock private PrivacyController privacyController;
  @Mock private PrivacyIdProvider privacyIdProvider;

  private PrivGetFilterLogs method;

  @BeforeEach
  public void before() {
    method = new PrivGetFilterLogs(filterManager, privacyController, privacyIdProvider);
  }

  @Test
  public void getMethodReturnsCorrectMethodName() {
    assertThat(method.getName()).isEqualTo("priv_getFilterLogs");
  }

  @Test
  public void privacyGroupIdIsRequired() {
    final JsonRpcRequestContext request = privGetFilterLogsRequest(null, "0x1");

    assertThatThrownBy(() -> method.response(request))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Invalid privacy group ID parameter (index 0)");
  }

  @Test
  public void filterIdIsRequired() {
    final JsonRpcRequestContext request = privGetFilterLogsRequest(PRIVACY_GROUP_ID, null);

    assertThatThrownBy(() -> method.response(request))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Invalid filter ID parameter (index 1)");
  }

  @Test
  public void correctFilterIsQueried() {
    final JsonRpcRequestContext request = privGetFilterLogsRequest(PRIVACY_GROUP_ID, FILTER_ID);
    method.response(request);

    verify(filterManager).logs(eq(FILTER_ID));
  }

  @Test
  public void returnExpectedLogs() {
    final LogWithMetadata logWithMetadata = logWithMetadata();
    when(filterManager.logs(eq(FILTER_ID))).thenReturn(List.of(logWithMetadata));

    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, new LogsResult(List.of(logWithMetadata)));

    final JsonRpcRequestContext request = privGetFilterLogsRequest(PRIVACY_GROUP_ID, FILTER_ID);
    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  @Test
  public void returnEmptyListWhenLogsReturnEmpty() {
    when(filterManager.logs(eq(FILTER_ID))).thenReturn(Collections.emptyList());

    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, new LogsResult(Collections.emptyList()));

    final JsonRpcRequestContext request = privGetFilterLogsRequest(PRIVACY_GROUP_ID, FILTER_ID);
    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  @Test
  public void returnFilterNotFoundWhenLogsReturnIsNull() {
    when(filterManager.logs(eq(FILTER_ID))).thenReturn(null);

    final JsonRpcResponse expectedResponse =
        new JsonRpcErrorResponse(null, RpcErrorType.LOGS_FILTER_NOT_FOUND);

    final JsonRpcRequestContext request = privGetFilterLogsRequest(PRIVACY_GROUP_ID, FILTER_ID);
    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualTo(expectedResponse);
  }

  private JsonRpcRequestContext privGetFilterLogsRequest(
      final String privacyGroupId, final String filterId) {
    return new JsonRpcRequestContext(
        new JsonRpcRequest("2.0", "priv_getFilterLogs", new Object[] {privacyGroupId, filterId}));
  }

  private LogWithMetadata logWithMetadata() {
    return new LogWithMetadata(
        0,
        100L,
        Hash.ZERO,
        Hash.ZERO,
        0,
        Address.fromHexString("0x0"),
        Bytes.EMPTY,
        Lists.newArrayList(),
        false);
  }
}
