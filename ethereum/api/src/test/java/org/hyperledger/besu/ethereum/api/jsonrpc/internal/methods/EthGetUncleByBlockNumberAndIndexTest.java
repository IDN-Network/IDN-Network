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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResult;
import org.idnecology.idn.ethereum.api.query.BlockWithMetadata;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.api.query.TransactionWithMetadata;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockBody;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderTestFixture;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EthGetUncleByBlockNumberAndIndexTest {

  private final BlockHeaderTestFixture blockHeaderTestFixture = new BlockHeaderTestFixture();
  private final TransactionTestFixture transactionTestFixture = new TransactionTestFixture();

  private EthGetUncleByBlockNumberAndIndex method;

  @Mock private BlockchainQueries blockchainQueries;

  @BeforeEach
  public void before() {
    this.method = new EthGetUncleByBlockNumberAndIndex(blockchainQueries);
  }

  @Test
  public void methodShouldReturnExpectedName() {
    assertThat(method.getName()).isEqualTo("eth_getUncleByBlockNumberAndIndex");
  }

  @Test
  public void shouldReturnErrorWhenMissingBlockNumberParam() {
    final JsonRpcRequestContext request = getUncleByBlockNumberAndIndex(new Object[] {});

    final Throwable thrown = catchThrowable(() -> method.response(request));

    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid block parameter (index 0)");
  }

  @Test
  public void shouldReturnErrorWhenMissingIndexParam() {
    final JsonRpcRequestContext request = getUncleByBlockNumberAndIndex(new Object[] {"0x1"});

    final Throwable thrown = catchThrowable(() -> method.response(request));

    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Invalid block index (index 1)");
  }

  @Test
  public void shouldReturnNullResultWhenBlockDoesNotHaveOmmer() {
    final JsonRpcRequestContext request =
        getUncleByBlockNumberAndIndex(new Object[] {"0x1", "0x0"});
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, null);

    when(blockchainQueries.getOmmer(eq(1L), eq(0))).thenReturn(Optional.empty());

    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  @Test
  public void shouldReturnExpectedBlockResult() {
    final JsonRpcRequestContext request =
        getUncleByBlockNumberAndIndex(new Object[] {"0x1", "0x0"});
    final BlockHeader header = blockHeaderTestFixture.baseFeePerGas(Wei.of(7L)).buildHeader();
    final BlockResult expectedBlockResult = blockResult(header);
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, expectedBlockResult);

    when(blockchainQueries.getOmmer(eq(1L), eq(0))).thenReturn(Optional.of(header));

    final JsonRpcResponse response = method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  private BlockResult blockResult(final BlockHeader header) {
    final Block block =
        new Block(header, new BlockBody(Collections.emptyList(), Collections.emptyList()));
    return new BlockResult(
        header,
        Collections.emptyList(),
        Collections.emptyList(),
        Difficulty.ZERO,
        block.calculateSize());
  }

  private JsonRpcRequestContext getUncleByBlockNumberAndIndex(final Object[] params) {
    return new JsonRpcRequestContext(
        new JsonRpcRequest("2.0", "eth_getUncleByBlockNumberAndIndex", params));
  }

  public BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetadata(
      final BlockHeader header) {
    final KeyPair keyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
    final List<TransactionWithMetadata> transactions = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      final Transaction transaction = transactionTestFixture.createTransaction(keyPair);
      transactions.add(
          new TransactionWithMetadata(
              transaction, header.getNumber(), Optional.empty(), header.getHash(), 0));
    }

    final List<Hash> ommers = new ArrayList<>();
    ommers.add(Hash.ZERO);

    return new BlockWithMetadata<>(header, transactions, ommers, header.getDifficulty(), 0);
  }
}
