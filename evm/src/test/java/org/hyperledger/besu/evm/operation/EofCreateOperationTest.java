/*
 * Copyright contributors to Idn ecology Idn.
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
import static org.assertj.core.api.Assertions.fail;
import static org.idnecology.idn.evm.EOFTestConstants.EOF_CREATE_CONTRACT;
import static org.idnecology.idn.evm.EOFTestConstants.INNER_CONTRACT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.evm.Code;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.MainnetEVMs;
import org.idnecology.idn.evm.account.MutableAccount;
import org.idnecology.idn.evm.code.CodeInvalid;
import org.idnecology.idn.evm.frame.BlockValues;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.log.Log;
import org.idnecology.idn.evm.precompile.MainnetPrecompiledContracts;
import org.idnecology.idn.evm.processor.ContractCreationProcessor;
import org.idnecology.idn.evm.processor.MessageCallProcessor;
import org.idnecology.idn.evm.tracing.OperationTracer;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;

class EofCreateOperationTest {

  private final WorldUpdater worldUpdater = mock(WorldUpdater.class);
  private final MutableAccount account = mock(MutableAccount.class);
  private final MutableAccount newAccount = mock(MutableAccount.class);

  static final Bytes CALL_DATA =
      Bytes.fromHexString(
          "cafebaba600dbaadc0de57aff60061e5cafebaba600dbaadc0de57aff60061e5"); // 32 bytes

  public static final String SENDER = "0xdeadc0de00000000000000000000000000000000";

  //  private static final int SHANGHAI_CREATE_GAS = 41240;

  @Test
  void innerContractIsCorrect() {
    final EVM evm = MainnetEVMs.osaka(EvmConfiguration.DEFAULT);
    Code code = evm.getCodeUncached(INNER_CONTRACT);
    assertThat(code.isValid()).isTrue();

    final MessageFrame messageFrame = testMemoryFrame(code, CALL_DATA);

    when(account.getNonce()).thenReturn(55L);
    when(account.getBalance()).thenReturn(Wei.ZERO);
    when(worldUpdater.getAccount(any())).thenReturn(account);
    when(worldUpdater.get(any())).thenReturn(account);
    when(worldUpdater.getSenderAccount(any())).thenReturn(account);
    when(worldUpdater.getOrCreate(any())).thenReturn(newAccount);
    when(newAccount.getCode()).thenReturn(Bytes.EMPTY);
    when(newAccount.isStorageEmpty()).thenReturn(true);
    when(worldUpdater.updater()).thenReturn(worldUpdater);

    final MessageFrame createFrame = messageFrame.getMessageFrameStack().peek();
    assertThat(createFrame).isNotNull();
    final ContractCreationProcessor ccp =
        new ContractCreationProcessor(evm, false, List.of(), 0, List.of());
    ccp.process(createFrame, OperationTracer.NO_TRACING);

    final Log log = createFrame.getLogs().get(0);
    final Bytes calculatedTopic = log.getTopics().get(0);
    assertThat(calculatedTopic).isEqualTo(CALL_DATA);
  }

  @Test
  void eofCreatePassesInCallData() {
    Bytes outerContract = EOF_CREATE_CONTRACT;
    final EVM evm = MainnetEVMs.osaka(EvmConfiguration.DEFAULT);

    Code code = evm.getCodeUncached(outerContract);
    if (!code.isValid()) {
      System.out.println(outerContract);
      fail(((CodeInvalid) code).getInvalidReason());
    }

    final MessageFrame messageFrame = testMemoryFrame(code, CALL_DATA);

    when(account.getNonce()).thenReturn(55L);
    when(account.getBalance()).thenReturn(Wei.ZERO);
    when(worldUpdater.getAccount(any())).thenReturn(account);
    when(worldUpdater.get(any())).thenReturn(account);
    when(worldUpdater.getSenderAccount(any())).thenReturn(account);
    when(worldUpdater.getOrCreate(any())).thenReturn(newAccount);
    when(newAccount.getCode()).thenReturn(Bytes.EMPTY);
    when(newAccount.isStorageEmpty()).thenReturn(true);
    when(worldUpdater.updater()).thenReturn(worldUpdater);

    var precompiles = MainnetPrecompiledContracts.prague(evm.getGasCalculator());
    final MessageFrame createFrame = messageFrame.getMessageFrameStack().peek();
    assertThat(createFrame).isNotNull();
    final MessageCallProcessor mcp = new MessageCallProcessor(evm, precompiles);
    final ContractCreationProcessor ccp =
        new ContractCreationProcessor(evm, false, List.of(), 0, List.of());
    while (!createFrame.getMessageFrameStack().isEmpty()) {
      var frame = createFrame.getMessageFrameStack().peek();
      assert frame != null;
      (switch (frame.getType()) {
            case CONTRACT_CREATION -> ccp;
            case MESSAGE_CALL -> mcp;
          })
          .process(frame, OperationTracer.NO_TRACING);
    }

    final Log log = createFrame.getLogs().get(0);
    final String calculatedTopic = log.getTopics().get(0).slice(0, 2).toHexString();
    assertThat(calculatedTopic).isEqualTo("0xc0de");

    assertThat(createFrame.getCreates())
        .containsExactly(Address.fromHexString("0x8c308e96997a8052e3aaab5af624cb827218687a"));
  }

  private MessageFrame testMemoryFrame(final Code code, final Bytes initData) {
    return MessageFrame.builder()
        .type(MessageFrame.Type.MESSAGE_CALL)
        .contract(Address.ZERO)
        .inputData(initData)
        .sender(Address.fromHexString(SENDER))
        .value(Wei.ZERO)
        .apparentValue(Wei.ZERO)
        .code(code)
        .completer(__ -> {})
        .address(Address.fromHexString(SENDER))
        .blockHashLookup((__, ___) -> Hash.ZERO)
        .blockValues(mock(BlockValues.class))
        .gasPrice(Wei.ZERO)
        .miningBeneficiary(Address.ZERO)
        .originator(Address.ZERO)
        .initialGas(100000L)
        .worldUpdater(worldUpdater)
        .build();
  }
}
