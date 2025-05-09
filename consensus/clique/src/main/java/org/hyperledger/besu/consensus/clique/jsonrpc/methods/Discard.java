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

import static com.google.common.base.Preconditions.checkState;

import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;

/** The Discard Json RPC method. */
public class Discard implements JsonRpcMethod {
  private final ValidatorProvider validatorProvider;

  /**
   * Instantiates a new Discard.
   *
   * @param validatorProvider the validator provider
   */
  public Discard(final ValidatorProvider validatorProvider) {
    this.validatorProvider = validatorProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.CLIQUE_DISCARD.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    checkState(
        validatorProvider.getVoteProviderAtHead().isPresent(), "Clique requires a vote provider");
    final Address address;
    try {
      address = requestContext.getRequiredParameter(0, Address.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid address parameter (index 0)", RpcErrorType.INVALID_ADDRESS_PARAMS, e);
    }
    validatorProvider.getVoteProviderAtHead().get().discardVote(address);
    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), true);
  }
}
