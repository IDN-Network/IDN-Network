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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.Quantity;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.MiningConfiguration;

import org.junit.jupiter.api.Test;

public class MinerGetMinGasPriceTest {

  @Test
  public void shouldReturnDefaultMinGasPrice() {
    final MiningConfiguration miningConfiguration = ImmutableMiningConfiguration.newDefault();
    final MinerGetMinGasPrice method = new MinerGetMinGasPrice(miningConfiguration);
    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", method.getName(), new Object[] {}));

    final JsonRpcResponse expected =
        new JsonRpcSuccessResponse(
            request.getRequest().getId(),
            Quantity.create(
                MiningConfiguration.MutableInitValues.DEFAULT_MIN_TRANSACTION_GAS_PRICE));

    final JsonRpcResponse actual = method.response(request);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void shouldReturnSetAtRuntimeMinGasPrice() {
    final MiningConfiguration miningConfiguration = ImmutableMiningConfiguration.newDefault();
    final MinerGetMinGasPrice method = new MinerGetMinGasPrice(miningConfiguration);

    final Wei minGasPriceAtRuntime = Wei.of(2000);

    miningConfiguration.setMinTransactionGasPrice(minGasPriceAtRuntime);

    final JsonRpcRequestContext request =
        new JsonRpcRequestContext(new JsonRpcRequest("2.0", method.getName(), new Object[] {}));

    final JsonRpcResponse expected =
        new JsonRpcSuccessResponse(
            request.getRequest().getId(), Quantity.create(minGasPriceAtRuntime));

    final JsonRpcResponse actual = method.response(request);
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
