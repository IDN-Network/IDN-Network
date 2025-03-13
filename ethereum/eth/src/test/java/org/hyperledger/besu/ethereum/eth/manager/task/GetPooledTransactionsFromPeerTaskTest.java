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
package org.idnecology.idn.ethereum.eth.manager.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.ethtaskutils.PeerMessageTaskTest;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.plugin.services.MetricsSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

public class GetPooledTransactionsFromPeerTaskTest extends PeerMessageTaskTest<List<Transaction>> {

  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Override
  protected List<Transaction> generateDataToBeRequested() {
    final List<Transaction> requestedData = new ArrayList<>(3);
    for (int i = 0; i < 3; i++) {
      Transaction tx =
          new TransactionTestFixture()
              .nonce(genesisAccountNonce + i)
              .gasPrice(Wei.of(2000))
              .gasLimit(100000)
              .chainId(Optional.empty())
              .createTransaction(genesisAccountKeyPair);
      assertThat(transactionPool.addTransactionViaApi(tx).isValid()).isTrue();
      requestedData.add(tx);
    }
    return requestedData;
  }

  @Override
  protected EthTask<AbstractPeerTask.PeerTaskResult<List<Transaction>>> createTask(
      final List<Transaction> requestedData) {
    final List<Hash> hashes =
        Lists.newArrayList(requestedData).stream()
            .map(Transaction::getHash)
            .collect(Collectors.toList());
    return GetPooledTransactionsFromPeerTask.forHashes(ethContext, hashes, metricsSystem);
  }

  @Override
  protected void assertPartialResultMatchesExpectation(
      final List<Transaction> requestedData, final List<Transaction> partialResponse) {
    assertThat(partialResponse.size()).isLessThanOrEqualTo(requestedData.size());
    assertThat(partialResponse.size()).isGreaterThan(0);
    for (Transaction data : partialResponse) {
      assertThat(requestedData).contains(data);
    }
  }

  @Override
  protected void assertResultMatchesExpectation(
      final List<Transaction> requestedData,
      final AbstractPeerTask.PeerTaskResult<List<Transaction>> response,
      final EthPeer respondingPeer) {
    assertThat(response.getResult().size()).isEqualTo(requestedData.size());
    for (Transaction data : response.getResult()) {
      assertThat(requestedData).contains(data);
    }
  }
}
