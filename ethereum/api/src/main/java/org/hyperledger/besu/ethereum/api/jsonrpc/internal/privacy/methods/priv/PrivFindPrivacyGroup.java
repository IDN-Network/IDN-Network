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

import static org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType.FIND_PRIVACY_GROUP_ERROR;

import org.idnecology.idn.enclave.types.PrivacyGroup;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.privacy.MultiTenancyValidationException;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("MockNotUsedInProduction")
@Deprecated(since = "24.12.0")
public class PrivFindPrivacyGroup implements JsonRpcMethod {

  private static final Logger LOG = LoggerFactory.getLogger(PrivFindPrivacyGroup.class);
  private final PrivacyController privacyController;
  private final PrivacyIdProvider privacyIdProvider;

  public PrivFindPrivacyGroup(
      final PrivacyController privacyController, final PrivacyIdProvider privacyIdProvider) {
    this.privacyController = privacyController;
    this.privacyIdProvider = privacyIdProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_FIND_PRIVACY_GROUP.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    LOG.trace("Executing {}", RpcMethod.PRIV_FIND_PRIVACY_GROUP.getMethodName());

    final String[] addresses;
    try {
      addresses = requestContext.getRequiredParameter(0, String[].class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid address parameters (index 0)", RpcErrorType.INVALID_ADDRESS_PARAMS, e);
    }

    LOG.trace("Finding a privacy group with members {}", Arrays.toString(addresses));

    final List<PrivacyGroup> response;
    try {
      response =
          Arrays.asList(
              privacyController.findPrivacyGroupByMembers(
                  Arrays.asList(addresses),
                  privacyIdProvider.getPrivacyUserId(requestContext.getUser())));
    } catch (final MultiTenancyValidationException e) {
      LOG.error("Unauthorized privacy multi-tenancy rpc request. {}", e.getMessage());
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), FIND_PRIVACY_GROUP_ERROR);
    } catch (final Exception e) {
      LOG.error("Failed to fetch privacy group", e);
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), FIND_PRIVACY_GROUP_ERROR);
    }
    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), response);
  }
}
