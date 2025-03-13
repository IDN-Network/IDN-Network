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
package org.idnecology.idn.ethereum.difficulty.fixed;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import org.junit.jupiter.api.Test;

public class FixedProtocolScheduleTest {

  @Test
  public void reportedDifficultyForAllBlocksIsAFixedValue() {

    final ProtocolSchedule schedule =
        FixedDifficultyProtocolSchedule.create(
            GenesisConfig.fromResource("/dev.json").getConfigOptions(),
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem());

    final BlockHeaderTestFixture headerBuilder = new BlockHeaderTestFixture();

    final BlockHeader parentHeader = headerBuilder.number(1).buildHeader();

    assertThat(
            schedule
                .getByBlockHeader(blockHeader(0))
                .getDifficultyCalculator()
                .nextDifficulty(1, parentHeader))
        .isEqualTo(FixedDifficultyCalculators.DEFAULT_DIFFICULTY);

    assertThat(
            schedule
                .getByBlockHeader(blockHeader(500))
                .getDifficultyCalculator()
                .nextDifficulty(1, parentHeader))
        .isEqualTo(FixedDifficultyCalculators.DEFAULT_DIFFICULTY);

    assertThat(
            schedule
                .getByBlockHeader(blockHeader(500_000))
                .getDifficultyCalculator()
                .nextDifficulty(1, parentHeader))
        .isEqualTo(FixedDifficultyCalculators.DEFAULT_DIFFICULTY);
  }

  private BlockHeader blockHeader(final long number) {
    return new BlockHeaderTestFixture().number(number).buildHeader();
  }
}
