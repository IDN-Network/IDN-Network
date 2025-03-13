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
package org.idnecology.idn.consensus.clique.jsonrpc;

import org.idnecology.idn.consensus.clique.CliqueBlockInterface;
import org.idnecology.idn.consensus.clique.CliqueContext;
import org.idnecology.idn.consensus.clique.jsonrpc.methods.CliqueGetSignerMetrics;
import org.idnecology.idn.consensus.clique.jsonrpc.methods.CliqueGetSigners;
import org.idnecology.idn.consensus.clique.jsonrpc.methods.CliqueGetSignersAtHash;
import org.idnecology.idn.consensus.clique.jsonrpc.methods.CliqueProposals;
import org.idnecology.idn.consensus.clique.jsonrpc.methods.Discard;
import org.idnecology.idn.consensus.clique.jsonrpc.methods.Propose;
import org.idnecology.idn.consensus.common.EpochManager;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.consensus.common.validator.blockbased.BlockValidatorProvider;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcApis;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.methods.ApiGroupJsonRpcMethods;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import java.util.Map;

/** The Clique json rpc methods. */
public class CliqueJsonRpcMethods extends ApiGroupJsonRpcMethods {
  private final ProtocolContext context;
  private final ProtocolSchedule protocolSchedule;
  private final MiningConfiguration miningConfiguration;

  /**
   * Instantiates a new Clique json rpc methods.
   *
   * @param context the protocol context
   * @param protocolSchedule the protocol schedule
   * @param miningConfiguration the mining parameters
   */
  public CliqueJsonRpcMethods(
      final ProtocolContext context,
      final ProtocolSchedule protocolSchedule,
      final MiningConfiguration miningConfiguration) {
    this.context = context;
    this.protocolSchedule = protocolSchedule;
    this.miningConfiguration = miningConfiguration;
  }

  @Override
  protected String getApiGroup() {
    return RpcApis.CLIQUE.name();
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final MutableBlockchain blockchain = context.getBlockchain();
    final WorldStateArchive worldStateArchive = context.getWorldStateArchive();
    final BlockchainQueries blockchainQueries =
        new BlockchainQueries(protocolSchedule, blockchain, worldStateArchive, miningConfiguration);
    final ValidatorProvider validatorProvider =
        context.getConsensusContext(CliqueContext.class).getValidatorProvider();

    // Must create our own voteTallyCache as using this would pollute the main voteTallyCache
    final ValidatorProvider readOnlyValidatorProvider =
        createValidatorProvider(context, blockchain);

    return mapOf(
        new CliqueGetSigners(blockchainQueries, readOnlyValidatorProvider),
        new CliqueGetSignersAtHash(blockchainQueries, readOnlyValidatorProvider),
        new Propose(validatorProvider),
        new Discard(validatorProvider),
        new CliqueProposals(validatorProvider),
        new CliqueGetSignerMetrics(
            readOnlyValidatorProvider, new CliqueBlockInterface(), blockchainQueries));
  }

  private ValidatorProvider createValidatorProvider(
      final ProtocolContext context, final MutableBlockchain blockchain) {
    final EpochManager epochManager =
        context.getConsensusContext(CliqueContext.class).getEpochManager();
    final CliqueBlockInterface cliqueBlockInterface = new CliqueBlockInterface();
    return BlockValidatorProvider.nonForkingValidatorProvider(
        blockchain, epochManager, cliqueBlockInterface);
  }
}
