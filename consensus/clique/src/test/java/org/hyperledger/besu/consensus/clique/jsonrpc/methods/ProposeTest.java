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
package org.idnecology.idn.consensus.clique.jsonrpc.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.VoteType;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.plugin.services.rpc.RpcResponseType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProposeTest {
  private final String JSON_RPC_VERSION = "2.0";
  private final String METHOD = "clique_propose";
  private ValidatorProvider validatorProvider;

  @BeforeEach
  public void setup() {
    final Blockchain blockchain = mock(Blockchain.class);
    final EpochManager epochManager = mock(EpochManager.class);
    final BlockInterface blockInterface = mock(BlockInterface.class);
    validatorProvider =
        BlockValidatorProvider.nonForkingValidatorProvider(
            blockchain, epochManager, blockInterface);
  }

  @Test
  public void testAuth() {
    final Propose propose = new Propose(validatorProvider);
    final Address a1 = Address.fromHexString("1");

    final JsonRpcResponse response = propose.response(requestWithParams(a1, true));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a1))
        .isEqualTo(VoteType.ADD);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void testAuthWithAddressZeroResultsInError() {
    final Propose propose = new Propose(validatorProvider);
    final Address a0 = Address.fromHexString("0");

    final JsonRpcResponse response = propose.response(requestWithParams(a0, true));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0)).isNull();
    assertThat(response.getType()).isEqualTo(RpcResponseType.ERROR);
    final JsonRpcErrorResponse errorResponse = (JsonRpcErrorResponse) response;
    assertThat(errorResponse.getErrorType()).isEqualTo(RpcErrorType.INVALID_REQUEST);
  }

  @Test
  public void testDrop() {
    final Propose propose = new Propose(validatorProvider);
    final Address a1 = Address.fromHexString("1");

    final JsonRpcResponse response = propose.response(requestWithParams(a1, false));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a1))
        .isEqualTo(VoteType.DROP);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void testDropWithAddressZeroResultsInError() {
    final Propose propose = new Propose(validatorProvider);
    final Address a0 = Address.fromHexString("0");

    final JsonRpcResponse response = propose.response(requestWithParams(a0, false));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0)).isNull();
    assertThat(response.getType()).isEqualTo(RpcResponseType.ERROR);
    final JsonRpcErrorResponse errorResponse = (JsonRpcErrorResponse) response;
    assertThat(errorResponse.getErrorType()).isEqualTo(RpcErrorType.INVALID_REQUEST);
  }

  @Test
  public void testRepeatAuth() {
    final Propose propose = new Propose(validatorProvider);
    final Address a1 = Address.fromHexString("1");

    validatorProvider.getVoteProviderAtHead().get().authVote(a1);
    final JsonRpcResponse response = propose.response(requestWithParams(a1, true));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a1))
        .isEqualTo(VoteType.ADD);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void testRepeatDrop() {
    final Propose propose = new Propose(validatorProvider);
    final Address a1 = Address.fromHexString("1");

    validatorProvider.getVoteProviderAtHead().get().dropVote(a1);
    final JsonRpcResponse response = propose.response(requestWithParams(a1, false));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a1))
        .isEqualTo(VoteType.DROP);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void testChangeToAuth() {
    final Propose propose = new Propose(validatorProvider);
    final Address a1 = Address.fromHexString("1");

    validatorProvider.getVoteProviderAtHead().get().dropVote(a1);
    final JsonRpcResponse response = propose.response(requestWithParams(a1, true));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a1))
        .isEqualTo(VoteType.ADD);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  @Test
  public void testChangeToDrop() {
    final Propose propose = new Propose(validatorProvider);
    final Address a0 = Address.fromHexString("1");

    validatorProvider.getVoteProviderAtHead().get().authVote(a0);
    final JsonRpcResponse response = propose.response(requestWithParams(a0, false));

    assertThat(validatorProvider.getVoteProviderAtHead().get().getProposals().get(a0))
        .isEqualTo(VoteType.DROP);
    assertThat(response.getType()).isEqualTo(RpcResponseType.SUCCESS);
    final JsonRpcSuccessResponse successResponse = (JsonRpcSuccessResponse) response;
    assertThat(successResponse.getResult()).isEqualTo(true);
  }

  private JsonRpcRequestContext requestWithParams(final Object... params) {
    return new JsonRpcRequestContext(new JsonRpcRequest(JSON_RPC_VERSION, METHOD, params));
  }
}
