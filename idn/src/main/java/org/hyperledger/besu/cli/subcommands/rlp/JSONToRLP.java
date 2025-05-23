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

import java.io.IOException;

import org.apache.tuweni.bytes.Bytes;

/** Behaviour of objects that can be encoded from JSON to RLP */
interface JSONToRLP {

  /**
   * Encodes the object into an RLP value.
   *
   * @param json the JSON to convert to RLP
   * @return the RLP encoded object.
   * @throws IOException if an error occurs while reading data
   */
  Bytes encode(String json) throws IOException;

  /**
   * Decodes the input RLP value into a validators list Object.
   *
   * @param inputData the RLP hex string to convert to validators list
   * @return the decoded BFT ExtraData object.
   * @throws IOException if an error occurs while reading data
   */
  BftExtraData decode(String inputData) throws IOException;
}
