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
package org.idnecology.idn.ethereum.api.graphql;

import org.idnecology.idn.ethereum.api.handlers.IsAliveHandler;
import org.idnecology.idn.ethereum.api.query.BlockchainQueries;
import org.idnecology.idn.ethereum.blockcreation.MiningCoordinator;
import org.idnecology.idn.ethereum.core.Synchronizer;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;

/**
 * Implementation of the GraphQLDataFetcherContext interface.
 *
 * <p>This class provides access to various components of the system such as the transaction pool,
 * blockchain queries, mining coordinator, synchronizer, and protocol schedule.
 */
public class GraphQLDataFetcherContextImpl implements GraphQLDataFetcherContext {

  private final BlockchainQueries blockchainQueries;
  private final MiningCoordinator miningCoordinator;
  private final Synchronizer synchronizer;
  private final ProtocolSchedule protocolSchedule;
  private final TransactionPool transactionPool;
  private final IsAliveHandler isAliveHandler;

  /**
   * Constructor that takes a GraphQLDataFetcherContext and an IsAliveHandler.
   *
   * @param context the GraphQLDataFetcherContext
   * @param isAliveHandler the IsAliveHandler
   */
  public GraphQLDataFetcherContextImpl(
      final GraphQLDataFetcherContext context, final IsAliveHandler isAliveHandler) {
    this(
        context.getBlockchainQueries(),
        context.getProtocolSchedule(),
        context.getTransactionPool(),
        context.getMiningCoordinator(),
        context.getSynchronizer(),
        isAliveHandler);
  }

  /**
   * Constructor that takes a BlockchainQueries, ProtocolSchedule, TransactionPool,
   * MiningCoordinator, and Synchronizer.
   *
   * @param blockchainQueries the BlockchainQueries
   * @param protocolSchedule the ProtocolSchedule
   * @param transactionPool the TransactionPool
   * @param miningCoordinator the MiningCoordinator
   * @param synchronizer the Synchronizer
   */
  public GraphQLDataFetcherContextImpl(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final MiningCoordinator miningCoordinator,
      final Synchronizer synchronizer) {
    this(
        blockchainQueries,
        protocolSchedule,
        transactionPool,
        miningCoordinator,
        synchronizer,
        new IsAliveHandler(true));
  }

  /**
   * Constructor that takes a BlockchainQueries, ProtocolSchedule, TransactionPool,
   * MiningCoordinator, Synchronizer, and IsAliveHandler.
   *
   * @param blockchainQueries the BlockchainQueries
   * @param protocolSchedule the ProtocolSchedule
   * @param transactionPool the TransactionPool
   * @param miningCoordinator the MiningCoordinator
   * @param synchronizer the Synchronizer
   * @param isAliveHandler the IsAliveHandler
   */
  public GraphQLDataFetcherContextImpl(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final MiningCoordinator miningCoordinator,
      final Synchronizer synchronizer,
      final IsAliveHandler isAliveHandler) {
    this.blockchainQueries = blockchainQueries;
    this.protocolSchedule = protocolSchedule;
    this.miningCoordinator = miningCoordinator;
    this.synchronizer = synchronizer;
    this.transactionPool = transactionPool;
    this.isAliveHandler = isAliveHandler;
  }

  @Override
  public TransactionPool getTransactionPool() {
    return transactionPool;
  }

  @Override
  public BlockchainQueries getBlockchainQueries() {
    return blockchainQueries;
  }

  @Override
  public MiningCoordinator getMiningCoordinator() {
    return miningCoordinator;
  }

  @Override
  public Synchronizer getSynchronizer() {
    return synchronizer;
  }

  @Override
  public ProtocolSchedule getProtocolSchedule() {
    return protocolSchedule;
  }

  @Override
  public IsAliveHandler getIsAliveHandler() {
    return isAliveHandler;
  }
}
