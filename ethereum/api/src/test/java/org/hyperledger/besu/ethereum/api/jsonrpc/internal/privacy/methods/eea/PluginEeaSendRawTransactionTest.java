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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.eea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PluginEeaSendRawTransactionTest extends BaseEeaSendRawTransaction {

  PluginEeaSendRawTransaction method;

  @BeforeEach
  public void before() {
    method =
        new PluginEeaSendRawTransaction(
            transactionPool,
            user -> "",
            privateMarkerTransactionFactory,
            address -> 0,
            privacyController,
            gasCalculator);
  }

  @Test
  public void validUnrestrictedTransactionIsSentToTransactionPool() {

    when(privacyController.createPrivateMarkerTransactionPayload(any(), any(), any()))
        .thenReturn(MOCK_ORION_KEY);
    when(privacyController.validatePrivateTransaction(any(), any()))
        .thenReturn(ValidationResult.valid());

    when(transactionPool.addTransactionViaApi(any())).thenReturn(ValidationResult.valid());

    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(
            validPrivacyGroupTransactionRequest.getRequest().getId(),
            "0x8d84a2d2158c9be3c18a8e6d064fd085e440d117294ba2e7f909e36bf192ffbd");

    final JsonRpcResponse actualResponse =
        method.response(validUnrestrictedPrivacyGroupTransactionRequest);

    assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    verify(transactionPool).addTransactionViaApi(PUBLIC_PLUGIN_TRANSACTION);
  }
}
