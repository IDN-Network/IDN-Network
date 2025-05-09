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
package org.idnecology.idn.consensus.ibft.jsonrpc;

import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftContext;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.consensus.ibft.jsonrpc.methods.IbftDiscardValidatorVote;
import org.idnecology.idn.consensus.ibft.jsonrpc.methods.IbftGetPendingVotes;
import org.idnecology.idn.consensus.ibft.jsonrpc.methods.IbftGetSignerMetrics;
import org.idnecology.idn.consensus.ibft.jsonrpc.methods.IbftGetValidatorsByBlockHash;
import org.idnecology.idn.consensus.ibft.jsonrpc.methods.IbftGetValidatorsByBlockNumber;
import org.idnecology.idn.consensus.ibft.jsonrpc.methods.IbftProposeValidatorVote;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.ApiGroupJsonRpcMethods;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

import java.util.Map;

/** The Ibft json rpc methods. */
public class IbftJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final ProtocolContext context;
  private final ProtocolSchedule protocolSchedule;
  private final MiningConfiguration miningConfiguration;

  /**
   * Instantiates a new Ibft json rpc methods.
   *
   * @param context the protocol context
   * @param protocolSchedule the protocol schedule
   * @param miningConfiguration the mining parameters
   */
  public IbftJsonRpcMethods(
      final ProtocolContext context,
      final ProtocolSchedule protocolSchedule,
      final MiningConfiguration miningConfiguration) {
    this.context = context;
    this.protocolSchedule = protocolSchedule;
    this.miningConfiguration = miningConfiguration;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.IBFT.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final MutableBlockchain blockchain = context.getBlockchain();
    final BlockchainQueries blockchainQueries =
        new BlockchainQueries(
            protocolSchedule, blockchain, context.getWorldStateArchive(), miningConfiguration);
    final BftContext bftContext = context.getConsensusContext(BftContext.class);
    final BlockInterface blockInterface = bftContext.getBlockInterface();
    final ValidatorProvider validatorProvider =
        context.getConsensusContext(BftContext.class).getValidatorProvider();

    // Must create our own voteTallyCache as using this would pollute the main voteTallyCache
    final ValidatorProvider readOnlyValidatorProvider =
        createValidatorProvider(context, blockchain);

    return mapOf(
        new IbftProposeValidatorVote(validatorProvider),
        new IbftGetValidatorsByBlockNumber(blockchainQueries, blockInterface),
        new IbftDiscardValidatorVote(validatorProvider),
        new IbftGetValidatorsByBlockHash(blockchain, blockInterface),
        new IbftGetSignerMetrics(readOnlyValidatorProvider, blockInterface, blockchainQueries),
        new IbftGetPendingVotes(validatorProvider));
  }

  private ValidatorProvider createValidatorProvider(
      final ProtocolContext context, final MutableBlockchain blockchain) {
    final BftContext bftContext = context.getConsensusContext(BftContext.class);
    final EpochManager epochManager = bftContext.getEpochManager();
    final BftBlockInterface bftBlockInterface = bftContext.getBlockInterface();
    return BlockValidatorProvider.nonForkingValidatorProvider(
        blockchain, epochManager, bftBlockInterface);
  }
}
