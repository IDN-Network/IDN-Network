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
package org.idnecology.idn.consensus.qbft.jsonrpc.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class QbftGetValidatorsByBlockNumberTest {

  @Mock private BlockchainQueries blockchainQueries;
  @Mock private BlockHeader blockHeader;
  @Mock private JsonRpcRequestContext request;
  @Mock private ValidatorProvider validatorProvider;

  private QbftGetValidatorsByBlockNumber method;

  @BeforeEach
  public void setUp() {
    method = new QbftGetValidatorsByBlockNumber(blockchainQueries, validatorProvider);
  }

  @Test
  public void blockParameterIsParameter0() {
    request = new JsonRpcRequestContext(new JsonRpcRequest("?", "ignore", new String[] {"0x1245"}));
    BlockParameter blockParameter = method.blockParameter(request);
    assertThat(blockParameter.getNumber()).isPresent();
    assertThat(blockParameter.getNumber().get()).isEqualTo(0x1245);
  }

  @Test
  public void nameShouldBeCorrect() {
    assertThat(method.getName()).isEqualTo("qbft_getValidatorsByBlockNumber");
  }

  @Test
  public void shouldReturnListOfValidatorsFromBlock() {
    when(blockchainQueries.getBlockHeaderByNumber(12)).thenReturn(Optional.of(blockHeader));
    final List<Address> addresses = Collections.singletonList(Address.ID);
    final List<String> expectedOutput = Collections.singletonList(Address.ID.toString());
    when(validatorProvider.getValidatorsForBlock(any())).thenReturn(addresses);
    Object result = method.resultByBlockNumber(request, 12);
    assertThat(result).isEqualTo(expectedOutput);
  }

  @Test
  public void shouldReturnListOfValidatorsFromLatestBlock() {
    request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("2.0", "qbft_getValidatorsByBlockNumber", new String[] {"latest"}));
    when(blockchainQueries.headBlockNumber()).thenReturn(12L);
    when(blockchainQueries.getBlockHeaderByNumber(12)).thenReturn(Optional.of(blockHeader));
    final List<Address> addresses = Collections.singletonList(Address.ID);
    final List<String> expectedOutput = Collections.singletonList(Address.ID.toString());
    when(validatorProvider.getValidatorsForBlock(any())).thenReturn(addresses);
    Object result = method.response(request);
    assertThat(result).isInstanceOf(JsonRpcSuccessResponse.class);
    assertThat(((JsonRpcSuccessResponse) result).getResult()).isEqualTo(expectedOutput);
  }

  @Test
  public void shouldReturnListOfValidatorsFromPendingBlock() {
    request =
        new JsonRpcRequestContext(
            new JsonRpcRequest("2.0", "qbft_getValidatorsByBlockNumber", new String[] {"pending"}));
    when(blockchainQueries.headBlockHeader()).thenReturn(blockHeader);
    final List<Address> addresses = Collections.singletonList(Address.ID);
    final List<String> expectedOutput = Collections.singletonList(Address.ID.toString());
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(addresses);
    Object result = method.response(request);
    assertThat(result).isInstanceOf(JsonRpcSuccessResponse.class);
    assertThat(((JsonRpcSuccessResponse) result).getResult()).isEqualTo(expectedOutput);
  }
}
