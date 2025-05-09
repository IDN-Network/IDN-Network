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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerChangeTargetGasLimit;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerGetExtraData;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerGetMinGasPrice;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerGetMinPriorityFee;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerSetCoinbase;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerSetEtherbase;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerSetExtraData;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerSetMinGasPrice;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerSetMinPriorityFee;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerStart;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.miner.MinerStop;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.core.MiningConfiguration;

import java.util.Map;

public class MinerJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final MiningCoordinator miningCoordinator;
  private final MiningConfiguration miningConfiguration;

  public MinerJsonRpcMethods(
      final MiningConfiguration miningConfiguration, final MiningCoordinator miningCoordinator) {
    this.miningConfiguration = miningConfiguration;
    this.miningCoordinator = miningCoordinator;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.MINER.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final MinerSetCoinbase minerSetCoinbase = new MinerSetCoinbase(miningCoordinator);
    return mapOf(
        new MinerStart(miningCoordinator),
        new MinerStop(miningCoordinator),
        minerSetCoinbase,
        new MinerSetEtherbase(minerSetCoinbase),
        new MinerChangeTargetGasLimit(miningCoordinator),
        new MinerGetMinPriorityFee(miningConfiguration),
        new MinerSetMinPriorityFee(miningConfiguration),
        new MinerGetMinGasPrice(miningConfiguration),
        new MinerSetMinGasPrice(miningConfiguration),
        new MinerGetExtraData(miningConfiguration),
        new MinerSetExtraData(miningConfiguration));
  }
}
