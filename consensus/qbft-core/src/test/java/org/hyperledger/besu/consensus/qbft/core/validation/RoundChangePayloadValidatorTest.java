/*
 * Copyright 2020 ConsenSys AG.
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
package org.idnecology.idn.consensus.qbft.core.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.consensus.common.bft.ConsensusRoundIdentifier;
import org.idnecology.idn.consensus.common.bft.payload.SignedData;
import org.idnecology.idn.consensus.qbft.core.payload.PreparedRoundMetadata;
import org.idnecology.idn.consensus.qbft.core.payload.RoundChangePayload;
import org.idnecology.idn.consensus.qbft.core.types.QbftBlockCodec;
import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Hash;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class RoundChangePayloadValidatorTest {

  private static final int VALIDATOR_COUNT = 4;

  private final long chainHeight = 5L;
  private final Hash preparedBlockHash = Hash.fromHexStringLenient("0x1");
  private RoundChangePayloadValidator messageValidator;
  private QbftNodeList validators;
  @Mock QbftBlockCodec qbftBlockCodec;

  @BeforeEach
  public void setup() {
    validators = QbftNodeList.createNodes(VALIDATOR_COUNT, qbftBlockCodec);
    messageValidator = new RoundChangePayloadValidator(validators.getNodeAddresses(), chainHeight);
  }

  @Test
  public void roundChangeIsValidIfItMatchesExpectedValues() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight, 1),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 0)));

    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final SignedData<RoundChangePayload> signedPayload =
          createSignedPayload(payload, validators.getNode(i).getNodeKey());
      assertThat(messageValidator.validate(signedPayload)).isTrue();
    }
  }

  @Test
  public void roundChangePayloadWithMissingPreparedMetadataIsValid() {
    final RoundChangePayload payload =
        new RoundChangePayload(new ConsensusRoundIdentifier(chainHeight, 1), Optional.empty());

    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final SignedData<RoundChangePayload> signedPayload =
          createSignedPayload(payload, validators.getNode(i).getNodeKey());
      assertThat(messageValidator.validate(signedPayload)).isTrue();
    }
  }

  @Test
  public void roundChangePayloadSignedByNonValidatorFails() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight, 1),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 0)));

    final NodeKey nonValidatorKey = NodeKeyUtils.generate();
    final SignedData<RoundChangePayload> signedPayload =
        createSignedPayload(payload, nonValidatorKey);
    assertThat(messageValidator.validate(signedPayload)).isFalse();
  }

  @Test
  public void roundChangeForFutureHeightFails() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight + 1, 1),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 0)));

    final SignedData<RoundChangePayload> signedPayload =
        createSignedPayload(payload, validators.getNode(0).getNodeKey());
    assertThat(messageValidator.validate(signedPayload)).isFalse();
  }

  @Test
  public void roundChangeForPriorHeightFails() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight - 1, 1),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 0)));

    final SignedData<RoundChangePayload> signedPayload =
        createSignedPayload(payload, validators.getNode(0).getNodeKey());
    assertThat(messageValidator.validate(signedPayload)).isFalse();
  }

  @Test
  public void roundChangeWithMatchingTargetAndPrepareFails() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight, 1),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 1)));

    final SignedData<RoundChangePayload> signedPayload =
        createSignedPayload(payload, validators.getNode(0).getNodeKey());
    assertThat(messageValidator.validate(signedPayload)).isFalse();
  }

  @Test
  public void roundChangeWithPreparedRoundInTheFutureFails() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight, 1),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 2)));

    final SignedData<RoundChangePayload> signedPayload =
        createSignedPayload(payload, validators.getNode(0).getNodeKey());
    assertThat(messageValidator.validate(signedPayload)).isFalse();
  }

  @Test
  public void roundChangeWithZeroTargetRoundFails() {
    final RoundChangePayload payload =
        new RoundChangePayload(
            new ConsensusRoundIdentifier(chainHeight, 0),
            Optional.of(new PreparedRoundMetadata(preparedBlockHash, 0)));

    final SignedData<RoundChangePayload> signedPayload =
        createSignedPayload(payload, validators.getNode(0).getNodeKey());
    assertThat(messageValidator.validate(signedPayload)).isFalse();
  }

  private SignedData<RoundChangePayload> createSignedPayload(
      final RoundChangePayload payload, final NodeKey nodeKey) {
    final SECPSignature signature = nodeKey.sign(payload.hashForSignature());
    return SignedData.create(payload, signature);
  }
}
