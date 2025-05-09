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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PrivGetCodeTest {

  @Mock private PrivacyController privacyController;
  @Mock private BlockchainQueries mockBlockchainQueries;
  @Mock private PrivacyIdProvider privacyIdProvider;

  private final Hash latestBlockHash = Hash.ZERO;
  private final String enclavePublicKey = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private final String privacyGroupId = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private final Address contractAddress =
      Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732");
  private final Bytes contractCode = Bytes.fromBase64String("ZXhhbXBsZQ==");

  private PrivGetCode method;
  private JsonRpcRequestContext privGetCodeRequest;

  @BeforeEach
  public void before() {
    when(privacyIdProvider.getPrivacyUserId(any())).thenReturn(enclavePublicKey);

    method = new PrivGetCode(mockBlockchainQueries, privacyController, privacyIdProvider);
    privGetCodeRequest = buildPrivGetCodeRequest();
  }

  @Test
  public void methodHasExpectedName() {
    assertThat(method.getName()).isEqualTo("priv_getCode");
  }

  @Test
  public void returnValidCodeWhenCalledOnValidContract() {
    when(mockBlockchainQueries.getBlockHashByNumber(anyLong()))
        .thenReturn(Optional.of(latestBlockHash));
    when(privacyController.getContractCode(
            eq(privacyGroupId), eq(contractAddress), eq(latestBlockHash), anyString()))
        .thenReturn(Optional.of(contractCode));

    final JsonRpcResponse response = method.response(privGetCodeRequest);

    assertThat(response).isInstanceOf(JsonRpcSuccessResponse.class);
    assertThat(((JsonRpcSuccessResponse) response).getResult())
        .isEqualTo(contractCode.toHexString());
  }

  @Test
  public void returnNullWhenContractDoesNotExist() {
    when(mockBlockchainQueries.getBlockHashByNumber(anyLong()))
        .thenReturn(Optional.of(latestBlockHash));
    when(privacyController.getContractCode(
            eq(privacyGroupId), eq(contractAddress), eq(latestBlockHash), anyString()))
        .thenReturn(Optional.empty());

    final JsonRpcResponse response = method.response(privGetCodeRequest);

    assertThat(response).isInstanceOf(JsonRpcSuccessResponse.class);
    assertThat(((JsonRpcSuccessResponse) response).getResult()).isNull();
  }

  @Test
  public void returnNullWhenBlockDoesNotExist() {
    when(mockBlockchainQueries.getBlockHashByNumber(anyLong())).thenReturn(Optional.empty());

    final JsonRpcResponse response = method.response(privGetCodeRequest);

    verifyNoInteractions(privacyController);

    assertThat(response).isInstanceOf(JsonRpcSuccessResponse.class);
    assertThat(((JsonRpcSuccessResponse) response).getResult()).isNull();
  }

  private JsonRpcRequestContext buildPrivGetCodeRequest() {
    return new JsonRpcRequestContext(
        new JsonRpcRequest(
            "2.0",
            "priv_getCode",
            new Object[] {privacyGroupId, contractAddress.toHexString(), "latest"}));
  }
}
