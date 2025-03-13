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
package org.idnecology.idn.consensus.ibft.queries;

import org.idnecology.idn.consensus.common.PoaQueryServiceImpl;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.plugin.services.query.IbftQueryService;

import java.util.Collection;
import java.util.Collections;

import org.apache.tuweni.bytes.Bytes32;

/** The Ibft query service. */
public class IbftQueryServiceImpl extends PoaQueryServiceImpl implements IbftQueryService {

  private final BftBlockInterface blockInterface;

  /**
   * Instantiates a new Ibft query service.
   *
   * @param blockInterface the block interface
   * @param blockchain the blockchain
   * @param nodeKey the node key
   */
  public IbftQueryServiceImpl(
      final BftBlockInterface blockInterface, final Blockchain blockchain, final NodeKey nodeKey) {
    super(blockInterface, blockchain, nodeKey);
    this.blockInterface = blockInterface;
  }

  @Override
  public int getRoundNumberFrom(final org.idnecology.idn.plugin.data.BlockHeader header) {
    final BlockHeader headerFromChain = getHeaderFromChain(header);
    final BftExtraData extraData = blockInterface.getExtraData(headerFromChain);
    return extraData.getRound();
  }

  @Override
  public Collection<Address> getSignersFrom(
      final org.idnecology.idn.plugin.data.BlockHeader header) {
    final BlockHeader headerFromChain = getHeaderFromChain(header);
    return Collections.unmodifiableList(blockInterface.getCommitters(headerFromChain));
  }

  private BlockHeader getHeaderFromChain(
      final org.idnecology.idn.plugin.data.BlockHeader header) {
    if (header instanceof BlockHeader) {
      return (BlockHeader) header;
    }

    final Hash blockHash = Hash.wrap(Bytes32.wrap(header.getBlockHash().toArray()));
    return getBlockchain().getBlockHeader(blockHash).orElseThrow();
  }
}
