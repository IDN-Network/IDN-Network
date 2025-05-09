/*
 * Copyright contributors to Idn ecology Idn.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal;

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.INTERNAL_ERROR;
import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.UNKNOWN_BLOCK;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.AbstractBlockParameterMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugReplayBlock extends AbstractBlockParameterMethod {
  private static final Logger LOG = LoggerFactory.getLogger(DebugReplayBlock.class);

  private final ProtocolContext protocolContext;
  private final ProtocolSchedule protocolSchedule;

  public DebugReplayBlock(
      final BlockchainQueries blockchain,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule) {
    super(blockchain);

    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_REPLAY_BLOCK.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    try {
      return request.getRequiredParameter(0, BlockParameter.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid block parameter (index 0)", RpcErrorType.INVALID_BLOCK_PARAMS, e);
    }
  }

  @Override
  protected Object resultByBlockNumber(
      final JsonRpcRequestContext request, final long blockNumber) {
    final Optional<Block> maybeBlock =
        getBlockchainQueries().getBlockchain().getBlockByNumber(blockNumber);

    if (maybeBlock.isEmpty()) {
      return new JsonRpcErrorResponse(request.getRequest().getId(), UNKNOWN_BLOCK);
    }

    final Block block = maybeBlock.get();

    // rewind to the block before the one we want to replay
    protocolContext.getBlockchain().rewindToBlock(blockNumber - 1);

    try {
      // replay block and persist it
      protocolSchedule
          .getByBlockHeader(block.getHeader())
          .getBlockValidator()
          .validateAndProcessBlock(
              protocolContext, block, HeaderValidationMode.FULL, HeaderValidationMode.NONE, true);
    } catch (Exception e) {
      LOG.error(e.getMessage());
      return new JsonRpcErrorResponse(request.getRequest().getId(), INTERNAL_ERROR);
    }

    // set head to newly imported block
    protocolContext.getBlockchain().forwardToBlock(maybeBlock.get().getHeader());

    return JsonRpcSuccessResponse.SUCCESS_RESULT;
  }
}
