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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonCallParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.Quantity;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.privacy.RestrictedDefaultPrivacyController;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.ethereum.transaction.CallParameter;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrivCallTest {

  private PrivCall method;

  private static final String ENCLAVE_PUBLIC_KEY = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";

  @Mock private BlockchainQueries blockchainQueries;
  String privacyGroupId = "privacyGroupId";
  private final PrivacyIdProvider privacyIdProvider = (user) -> ENCLAVE_PUBLIC_KEY;
  private final PrivacyController privacyController =
      mock(RestrictedDefaultPrivacyController.class);

  @BeforeEach
  public void setUp() {
    method = new PrivCall(blockchainQueries, privacyController, privacyIdProvider);
  }

  @Test
  public void shouldReturnCorrectMethodName() {
    assertThat(method.getName()).isEqualTo("priv_call");
  }

  @Test
  public void shouldThrowInvalidJsonRpcParametersExceptionWhenMissingToField() {
    final JsonCallParameter callParameter =
        new JsonCallParameter.JsonCallParameterBuilder()
            .withFrom(Address.fromHexString("0x0"))
            .withGas(0L)
            .withGasPrice(Wei.ZERO)
            .withValue(Wei.ZERO)
            .withInput(Bytes.EMPTY)
            .build();
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter, "latest");

    final Throwable thrown = catchThrowable(() -> method.response(request));

    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasNoCause()
        .hasMessage("Missing \"to\" field in call arguments");
  }

  @Test
  public void shouldReturnNullWhenProcessorReturnsEmpty() {
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "latest");
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, null);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), anyLong());
  }

  @Test
  public void shouldAcceptRequestWhenMissingOptionalFields() {
    final JsonCallParameter callParameter =
        new JsonCallParameter.JsonCallParameterBuilder()
            .withTo(Address.fromHexString("0x0"))
            .build();
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter, "latest");
    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, Bytes.of().toString());

    mockTransactionProcessorSuccessResult(Bytes.of());

    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
    verify(privacyController)
        .simulatePrivateTransaction(any(), any(), eq(callParameter), anyLong());
  }

  @Test
  public void shouldReturnExecutionResultWhenExecutionIsSuccessful() {
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "latest");
    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, Bytes.of(1).toString());
    mockTransactionProcessorSuccessResult(Bytes.of(1));

    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
    verify(privacyController)
        .simulatePrivateTransaction(any(), any(), eq(callParameter()), anyLong());
  }

  @Test
  public void shouldUseCorrectBlockNumberWhenLatest() {
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "latest");
    when(blockchainQueries.headBlockNumber()).thenReturn(11L);

    method.response(request);

    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), eq(11L));
  }

  @Test
  public void shouldUseCorrectBlockNumberWhenEarliest() {
    final JsonRpcRequestContext request =
        ethCallRequest(privacyGroupId, callParameter(), "earliest");
    method.response(request);

    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), eq(0L));
  }

  @Test
  public void shouldUseCorrectBlockNumberWhenSpecified() {
    final JsonRpcRequestContext request =
        ethCallRequest(privacyGroupId, callParameter(), Quantity.create(13L));

    method.response(request);

    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), eq(13L));
  }

  @Test
  public void shouldThrowCorrectExceptionWhenNoPrivacyGroupSpecified() {
    final JsonRpcRequestContext request =
        ethCallRequest(null, callParameter(), Quantity.create(13L));
    final Throwable thrown = catchThrowable(() -> method.response(request));

    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid privacy group ID parameter (index 0)");
  }

  private JsonCallParameter callParameter() {
    return new JsonCallParameter.JsonCallParameterBuilder()
        .withFrom(Address.fromHexString("0x0"))
        .withTo(Address.fromHexString("0x0"))
        .withGas(0L)
        .withGasPrice(Wei.ZERO)
        .withValue(Wei.ZERO)
        .withInput(Bytes.EMPTY)
        .build();
  }

  private JsonRpcRequestContext ethCallRequest(
      final String privacyGroupId,
      final CallParameter callParameter,
      final String blockNumberInHex) {
    return new JsonRpcRequestContext(
        new JsonRpcRequest(
            "2.0", "priv_call", new Object[] {privacyGroupId, callParameter, blockNumberInHex}));
  }

  private void mockTransactionProcessorSuccessResult(final Bytes output) {
    final TransactionProcessingResult result = mock(TransactionProcessingResult.class);

    when(result.getValidationResult()).thenReturn(ValidationResult.valid());
    when(result.getOutput()).thenReturn(output);
    when(privacyController.simulatePrivateTransaction(any(), any(), any(), anyLong()))
        .thenReturn(Optional.of(result));
  }
}
