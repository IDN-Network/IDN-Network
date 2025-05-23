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
package org.idnecology.idn.consensus.ibft.jsonrpc.methods;

import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.jsonrpc.AbstractGetSignerMetricsMethod;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;

/** The Ibft get signer metrics. */
public class IbftGetSignerMetrics extends AbstractGetSignerMetricsMethod implements JsonRpcMethod {

  /**
   * Instantiates a new Ibft get signer metrics.
   *
   * @param validatorProvider the validator provider
   * @param blockInterface the block interface
   * @param blockchainQueries the blockchain queries
   */
  public IbftGetSignerMetrics(
      final ValidatorProvider validatorProvider,
      final BlockInterface blockInterface,
      final BlockchainQueries blockchainQueries) {
    super(validatorProvider, blockInterface, blockchainQueries);
  }

  @Override
  public String getName() {
    return RpcMethod.IBFT_GET_SIGNER_METRICS.getMethodName();
  }
}
