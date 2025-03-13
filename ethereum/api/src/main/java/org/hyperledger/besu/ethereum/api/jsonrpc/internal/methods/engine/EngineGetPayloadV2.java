/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine;

import static org.idnecology.idn.datatypes.HardforkId.MainnetHardforkId.CANCUN;

import org.idnecology.idn.consensus.merge.PayloadWrapper;
import org.idnecology.idn.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;

import java.util.Optional;

import io.vertx.core.Vertx;

public class EngineGetPayloadV2 extends AbstractEngineGetPayload {

  private final Optional<Long> cancunMilestone;

  public EngineGetPayloadV2(
      final Vertx vertx,
      final ProtocolContext protocolContext,
      final MergeMiningCoordinator mergeMiningCoordinator,
      final BlockResultFactory blockResultFactory,
      final EngineCallListener engineCallListener,
      final ProtocolSchedule schedule) {
    super(
        vertx,
        schedule,
        protocolContext,
        mergeMiningCoordinator,
        blockResultFactory,
        engineCallListener);
    cancunMilestone = schedule.milestoneFor(CANCUN);
  }

  @Override
  public String getName() {
    return RpcMethod.ENGINE_GET_PAYLOAD_V2.getMethodName();
  }

  @Override
  protected JsonRpcResponse createResponse(
      final JsonRpcRequestContext request, final PayloadWrapper payload) {
    final var result = blockResultFactory.payloadTransactionCompleteV2(payload);
    logProposal(payload);
    return new JsonRpcSuccessResponse(request.getRequest().getId(), result);
  }

  @Override
  protected ValidationResult<RpcErrorType> validateForkSupported(final long blockTimestamp) {
    if (cancunMilestone.isPresent() && blockTimestamp >= cancunMilestone.get()) {
      return ValidationResult.invalid(RpcErrorType.UNSUPPORTED_FORK);
    }

    return ValidationResult.valid();
  }
}
