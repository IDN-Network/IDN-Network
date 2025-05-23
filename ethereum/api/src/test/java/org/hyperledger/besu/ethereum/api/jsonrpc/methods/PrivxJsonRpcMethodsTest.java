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
package org.idnecology.idn.ethereum.api.jsonrpc.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod.PRIVX_FIND_PRIVACY_GROUP;
import static org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod.PRIVX_FIND_PRIVACY_GROUP_OLD;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.privx.PrivxFindFlexiblePrivacyGroup;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.privx.PrivxFindOnchainPrivacyGroup;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PrivxJsonRpcMethodsTest {

  @Mock private BlockchainQueries blockchainQueries;
  @Mock private ProtocolSchedule protocolSchedule;
  @Mock private TransactionPool transactionPool;
  @Mock private PrivacyParameters privacyParameters;

  private PrivxJsonRpcMethods privxJsonRpcMethods;

  @BeforeEach
  public void setup() {
    privxJsonRpcMethods =
        new PrivxJsonRpcMethods(
            blockchainQueries, protocolSchedule, transactionPool, privacyParameters);

    lenient().when(privacyParameters.isEnabled()).thenReturn(true);
  }

  @Test
  public void privxFindPrivacyGroupMethodIsDisabledWhenFlexiblePrivacyGroupIsDisabled() {
    when(privacyParameters.isFlexiblePrivacyGroupsEnabled()).thenReturn(false);
    final Map<String, JsonRpcMethod> rpcMethods = privxJsonRpcMethods.create();
    final JsonRpcMethod method = rpcMethods.get(PRIVX_FIND_PRIVACY_GROUP.getMethodName());

    assertThat(method).isNull();
  }

  @Test
  public void privxFindPrivacyGroupMethodIsEnabledWhenFlexiblePrivacyGroupIsEnabled() {
    when(privacyParameters.isFlexiblePrivacyGroupsEnabled()).thenReturn(true);
    final Map<String, JsonRpcMethod> rpcMethods = privxJsonRpcMethods.create();
    final JsonRpcMethod method = rpcMethods.get(PRIVX_FIND_PRIVACY_GROUP.getMethodName());

    assertThat(method).isNotNull();
    assertThat(method).isInstanceOf(PrivxFindFlexiblePrivacyGroup.class);
  }

  @Deprecated
  @Test
  public void privxFindOnchainPrivacyGroupMethodIsStillEnabled() {
    when(privacyParameters.isFlexiblePrivacyGroupsEnabled()).thenReturn(true);
    final Map<String, JsonRpcMethod> rpcMethods = privxJsonRpcMethods.create();
    final JsonRpcMethod method = rpcMethods.get(PRIVX_FIND_PRIVACY_GROUP_OLD.getMethodName());

    assertThat(method).isNotNull();
    assertThat(method).isInstanceOf(PrivxFindOnchainPrivacyGroup.class);
  }
}
