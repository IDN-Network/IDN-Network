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
package org.idnecology.idn.ethereum.blockcreation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.PendingTransactions;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.GasPricePendingTransactionsSorter;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.testutil.TestClock;
import org.idnecology.idn.util.number.Fraction;

import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Disabled;

@Disabled("flaky https://github.com/hyperledger/idn/issues/8238")
public class LegacyFeeMarketBlockTransactionSelectorTest
    extends AbstractBlockTransactionSelectorTest {

  @Override
  protected GenesisConfig getGenesisConfig() {
    return GenesisConfig.fromResource("/block-transaction-selector/gas-price-genesis.json");
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return new ProtocolScheduleBuilder(
            genesisConfig.getConfigOptions(),
            Optional.of(CHAIN_ID),
            ProtocolSpecAdapters.create(0, Function.identity()),
            new PrivacyParameters(),
            false,
            EvmConfiguration.DEFAULT,
            MiningConfiguration.MINING_DISABLED,
            new BadBlockManager(),
            false,
            new NoOpMetricsSystem())
        .createProtocolSchedule();
  }

  @Override
  protected TransactionPool createTransactionPool() {
    final TransactionPoolConfiguration poolConf =
        ImmutableTransactionPoolConfiguration.builder()
            .txPoolMaxSize(5)
            .txPoolLimitByAccountPercentage(Fraction.fromFloat(1f))
            .minGasPrice(Wei.ONE)
            .build();

    final PendingTransactions pendingTransactions =
        new GasPricePendingTransactionsSorter(
            poolConf,
            TestClock.system(ZoneId.systemDefault()),
            metricsSystem,
            blockchain::getChainHeadHeader);

    final EthContext ethContext = mock(EthContext.class, RETURNS_DEEP_STUBS);
    when(ethContext.getEthPeers().subscribeConnect(any())).thenReturn(1L);

    final TransactionPool transactionPool =
        new TransactionPool(
            () -> pendingTransactions,
            protocolSchedule,
            protocolContext,
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(metricsSystem),
            poolConf,
            new BlobCache());
    transactionPool.setEnabled();
    return transactionPool;
  }
}
