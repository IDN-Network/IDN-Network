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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.consensus.common.ForkSpec;
import org.idnecology.idn.consensus.common.ForksSchedule;
import org.idnecology.idn.consensus.common.MigratingConsensusContext;
import org.idnecology.idn.consensus.common.MigratingMiningCoordinator;
import org.idnecology.idn.consensus.common.bft.blockcreation.BftMiningCoordinator;
import org.idnecology.idn.ethereum.ConsensusContext;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.chain.Blockchain;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.eth.manager.EthProtocolManager;
import org.idnecology.idn.ethereum.eth.sync.state.SyncState;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.worldstate.WorldStateArchive;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConsensusScheduleIdnControllerBuilderTest {
  private @Mock BiFunction<
          NavigableSet<ForkSpec<ProtocolSchedule>>, Optional<BigInteger>, ProtocolSchedule>
      combinedProtocolScheduleFactory;
  private @Mock GenesisConfig genesisConfig;
  private @Mock IdnControllerBuilder idnControllerBuilder1;
  private @Mock IdnControllerBuilder idnControllerBuilder2;
  private @Mock IdnControllerBuilder idnControllerBuilder3;
  private @Mock ProtocolSchedule protocolSchedule1;
  private @Mock ProtocolSchedule protocolSchedule2;
  private @Mock ProtocolSchedule protocolSchedule3;
  private @Mock MiningCoordinator miningCoordinator1;
  private @Mock BftMiningCoordinator miningCoordinator2;

  @Test
  public void mustProvideNonNullConsensusScheduleWhenInstantiatingNew() {
    assertThatThrownBy(() -> new ConsensusScheduleIdnControllerBuilder(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("IdnControllerBuilder schedule can't be null");
  }

  @Test
  public void mustProvideNonEmptyConsensusScheduleWhenInstantiatingNew() {
    assertThatThrownBy(() -> new ConsensusScheduleIdnControllerBuilder(Collections.emptyMap()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("IdnControllerBuilder schedule can't be empty");
  }

  @Test
  public void mustCreateCombinedProtocolScheduleUsingProtocolSchedulesOrderedByBlock() {
    // Use an ordered map with keys in the incorrect order so that we can show that set is created
    // with the correct order
    final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule = new TreeMap<>();
    idnControllerBuilderSchedule.put(30L, idnControllerBuilder3);
    idnControllerBuilderSchedule.put(10L, idnControllerBuilder2);
    idnControllerBuilderSchedule.put(0L, idnControllerBuilder1);

    when(idnControllerBuilder1.createProtocolSchedule()).thenReturn(protocolSchedule1);
    when(idnControllerBuilder2.createProtocolSchedule()).thenReturn(protocolSchedule2);
    when(idnControllerBuilder3.createProtocolSchedule()).thenReturn(protocolSchedule3);

    final StubGenesisConfigOptions genesisConfigOptions = new StubGenesisConfigOptions();
    genesisConfigOptions.chainId(BigInteger.TEN);

    final ConsensusScheduleIdnControllerBuilder consensusScheduleIdnControllerBuilder =
        new ConsensusScheduleIdnControllerBuilder(
            idnControllerBuilderSchedule, combinedProtocolScheduleFactory);
    when(genesisConfig.getConfigOptions()).thenReturn(genesisConfigOptions);
    consensusScheduleIdnControllerBuilder.genesisConfig(genesisConfig);
    consensusScheduleIdnControllerBuilder.createProtocolSchedule();

    final NavigableSet<ForkSpec<ProtocolSchedule>> expectedProtocolSchedulesSpecs =
        new TreeSet<>(ForkSpec.COMPARATOR);
    expectedProtocolSchedulesSpecs.add(new ForkSpec<>(0L, protocolSchedule1));
    expectedProtocolSchedulesSpecs.add(new ForkSpec<>(10L, protocolSchedule2));
    expectedProtocolSchedulesSpecs.add(new ForkSpec<>(30L, protocolSchedule3));
    Mockito.verify(combinedProtocolScheduleFactory)
        .apply(expectedProtocolSchedulesSpecs, Optional.of(BigInteger.TEN));
  }

  @Test
  public void createsMigratingMiningCoordinator() {
    final Map<Long, IdnControllerBuilder> consensusSchedule =
        Map.of(0L, idnControllerBuilder1, 5L, idnControllerBuilder2);

    when(idnControllerBuilder1.createMiningCoordinator(any(), any(), any(), any(), any(), any()))
        .thenReturn(miningCoordinator1);
    when(idnControllerBuilder2.createMiningCoordinator(any(), any(), any(), any(), any(), any()))
        .thenReturn(miningCoordinator2);
    final ProtocolContext mockProtocolContext = mock(ProtocolContext.class);
    when(mockProtocolContext.getBlockchain()).thenReturn(mock(MutableBlockchain.class));

    final ConsensusScheduleIdnControllerBuilder builder =
        new ConsensusScheduleIdnControllerBuilder(consensusSchedule);
    final MiningCoordinator miningCoordinator =
        builder.createMiningCoordinator(
            protocolSchedule1,
            mockProtocolContext,
            mock(TransactionPool.class),
            mock(MiningConfiguration.class),
            mock(SyncState.class),
            mock(EthProtocolManager.class));

    assertThat(miningCoordinator).isInstanceOf(MigratingMiningCoordinator.class);
    final MigratingMiningCoordinator migratingMiningCoordinator =
        (MigratingMiningCoordinator) miningCoordinator;

    SoftAssertions.assertSoftly(
        (softly) -> {
          softly
              .assertThat(
                  migratingMiningCoordinator.getMiningCoordinatorSchedule().getFork(0L).getValue())
              .isSameAs(miningCoordinator1);
          softly
              .assertThat(
                  migratingMiningCoordinator.getMiningCoordinatorSchedule().getFork(4L).getValue())
              .isSameAs(miningCoordinator1);
          softly
              .assertThat(
                  migratingMiningCoordinator.getMiningCoordinatorSchedule().getFork(5L).getValue())
              .isSameAs(miningCoordinator2);
          softly
              .assertThat(
                  migratingMiningCoordinator.getMiningCoordinatorSchedule().getFork(6L).getValue())
              .isSameAs(miningCoordinator2);
        });
  }

  @Test
  public void createsMigratingContext() {
    final ConsensusContext context1 = mock(ConsensusContext.class);
    final ConsensusContext context2 = mock(ConsensusContext.class);

    final Map<Long, IdnControllerBuilder> idnControllerBuilderSchedule = new TreeMap<>();
    idnControllerBuilderSchedule.put(0L, idnControllerBuilder1);
    idnControllerBuilderSchedule.put(10L, idnControllerBuilder2);

    when(idnControllerBuilder1.createConsensusContext(any(), any(), any())).thenReturn(context1);
    when(idnControllerBuilder2.createConsensusContext(any(), any(), any())).thenReturn(context2);

    final ConsensusScheduleIdnControllerBuilder controllerBuilder =
        new ConsensusScheduleIdnControllerBuilder(idnControllerBuilderSchedule);
    final ConsensusContext consensusContext =
        controllerBuilder.createConsensusContext(
            mock(Blockchain.class), mock(WorldStateArchive.class), mock(ProtocolSchedule.class));

    assertThat(consensusContext).isInstanceOf(MigratingConsensusContext.class);
    final MigratingConsensusContext migratingConsensusContext =
        (MigratingConsensusContext) consensusContext;

    final ForksSchedule<ConsensusContext> contextSchedule =
        migratingConsensusContext.getConsensusContextSchedule();

    final NavigableSet<ForkSpec<ConsensusContext>> expectedConsensusContextSpecs =
        new TreeSet<>(ForkSpec.COMPARATOR);
    expectedConsensusContextSpecs.add(new ForkSpec<>(0L, context1));
    expectedConsensusContextSpecs.add(new ForkSpec<>(10L, context2));
    assertThat(contextSchedule.getForks()).isEqualTo(expectedConsensusContextSpecs);

    assertThat(contextSchedule.getFork(0).getValue()).isSameAs(context1);
    assertThat(contextSchedule.getFork(1).getValue()).isSameAs(context1);
    assertThat(contextSchedule.getFork(9).getValue()).isSameAs(context1);
    assertThat(contextSchedule.getFork(10).getValue()).isSameAs(context2);
    assertThat(contextSchedule.getFork(11).getValue()).isSameAs(context2);
  }
}
