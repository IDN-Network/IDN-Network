/*
 * Copyright contributors to Hyperledger Idn.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.QbftConfigOptions;
import org.idnecology.idn.ethereum.eth.sync.SyncMode;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IdnControllerTest {

  @Mock GenesisConfig genesisConfig;
  @Mock GenesisConfigOptions genesisConfigOptions;
  @Mock QbftConfigOptions qbftConfigOptions;

  @BeforeEach
  public void setUp() {
    lenient().when(genesisConfig.getConfigOptions()).thenReturn(genesisConfigOptions);
  }

  @Test
  public void missingQbftStartBlock() {
    mockGenesisConfigForMigration("ibft2", OptionalLong.empty());
    assertThatThrownBy(
            () -> new IdnController.Builder().fromGenesisFile(genesisConfig, SyncMode.FULL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing QBFT startBlock config in genesis file");
  }

  @Test
  public void invalidQbftStartBlock() {
    mockGenesisConfigForMigration("ibft2", OptionalLong.of(-1L));
    assertThatThrownBy(
            () -> new IdnController.Builder().fromGenesisFile(genesisConfig, SyncMode.FULL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid QBFT startBlock config in genesis file");
  }

  @Test
  public void invalidConsensusCombination() {
    when(genesisConfigOptions.isConsensusMigration()).thenReturn(true);
    // explicitly not setting isIbft2() for genesisConfigOptions

    assertThatThrownBy(
            () -> new IdnController.Builder().fromGenesisFile(genesisConfig, SyncMode.FULL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Invalid genesis migration config. Migration is supported from IBFT (legacy) or IBFT2 to QBFT)");
  }

  @Test
  public void createConsensusScheduleIdnControllerBuilderWhenMigratingFromIbft2ToQbft() {
    final long qbftStartBlock = 10L;
    mockGenesisConfigForMigration("ibft2", OptionalLong.of(qbftStartBlock));

    final IdnControllerBuilder idnControllerBuilder =
        new IdnController.Builder().fromGenesisFile(genesisConfig, SyncMode.FULL);

    assertThat(idnControllerBuilder).isInstanceOf(ConsensusScheduleIdnControllerBuilder.class);

    final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule =
        ((ConsensusScheduleIdnControllerBuilder) idnControllerBuilder)
            .getIdnControllerBuilderSchedule();

    assertThat(idnControllerBuilderSchedule).containsKeys(0L, qbftStartBlock);
    assertThat(idnControllerBuilderSchedule.get(0L)).isInstanceOf(IbftIdnControllerBuilder.class);
    assertThat(idnControllerBuilderSchedule.get(qbftStartBlock))
        .isInstanceOf(QbftIdnControllerBuilder.class);
  }

  @Test
  public void createConsensusScheduleIdnControllerBuilderWhenMigratingFromIbftLegacyToQbft() {
    final long qbftStartBlock = 10L;
    mockGenesisConfigForMigration("ibftLegacy", OptionalLong.of(qbftStartBlock));

    final IdnControllerBuilder idnControllerBuilder =
        new IdnController.Builder().fromGenesisFile(genesisConfig, SyncMode.FULL);

    assertThat(idnControllerBuilder).isInstanceOf(ConsensusScheduleIdnControllerBuilder.class);

    final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule =
        ((ConsensusScheduleIdnControllerBuilder) idnControllerBuilder)
            .getIdnControllerBuilderSchedule();

    assertThat(idnControllerBuilderSchedule).containsKeys(0L, qbftStartBlock);
    assertThat(idnControllerBuilderSchedule.get(0L))
        .isInstanceOf(IbftLegacyIdnControllerBuilder.class);
    assertThat(idnControllerBuilderSchedule.get(qbftStartBlock))
        .isInstanceOf(QbftIdnControllerBuilder.class);
  }

  private void mockGenesisConfigForMigration(
      final String consensus, final OptionalLong startBlock) {
    when(genesisConfigOptions.isConsensusMigration()).thenReturn(true);

    switch (consensus.toLowerCase(Locale.ROOT)) {
      case "ibft2":
        {
          when(genesisConfigOptions.isIbft2()).thenReturn(true);
          break;
        }
      case "ibftlegacy":
        {
          when(genesisConfigOptions.isIbftLegacy()).thenReturn(true);
          break;
        }
      default:
        fail("Invalid consensus algorithm");
    }

    when(genesisConfigOptions.getQbftConfigOptions()).thenReturn(qbftConfigOptions);
    when(qbftConfigOptions.getStartBlock()).thenReturn(startBlock);
  }

  @Test
  public void postMergeCheckpointSyncUsesMergeControllerBuilder() {
    final GenesisConfig postMergeGenesisFile =
        GenesisConfig.fromResource("/valid_post_merge_near_head_checkpoint.json");

    final IdnControllerBuilder idnControllerBuilder =
        new IdnController.Builder().fromGenesisFile(postMergeGenesisFile, SyncMode.CHECKPOINT);

    assertThat(idnControllerBuilder).isInstanceOf(MergeIdnControllerBuilder.class);
  }

  @Test
  public void postMergeCheckpointSyncWithTotalDifficultyEqualsTTDUsesTransitionControllerBuilder()
      throws IOException {
    final GenesisConfig mergeAtGenesisFile =
        GenesisConfig.fromResource(
            "/invalid_post_merge_checkpoint_total_difficulty_same_as_TTD.json");

    final IdnControllerBuilder idnControllerBuilder =
        new IdnController.Builder().fromGenesisFile(mergeAtGenesisFile, SyncMode.CHECKPOINT);

    assertThat(idnControllerBuilder).isInstanceOf(TransitionIdnControllerBuilder.class);
  }

  @Test
  public void preMergeCheckpointSyncUsesTransitionControllerBuilder() {
    final IdnControllerBuilder idnControllerBuilder =
        new IdnController.Builder().fromGenesisFile(GenesisConfig.mainnet(), SyncMode.CHECKPOINT);

    assertThat(idnControllerBuilder).isInstanceOf(TransitionIdnControllerBuilder.class);
  }

  @Test
  public void nonCheckpointSyncUsesTransitionControllerBuild() {
    final IdnControllerBuilder idnControllerBuilder =
        new IdnController.Builder().fromGenesisFile(GenesisConfig.mainnet(), SyncMode.SNAP);

    assertThat(idnControllerBuilder).isInstanceOf(TransitionIdnControllerBuilder.class);
  }
}
