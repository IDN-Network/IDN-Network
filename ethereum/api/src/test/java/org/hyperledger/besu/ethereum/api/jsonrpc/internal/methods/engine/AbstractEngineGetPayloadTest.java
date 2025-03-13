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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.consensus.merge.MergeContext;
import org.idnecology.idn.consensus.merge.PayloadWrapper;
import org.idnecology.idn.consensus.merge.blockcreation.MergeMiningCoordinator;
import org.idnecology.idn.consensus.merge.blockcreation.PayloadIdentifier;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.BlockWithReceipts;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractEngineGetPayloadTest extends AbstractScheduledApiTest {

  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);
  protected static final KeyPair senderKeys = SIGNATURE_ALGORITHM.get().generateKeyPair();

  @FunctionalInterface
  interface MethodFactory {
    AbstractEngineGetPayload create(
        final Vertx vertx,
        final ProtocolContext protocolContext,
        final MergeMiningCoordinator mergeCoordinator,
        final BlockResultFactory ethPeers,
        final EngineCallListener engineCallListener);
  }

  private final Optional<MethodFactory> methodFactory;
  protected AbstractEngineGetPayload method;

  public AbstractEngineGetPayloadTest(final MethodFactory methodFactory) {
    this.methodFactory = Optional.of(methodFactory);
  }

  public AbstractEngineGetPayloadTest() {
    this.methodFactory = Optional.empty();
  }

  protected static final Vertx vertx = Vertx.vertx();
  protected static final BlockResultFactory factory = new BlockResultFactory();
  protected static final PayloadIdentifier mockPid =
      PayloadIdentifier.forPayloadParams(
          Hash.ZERO,
          1337L,
          Bytes32.random(),
          Address.fromHexString("0x42"),
          Optional.empty(),
          Optional.empty());
  protected static final BlockHeader mockHeader =
      new BlockHeaderTestFixture().prevRandao(Bytes32.random()).buildHeader();
  private static final Block mockBlock =
      new Block(mockHeader, new BlockBody(Collections.emptyList(), Collections.emptyList()));
  protected static final BlockWithReceipts mockBlockWithReceipts =
      new BlockWithReceipts(mockBlock, Collections.emptyList());
  protected static final PayloadWrapper mockPayload =
      new PayloadWrapper(mockPid, mockBlockWithReceipts, Optional.empty());
  private static final Block mockBlockWithWithdrawals =
      new Block(
          mockHeader,
          new BlockBody(
              Collections.emptyList(),
              Collections.emptyList(),
              Optional.of(Collections.emptyList())));
  private static final Block mockBlockWithDepositRequests =
      new Block(
          mockHeader,
          new BlockBody(Collections.emptyList(), Collections.emptyList(), Optional.empty()));
  protected static final BlockWithReceipts mockBlockWithReceiptsAndWithdrawals =
      new BlockWithReceipts(mockBlockWithWithdrawals, Collections.emptyList());
  protected static final PayloadWrapper mockPayloadWithWithdrawals =
      new PayloadWrapper(mockPid, mockBlockWithReceiptsAndWithdrawals, Optional.empty());

  protected static final BlockWithReceipts mockBlockWithReceiptsAndDepositRequests =
      new BlockWithReceipts(mockBlockWithDepositRequests, Collections.emptyList());
  protected static final PayloadWrapper mockPayloadWithDepositRequests =
      new PayloadWrapper(mockPid, mockBlockWithReceiptsAndDepositRequests, Optional.empty());

  @Mock protected ProtocolContext protocolContext;

  @Mock protected MergeContext mergeContext;
  @Mock protected MergeMiningCoordinator mergeMiningCoordinator;

  @Mock protected EngineCallListener engineCallListener;

  @BeforeEach
  @Override
  public void before() {
    super.before();
    when(mergeContext.retrievePayloadById(mockPid)).thenReturn(Optional.of(mockPayload));
    when(protocolContext.safeConsensusContext(Mockito.any())).thenReturn(Optional.of(mergeContext));
    if (methodFactory.isPresent()) {
      this.method =
          methodFactory
              .get()
              .create(vertx, protocolContext, mergeMiningCoordinator, factory, engineCallListener);
    }
  }

  @Test
  public abstract void shouldReturnExpectedMethodName();

  @Test
  public abstract void shouldReturnBlockForKnownPayloadId();

  @Test
  public void shouldFailForUnknownPayloadId() {
    final var resp =
        resp(
            getMethodName(),
            PayloadIdentifier.forPayloadParams(
                Hash.ZERO,
                0L,
                Bytes32.random(),
                Address.fromHexString("0x42"),
                Optional.empty(),
                Optional.empty()));
    assertThat(resp).isInstanceOf(JsonRpcErrorResponse.class);
    verify(engineCallListener, times(1)).executionEngineCalled();
  }

  abstract String getMethodName();

  protected JsonRpcResponse resp(final String methodName, final PayloadIdentifier pid) {
    return method.response(
        new JsonRpcRequestContext(
            new JsonRpcRequest("2.0", methodName, new Object[] {pid.serialize()})));
  }
}
