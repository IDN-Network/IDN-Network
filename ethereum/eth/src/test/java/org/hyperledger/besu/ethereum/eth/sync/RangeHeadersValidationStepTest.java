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
package org.idnecology.idn.ethereum.eth.sync;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.idnecology.idn.ethereum.mainnet.HeaderValidationMode.DETACHED_ONLY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.sync.range.RangeHeaders;
import org.idnecology.idn.ethereum.eth.sync.range.RangeHeadersValidationStep;
import org.idnecology.idn.ethereum.eth.sync.range.SyncTargetRange;
import org.idnecology.idn.ethereum.eth.sync.tasks.exceptions.InvalidBlockException;
import org.idnecology.idn.ethereum.mainnet.BlockHeaderValidator;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RangeHeadersValidationStepTest {
  @Mock private ProtocolSchedule protocolSchedule;
  @Mock private ProtocolSpec protocolSpec;
  @Mock private ProtocolContext protocolContext;
  @Mock private BlockHeaderValidator headerValidator;
  @Mock private ValidationPolicy validationPolicy;
  @Mock private EthPeer syncTarget;

  private final BlockDataGenerator gen = new BlockDataGenerator();
  private RangeHeadersValidationStep validationStep;

  private final BlockHeader rangeStart = gen.header(10);
  private final BlockHeader rangeEnd = gen.header(13);
  private final BlockHeader firstHeader = gen.header(11);
  private final RangeHeaders rangeHeaders =
      new RangeHeaders(
          new SyncTargetRange(syncTarget, rangeStart, rangeEnd),
          asList(firstHeader, gen.header(12), rangeEnd));

  public void setUp() {
    when(protocolSchedule.getByBlockHeader(any(BlockHeader.class))).thenReturn(protocolSpec);
    when(protocolSpec.getBlockHeaderValidator()).thenReturn(headerValidator);
    when(validationPolicy.getValidationModeForNextBlock()).thenReturn(DETACHED_ONLY);

    validationStep =
        new RangeHeadersValidationStep(protocolSchedule, protocolContext, validationPolicy);
  }

  @Test
  public void shouldValidateFirstHeaderAgainstRangeStartHeader() {
    setUp();
    when(headerValidator.validateHeader(firstHeader, rangeStart, protocolContext, DETACHED_ONLY))
        .thenReturn(true);
    final Stream<BlockHeader> result = validationStep.apply(rangeHeaders);

    verify(protocolSchedule).getByBlockHeader(firstHeader);
    verify(validationPolicy).getValidationModeForNextBlock();
    verify(headerValidator).validateHeader(firstHeader, rangeStart, protocolContext, DETACHED_ONLY);
    verifyNoMoreInteractions(headerValidator, validationPolicy);

    assertThat(result).containsExactlyElementsOf(rangeHeaders.getHeadersToImport());
  }

  @Test
  public void shouldThrowExceptionWhenValidationFails() {
    setUp();
    when(headerValidator.validateHeader(firstHeader, rangeStart, protocolContext, DETACHED_ONLY))
        .thenReturn(false);
    assertThatThrownBy(() -> validationStep.apply(rangeHeaders))
        .isInstanceOf(InvalidBlockException.class)
        .hasMessageContaining(
            "Invalid range headers.  Headers downloaded between #"
                + rangeStart.getNumber()
                + " ("
                + rangeStart.getHash()
                + ") and #"
                + rangeEnd.getNumber()
                + " ("
                + rangeEnd.getHash()
                + ") do not connect at #"
                + firstHeader.getNumber()
                + " ("
                + firstHeader.getHash()
                + ")");
  }

  @Test
  public void acceptResponseWithNoHeadersAndNoSetUp() {
    // don't run the setUp
    validationStep =
        new RangeHeadersValidationStep(protocolSchedule, protocolContext, validationPolicy);
    var emptyRangeHeaders =
        new RangeHeaders(new SyncTargetRange(syncTarget, rangeStart, rangeEnd), List.of());

    final Stream<BlockHeader> result = validationStep.apply(emptyRangeHeaders);
    assertThat(result).isEmpty();

    verifyNoMoreInteractions(
        protocolSchedule,
        protocolSpec,
        protocolContext,
        headerValidator,
        validationPolicy,
        syncTarget);
  }
}
