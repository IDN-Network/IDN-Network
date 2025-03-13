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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class QbftGetValidatorsByBlockHashTest {

  private static final String ETH_METHOD = "qbft_getValidatorsByBlockHash";
  private static final String JSON_RPC_VERSION = "2.0";
  private static final String ZERO_HASH = String.valueOf(Hash.ZERO);

  @Mock private Blockchain blockchain;
  @Mock private BlockHeader blockHeader;
  @Mock private JsonRpcRequestContext request;
  @Mock private ValidatorProvider validatorProvider;

  private QbftGetValidatorsByBlockHash method;

  @BeforeEach
  public void setUp() {
    method = new QbftGetValidatorsByBlockHash(blockchain, validatorProvider);
  }

  @Test
  public void nameShouldBeCorrect() {
    Assertions.assertThat(method.getName()).isEqualTo(ETH_METHOD);
  }

  @Test
  public void shouldReturnListOfValidatorsFromBlock() {
    when(blockchain.getBlockHeader(Hash.ZERO)).thenReturn(Optional.of(blockHeader));
    final List<Address> addresses = Collections.singletonList(Address.ID);
    final List<String> expectedOutput = Collections.singletonList(Address.ID.toString());
    when(validatorProvider.getValidatorsForBlock(any())).thenReturn(addresses);
    request = requestWithParams(ZERO_HASH);
    JsonRpcSuccessResponse response = (JsonRpcSuccessResponse) method.response(request);
    Assertions.assertThat(response.getResult()).isEqualTo(expectedOutput);
  }

  private JsonRpcRequestContext requestWithParams(final Object... params) {
    return new JsonRpcRequestContext(new JsonRpcRequest(JSON_RPC_VERSION, ETH_METHOD, params));
  }
}
