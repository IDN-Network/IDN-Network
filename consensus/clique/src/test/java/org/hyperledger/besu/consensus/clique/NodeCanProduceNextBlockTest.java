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
package org.idnecology.idn.consensus.clique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.clique.headervalidationrules.SignerRateLimitValidationRule;
import org.idnecology.idn.consensus.common.validator.ValidatorProvider;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.AddressHelpers;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Util;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NodeCanProduceNextBlockTest {

  private final KeyPair proposerKeyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private Address localAddress;
  private final KeyPair otherNodeKeyPair =
      SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private final List<Address> validatorList = Lists.newArrayList();
  private final BlockHeaderTestFixture headerBuilder = new BlockHeaderTestFixture();
  private ProtocolContext cliqueProtocolContext;
  private final CliqueBlockInterface blockInterface = new CliqueBlockInterface();

  MutableBlockchain blockChain;
  private Block genesisBlock;

  private Block createEmptyBlock(final KeyPair blockSigner) {
    final BlockHeader header =
        TestHelpers.createCliqueSignedBlockHeader(headerBuilder, blockSigner, validatorList);
    return new Block(header, new BlockBody(Lists.newArrayList(), Lists.newArrayList()));
  }

  @BeforeEach
  public void setup() {
    localAddress = Util.publicKeyToAddress(proposerKeyPair.getPublicKey());
    validatorList.add(localAddress);
  }

  @Test
  public void networkWithOneValidatorIsAllowedToCreateConsecutiveBlocks() {
    final Address localAddress = Util.publicKeyToAddress(proposerKeyPair.getPublicKey());

    genesisBlock = createEmptyBlock(proposerKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.number(1).parentHash(genesisBlock.getHash());
    final Block block_1 = createEmptyBlock(proposerKeyPair);
    blockChain.appendBlock(block_1, Lists.newArrayList());

    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_1.getHeader()))
        .isTrue();
  }

  @Test
  public void networkWithTwoValidatorsIsAllowedToProduceBlockIfNotPreviousBlockProposer() {
    final Address otherAddress = Util.publicKeyToAddress(otherNodeKeyPair.getPublicKey());
    validatorList.add(otherAddress);

    genesisBlock = createEmptyBlock(otherNodeKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.number(1).parentHash(genesisBlock.getHash());
    final Block block_1 = createEmptyBlock(proposerKeyPair);
    blockChain.appendBlock(block_1, Lists.newArrayList());

    headerBuilder.number(2).parentHash(block_1.getHash());
    final Block block_2 = createEmptyBlock(otherNodeKeyPair);
    blockChain.appendBlock(block_2, Lists.newArrayList());

    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_1.getHeader()))
        .isFalse();

    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_2.getHeader()))
        .isTrue();
  }

  @Test
  public void networkWithTwoValidatorsIsNotAllowedToProduceBlockIfIsPreviousBlockProposer() {
    final Address otherAddress = Util.publicKeyToAddress(otherNodeKeyPair.getPublicKey());
    validatorList.add(otherAddress);

    genesisBlock = createEmptyBlock(proposerKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.parentHash(genesisBlock.getHash()).number(1);
    final Block block_1 = createEmptyBlock(proposerKeyPair);
    blockChain.appendBlock(block_1, Lists.newArrayList());

    headerBuilder.parentHash(block_1.getHeader().getHash()).number(2);
    final BlockHeader block_2 =
        TestHelpers.createCliqueSignedBlockHeader(headerBuilder, proposerKeyPair, validatorList);

    final SignerRateLimitValidationRule validationRule = new SignerRateLimitValidationRule();

    assertThat(validationRule.validate(block_2, block_1.getHeader(), cliqueProtocolContext))
        .isFalse();
  }

  @Test
  public void withThreeValidatorsMustHaveOneBlockBetweenSignings() {
    final Address otherAddress = Util.publicKeyToAddress(otherNodeKeyPair.getPublicKey());
    validatorList.add(otherAddress);
    validatorList.add(AddressHelpers.ofValue(1));

    genesisBlock = createEmptyBlock(proposerKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.parentHash(genesisBlock.getHash()).number(1);
    final Block block_1 = createEmptyBlock(proposerKeyPair);
    blockChain.appendBlock(block_1, Lists.newArrayList());

    headerBuilder.parentHash(block_1.getHash()).number(2);
    final Block block_2 = createEmptyBlock(otherNodeKeyPair);
    blockChain.appendBlock(block_2, Lists.newArrayList());

    headerBuilder.parentHash(block_2.getHash()).number(3);
    final Block block_3 = createEmptyBlock(otherNodeKeyPair);
    blockChain.appendBlock(block_3, Lists.newArrayList());

    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_1.getHeader()))
        .isFalse();
    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_2.getHeader()))
        .isTrue();
    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_3.getHeader()))
        .isTrue();
  }

  @Test
  public void signerIsValidIfInsufficientBlocksExistInHistory() {
    final Address otherAddress = Util.publicKeyToAddress(otherNodeKeyPair.getPublicKey());
    validatorList.add(otherAddress);
    validatorList.add(AddressHelpers.ofValue(1));
    validatorList.add(AddressHelpers.ofValue(2));
    validatorList.add(AddressHelpers.ofValue(3));
    // Should require 2 blocks between signings.

    genesisBlock = createEmptyBlock(proposerKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.parentHash(genesisBlock.getHash()).number(1);
    final Block block_1 = createEmptyBlock(otherNodeKeyPair);
    blockChain.appendBlock(block_1, Lists.newArrayList());

    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, genesisBlock.getHeader()))
        .isTrue();
    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                localAddress, cliqueProtocolContext, block_1.getHeader()))
        .isTrue();
  }

  @Test
  public void exceptionIsThrownIfOnAnOrphanedChain() {
    final Address otherAddress = Util.publicKeyToAddress(otherNodeKeyPair.getPublicKey());
    validatorList.add(otherAddress);

    genesisBlock = createEmptyBlock(proposerKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.parentHash(Hash.ZERO).number(3);
    final BlockHeader parentHeader =
        TestHelpers.createCliqueSignedBlockHeader(headerBuilder, otherNodeKeyPair, validatorList);

    assertThatThrownBy(
            () ->
                CliqueHelpers.addressIsAllowedToProduceNextBlock(
                    localAddress, cliqueProtocolContext, parentHeader))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("The block was on a orphaned chain.");
  }

  @Test
  public void nonValidatorIsNotAllowedToCreateABlock() {
    genesisBlock = createEmptyBlock(otherNodeKeyPair);

    blockChain = createInMemoryBlockchain(genesisBlock);

    final ValidatorProvider validatorProvider = mock(ValidatorProvider.class);
    when(validatorProvider.getValidatorsAfterBlock(any())).thenReturn(validatorList);
    final CliqueContext cliqueContext = new CliqueContext(validatorProvider, null, blockInterface);
    cliqueProtocolContext =
        new ProtocolContext(blockChain, null, cliqueContext, new BadBlockManager());

    headerBuilder.parentHash(Hash.ZERO).number(3);
    final BlockHeader parentHeader = headerBuilder.buildHeader();
    assertThat(
            CliqueHelpers.addressIsAllowedToProduceNextBlock(
                AddressHelpers.ofValue(1), cliqueProtocolContext, parentHeader))
        .isFalse();
  }
}
