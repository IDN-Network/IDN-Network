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
package org.idnecology.idn.consensus.common.bft;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.idnecology.idn.crypto.SECPSignature;
import org.idnecology.idn.cryptoservices.NodeKey;
import org.idnecology.idn.cryptoservices.NodeKeyUtils;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.Util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.tuweni.bytes.Bytes;

public class BftExtraDataFixture {

  public static BftExtraData createExtraData(
      final BlockHeader header, final BftExtraDataCodec bftExtraDataCodec) {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());
    final List<Address> validators = singletonList(proposerAddress);

    return createExtraData(
        header,
        Bytes.wrap(new byte[BftExtraDataCodec.EXTRA_VANITY_LENGTH]),
        Optional.of(Vote.authVote(Address.fromHexString("1"))),
        validators,
        singletonList(proposerNodeKey),
        0x2A,
        bftExtraDataCodec);
  }

  public static BftExtraData createExtraData(
      final BlockHeader header,
      final Bytes vanityData,
      final Optional<Vote> vote,
      final List<Address> validators,
      final List<NodeKey> committerNodeKeys,
      final BftExtraDataCodec bftExtraDataCodec) {

    return createExtraData(
        header, vanityData, vote, validators, committerNodeKeys, 0, bftExtraDataCodec);
  }

  public static BftExtraData createExtraData(
      final BlockHeader header,
      final Bytes vanityData,
      final Optional<Vote> vote,
      final List<Address> validators,
      final List<NodeKey> committerNodeKeys,
      final int roundNumber,
      final BftExtraDataCodec bftExtraDataCodec) {

    return createExtraData(
        header,
        vanityData,
        vote,
        validators,
        committerNodeKeys,
        roundNumber,
        false,
        bftExtraDataCodec);
  }

  public static BftExtraData createExtraData(
      final BlockHeader header,
      final Bytes vanityData,
      final Optional<Vote> vote,
      final List<Address> validators,
      final List<NodeKey> committerNodeKeys,
      final int baseRoundNumber,
      final boolean useDifferentRoundNumbersForCommittedSeals,
      final BftExtraDataCodec bftExtraDataCodec) {

    final BftExtraData bftExtraDataNoCommittedSeals =
        new BftExtraData(vanityData, emptyList(), vote, baseRoundNumber, validators);

    // if useDifferentRoundNumbersForCommittedSeals is true then each committed seal will be
    // calculated for an extraData field with a different round number
    List<SECPSignature> commitSeals =
        IntStream.range(0, committerNodeKeys.size())
            .mapToObj(
                i -> {
                  final int round =
                      useDifferentRoundNumbersForCommittedSeals
                          ? bftExtraDataNoCommittedSeals.getRound() + i
                          : bftExtraDataNoCommittedSeals.getRound();

                  BftExtraData extraDataForCommittedSealCalculation =
                      new BftExtraData(
                          bftExtraDataNoCommittedSeals.getVanityData(),
                          emptyList(),
                          bftExtraDataNoCommittedSeals.getVote(),
                          round,
                          bftExtraDataNoCommittedSeals.getValidators());

                  final Hash headerHashForCommitters =
                      new BftBlockHashing(bftExtraDataCodec)
                          .calculateDataHashForCommittedSeal(
                              header, extraDataForCommittedSealCalculation);

                  return committerNodeKeys.get(i).sign(headerHashForCommitters);
                })
            .collect(Collectors.toList());

    return new BftExtraData(
        bftExtraDataNoCommittedSeals.getVanityData(),
        commitSeals,
        bftExtraDataNoCommittedSeals.getVote(),
        bftExtraDataNoCommittedSeals.getRound(),
        bftExtraDataNoCommittedSeals.getValidators());
  }
}
