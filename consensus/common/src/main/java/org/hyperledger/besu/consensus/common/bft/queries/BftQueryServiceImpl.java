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
package org.idnecology.idn.consensus.common.bft.queries;

import org.idnecology.idn.consensus.common.PoaQueryServiceImpl;
import org.idnecology.idn.consensus.common.bft.BftBlockInterface;
import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.plugin.services.query.BftQueryService;

import java.util.Collection;
import java.util.Collections;

import org.apache.tuweni.bytes.Bytes32;

/** The Bft query service. */
public class BftQueryServiceImpl extends PoaQueryServiceImpl implements BftQueryService {

  private final ValidatorProvider validatorProvider;
  private final String consensusMechanismName;
  private final BftBlockInterface bftBlockInterface;

  /**
   * Instantiates a new Bft query service.
   *
   * @param blockInterface the block interface
   * @param blockchain the blockchain
   * @param validatorProvider the validator provider
   * @param nodeKey the node key
   * @param consensusMechanismName the consensus mechanism name
   */
  public BftQueryServiceImpl(
      final BftBlockInterface blockInterface,
      final Blockchain blockchain,
      final ValidatorProvider validatorProvider,
      final NodeKey nodeKey,
      final String consensusMechanismName) {
    super(blockInterface, blockchain, nodeKey);
    this.bftBlockInterface = blockInterface;
    this.validatorProvider = validatorProvider;
    this.consensusMechanismName = consensusMechanismName;
  }

  @Override
  public int getRoundNumberFrom(final org.idnecology.idn.plugin.data.BlockHeader header) {
    final BlockHeader headerFromChain = getHeaderFromChain(header);
    final BftExtraData extraData = bftBlockInterface.getExtraData(headerFromChain);
    return extraData.getRound();
  }

  @Override
  public Collection<Address> getSignersFrom(
      final org.idnecology.idn.plugin.data.BlockHeader header) {
    final BlockHeader headerFromChain = getHeaderFromChain(header);
    return Collections.unmodifiableList(bftBlockInterface.getCommitters(headerFromChain));
  }

  @Override
  public Collection<Address> getValidatorsForLatestBlock() {
    return Collections.unmodifiableCollection(validatorProvider.getValidatorsAtHead());
  }

  @Override
  public String getConsensusMechanismName() {
    return consensusMechanismName;
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
