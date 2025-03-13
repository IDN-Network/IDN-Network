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
package org.idnecology.idn.ethereum.mainnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.InMemoryKeyValueStorageProvider;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.mainnet.systemcall.BlockProcessingContext;
import org.idnecology.idn.ethereum.mainnet.systemcall.SystemCallProcessor;
import org.idnecology.idn.evm.blockhash.BlockHashLookup;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.processor.AbstractMessageProcessor;
import org.idnecology.idn.evm.processor.MessageCallProcessor;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MainnetBlockContextProcessorTest {
  private static final Address CALL_ADDRESS = Address.fromHexString("0x1");
  private static final Bytes EXPECTED_OUTPUT = Bytes.fromHexString("0x01");
  private ProcessableBlockHeader mockBlockHeader;
  private MainnetTransactionProcessor mockTransactionProcessor;
  private BlockHashLookup mockBlockHashLookup;
  private AbstractMessageProcessor mockMessageCallProcessor;

  @BeforeEach
  public void setUp() {
    mockBlockHeader = mock(ProcessableBlockHeader.class);
    mockTransactionProcessor = mock(MainnetTransactionProcessor.class);
    mockMessageCallProcessor = mock(MessageCallProcessor.class);
    mockBlockHashLookup = mock(BlockHashLookup.class);
    when(mockTransactionProcessor.getMessageProcessor(any())).thenReturn(mockMessageCallProcessor);
  }

  @Test
  void shouldProcessSuccessfully() {
    doAnswer(
            invocation -> {
              MessageFrame messageFrame = invocation.getArgument(0);
              messageFrame.setOutputData(EXPECTED_OUTPUT);
              messageFrame.getMessageFrameStack().pop();
              messageFrame.setState(MessageFrame.State.COMPLETED_SUCCESS);
              return null;
            })
        .when(mockMessageCallProcessor)
        .process(any(), any());
    final MutableWorldState worldState = createWorldState(CALL_ADDRESS);
    Bytes actualOutput = processSystemCall(worldState);
    assertThat(actualOutput).isEqualTo(EXPECTED_OUTPUT);
  }

  @Test
  void shouldThrowExceptionOnFailedExecution() {
    doAnswer(
            invocation -> {
              MessageFrame messageFrame = invocation.getArgument(0);
              messageFrame.getMessageFrameStack().pop();
              messageFrame.setState(MessageFrame.State.COMPLETED_FAILED);
              return null;
            })
        .when(mockMessageCallProcessor)
        .process(any(), any());
    final MutableWorldState worldState = createWorldState(CALL_ADDRESS);
    var exception = assertThrows(RuntimeException.class, () -> processSystemCall(worldState));
    assertThat(exception.getMessage()).isEqualTo("System call did not execute to completion");
  }

  @Test
  void shouldReturnEmptyWhenContractDoesNotExist() {
    final MutableWorldState worldState = InMemoryKeyValueStorageProvider.createInMemoryWorldState();
    Bytes actualOutput = processSystemCall(worldState);
    assertThat(actualOutput).isEqualTo(Bytes.EMPTY);
  }

  Bytes processSystemCall(final MutableWorldState worldState) {
    SystemCallProcessor systemCallProcessor = new SystemCallProcessor(mockTransactionProcessor);

    BlockProcessingContext blockProcessingContext =
        new BlockProcessingContext(
            mockBlockHeader,
            worldState,
            mock(ProtocolSpec.class),
            mockBlockHashLookup,
            OperationTracer.NO_TRACING);

    when(mockBlockHashLookup.apply(any(), any())).thenReturn(Hash.EMPTY);
    return systemCallProcessor.process(CALL_ADDRESS, blockProcessingContext, Bytes.EMPTY);
  }

  private MutableWorldState createWorldState(final Address address) {
    final MutableWorldState worldState = InMemoryKeyValueStorageProvider.createInMemoryWorldState();
    final WorldUpdater updater = worldState.updater();
    updater.getOrCreate(address);
    updater.commit();
    return worldState;
  }
}
