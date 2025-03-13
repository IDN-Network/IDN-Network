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

import org.idnecology.idn.consensus.clique.CliqueBlockInterface;
import org.idnecology.idn.consensus.common.BlockInterface;
import org.idnecology.idn.consensus.common.PoaQueryServiceImpl;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.plugin.services.metrics.PoAMetricsService;
import org.idnecology.idn.plugin.services.query.PoaQueryService;
import org.idnecology.idn.services.IdnPluginContextImpl;

/** The Clique query plugin service factory. */
public class CliqueQueryPluginServiceFactory implements PluginServiceFactory {

  private final Blockchain blockchain;
  private final NodeKey nodeKey;

  /**
   * Instantiates a new Clique query plugin service factory.
   *
   * @param blockchain the blockchain
   * @param nodeKey the node key
   */
  public CliqueQueryPluginServiceFactory(final Blockchain blockchain, final NodeKey nodeKey) {
    this.blockchain = blockchain;
    this.nodeKey = nodeKey;
  }

  @Override
  public void appendPluginServices(final IdnPluginContextImpl idnContext) {
    final BlockInterface blockInterface = new CliqueBlockInterface();
    final PoaQueryServiceImpl service =
        new PoaQueryServiceImpl(blockInterface, blockchain, nodeKey);
    idnContext.addService(PoaQueryService.class, service);
    idnContext.addService(PoAMetricsService.class, service);
  }
}
