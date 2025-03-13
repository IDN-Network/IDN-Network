/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.tests.acceptance.bft;

import org.idnecology.idn.ethereum.eth.sync.SyncMode;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BftSyncAcceptanceTest extends ParameterizedBftTestBase {

  private static final int TARGET_BLOCK_HEIGHT = 70;

  static Stream<Arguments> syncModeTestParameters() {
    return Stream.of(SyncMode.FULL, SyncMode.SNAP, SyncMode.CHECKPOINT)
        .flatMap(
            syncMode ->
                factoryFunctions()
                    .map(args -> Arguments.of(args.get()[0], args.get()[1], syncMode)));
  }

  @ParameterizedTest(name = "{index}: {0} with {2} sync")
  @MethodSource("syncModeTestParameters")
  public void shouldSyncValidatorNode(
      final String testName,
      final BftAcceptanceTestParameterization nodeFactory,
      final SyncMode syncMode)
      throws Exception {
    setUp(testName, nodeFactory);

    // Create validator network with 4 validators
    final IdnNode validator1 = nodeFactory.createBonsaiNodeFixedPort(idn, "validator1");
    final IdnNode validator2 = nodeFactory.createBonsaiNodeFixedPort(idn, "validator2");
    final IdnNode validator3 = nodeFactory.createBonsaiNodeFixedPort(idn, "validator3");
    final IdnNode validator4 = nodeFactory.createBonsaiNodeFixedPort(idn, "validator4");

    // Configure validators with specified sync mode
    final SynchronizerConfiguration syncConfig =
        SynchronizerConfiguration.builder().syncMode(syncMode).syncMinimumPeerCount(1).build();

    validator4.setSynchronizerConfiguration(syncConfig);

    // Start first three validators
    cluster.start(validator1, validator2, validator3);

    validator1.verify(blockchain.minimumHeight(TARGET_BLOCK_HEIGHT, TARGET_BLOCK_HEIGHT));
    // Add validator4 to cluster and start
    cluster.addNode(validator4);

    validator4.verify(blockchain.minimumHeight(TARGET_BLOCK_HEIGHT, 60));
  }
}
