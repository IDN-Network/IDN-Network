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
package org.idnecology.idn.cli.subcommands.rlp;

import org.idnecology.idn.consensus.common.bft.BftExtraData;
import org.idnecology.idn.consensus.qbft.QbftExtraDataCodec;
import org.idnecology.idn.datatypes.Address;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tuweni.bytes.Bytes;

/** Adapter to convert a typed JSON of addresses to a QBFT RLP extra data encoding */
public class QbftExtraDataCLIAdapter implements JSONToRLP {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Collection<String>> TYPE_REF = new TypeReference<>() {};

  /** Default Constructor. */
  public QbftExtraDataCLIAdapter() {}

  @Override
  public Bytes encode(final String json) throws IOException {
    return fromJsonAddresses(json);
  }

  private Bytes fromJsonAddresses(final String jsonAddresses) throws IOException {
    final Collection<String> validatorAddresses = MAPPER.readValue(jsonAddresses, TYPE_REF);
    return QbftExtraDataCodec.encodeFromAddresses(
        validatorAddresses.stream().map(Address::fromHexString).collect(Collectors.toList()));
  }

  @Override
  public BftExtraData decode(final String rlpInput) throws IOException {
    return fromRLPInput(rlpInput);
  }

  private BftExtraData fromRLPInput(final String rlpInput) throws IOException {
    return new QbftExtraDataCodec().decodeRaw(Bytes.fromHexString(rlpInput));
  }
}
