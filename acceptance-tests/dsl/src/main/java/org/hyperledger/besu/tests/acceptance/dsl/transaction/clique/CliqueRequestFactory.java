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
package org.idnecology.idn.tests.acceptance.dsl.transaction.clique;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

public class CliqueRequestFactory {

  public static class ProposeResponse extends Response<Boolean> {}

  public static class DiscardResponse extends Response<Boolean> {}

  public static class SignersBlockResponse extends Response<List<Address>> {}

  public static class ProposalsResponse extends Response<Map<Address, Boolean>> {}

  private final Web3jService web3jService;

  public CliqueRequestFactory(final Web3jService web3jService) {
    this.web3jService = web3jService;
  }

  Request<?, ProposeResponse> cliquePropose(final String address, final Boolean auth) {
    return new Request<>(
        "clique_propose",
        Arrays.asList(address, auth.toString()),
        web3jService,
        ProposeResponse.class);
  }

  Request<?, DiscardResponse> cliqueDiscard(final String address) {
    return new Request<>(
        "clique_discard", singletonList(address), web3jService, DiscardResponse.class);
  }

  Request<?, ProposalsResponse> cliqueProposals() {
    return new Request<>("clique_proposals", emptyList(), web3jService, ProposalsResponse.class);
  }

  Request<?, SignersBlockResponse> cliqueGetSigners(final String blockNumber) {
    return new Request<>(
        "clique_getSigners", singletonList(blockNumber), web3jService, SignersBlockResponse.class);
  }

  Request<?, SignersBlockResponse> cliqueGetSignersAtHash(final Hash hash) {
    return new Request<>(
        "clique_getSignersAtHash",
        singletonList(hash.toString()),
        web3jService,
        SignersBlockResponse.class);
  }
}
