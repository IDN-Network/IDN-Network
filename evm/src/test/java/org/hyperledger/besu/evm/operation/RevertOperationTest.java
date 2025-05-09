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
package org.idnecology.idn.evm.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.ConstantinopleGasCalculator;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevertOperationTest {

  @Mock private MessageFrame messageFrame;
  private final RevertOperation operation = new RevertOperation(new ConstantinopleGasCalculator());

  private final Bytes revertReasonBytes = Bytes.fromHexString("726576657274206d657373616765");

  @BeforeEach
  void setUp() {
    when(messageFrame.popStackItem())
        .thenReturn(UInt256.fromHexString("0x00"))
        .thenReturn(UInt256.fromHexString("0x0e"));
    when(messageFrame.readMemory(0, 14)).thenReturn(revertReasonBytes);
    when(messageFrame.memoryWordSize()).thenReturn(0);
    when(messageFrame.calculateMemoryExpansion(anyLong(), anyLong())).thenReturn(14L);
    when(messageFrame.getRemainingGas()).thenReturn(10_000L);
  }

  @Test
  void shouldReturnReason() {
    final ArgumentCaptor<Bytes> arg = ArgumentCaptor.forClass(Bytes.class);
    operation.execute(messageFrame, null);
    Mockito.verify(messageFrame).setRevertReason(arg.capture());
    assertThat(arg.getValue()).isEqualTo(revertReasonBytes);
  }
}
