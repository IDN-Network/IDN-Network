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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.ApiConfiguration;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.Quantity;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;

public class EthGasPrice implements JsonRpcMethod {

  private final BlockchainQueries blockchainQueries;
  private final ApiConfiguration apiConfiguration;

  public EthGasPrice(
      final BlockchainQueries blockchainQueries, final ApiConfiguration apiConfiguration) {
    this.blockchainQueries = blockchainQueries;
    this.apiConfiguration = apiConfiguration;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GAS_PRICE.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), Quantity.create(calculateGasPrice()));
  }

  private Wei calculateGasPrice() {
    final Wei gasPrice = blockchainQueries.gasPrice();
    return isGasPriceLimitingEnabled() ? limitGasPrice(gasPrice) : gasPrice;
  }

  private boolean isGasPriceLimitingEnabled() {
    return apiConfiguration.isGasAndPriorityFeeLimitingEnabled();
  }

  private Wei limitGasPrice(final Wei gasPrice) {
    final Wei lowerBoundGasPrice = blockchainQueries.gasPriceLowerBound();
    final Wei forcedLowerBound =
        calculateBound(
            lowerBoundGasPrice, apiConfiguration.getLowerBoundGasAndPriorityFeeCoefficient());
    final Wei forcedUpperBound =
        calculateBound(
            lowerBoundGasPrice, apiConfiguration.getUpperBoundGasAndPriorityFeeCoefficient());

    return gasPrice.compareTo(forcedLowerBound) <= 0
        ? forcedLowerBound
        : gasPrice.compareTo(forcedUpperBound) >= 0 ? forcedUpperBound : gasPrice;
  }

  private Wei calculateBound(final Wei price, final long coefficient) {
    return price.multiply(coefficient).divide(100);
  }
}
