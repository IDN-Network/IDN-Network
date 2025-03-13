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
package org.idnecology.idn.controller;

import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftExtraDataCodec;
import org.idnecology.idn.consensus.common.bft.queries.BftQueryServiceImpl;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.plugin.services.metrics.PoAMetricsService;
import org.idnecology.idn.plugin.services.query.BftQueryService;
import org.idnecology.idn.plugin.services.query.PoaQueryService;
import org.idnecology.idn.services.IdnPluginContextImpl;

/** Bft query plugin service factory which is a concrete implementation of PluginServiceFactory. */
public class BftQueryPluginServiceFactory implements PluginServiceFactory {

  private final Blockchain blockchain;
  private final BftExtraDataCodec bftExtraDataCodec;
  private final ValidatorProvider validatorProvider;
  private final NodeKey nodeKey;
  private final String consensusMechanismName;

  /**
   * Instantiates a new Bft query plugin service factory.
   *
   * @param blockchain the blockchain
   * @param bftExtraDataCodec the bft extra data codec
   * @param validatorProvider the validator provider
   * @param nodeKey the node key
   * @param consensusMechanismName the consensus mechanism name
   */
  public BftQueryPluginServiceFactory(
      final Blockchain blockchain,
      final BftExtraDataCodec bftExtraDataCodec,
      final ValidatorProvider validatorProvider,
      final NodeKey nodeKey,
      final String consensusMechanismName) {
    this.blockchain = blockchain;
    this.bftExtraDataCodec = bftExtraDataCodec;
    this.validatorProvider = validatorProvider;
    this.nodeKey = nodeKey;
    this.consensusMechanismName = consensusMechanismName;
  }

  @Override
  public void appendPluginServices(final IdnPluginContextImpl idnContext) {
    final BftBlockInterface blockInterface = new BftBlockInterface(bftExtraDataCodec);

    final BftQueryServiceImpl service =
        new BftQueryServiceImpl(
            blockInterface, blockchain, validatorProvider, nodeKey, consensusMechanismName);
    idnContext.addService(BftQueryService.class, service);
    idnContext.addService(PoaQueryService.class, service);
    idnContext.addService(PoAMetricsService.class, service);
  }
}
