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

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.ethereum.mainnet.requests.RequestContractAddresses.DEFAULT_DEPOSIT_CONTRACT_ADDRESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.crypto.KeyPair;
import org.idnecology.idn.crypto.SECPPrivateKey;
import org.idnecology.idn.crypto.SignatureAlgorithm;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.BlobGas;
import org.idnecology.idn.datatypes.BlobsWithCommitments;
import org.idnecology.idn.datatypes.GWei;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.RequestType;
import org.idnecology.idn.datatypes.TransactionType;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.blockcreation.BlockCreator.BlockCreationResult;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.BlobTestFixture;
import org.idnecology.idn.ethereum.core.BlockDataGenerator;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockHeaderBuilder;
import org.idnecology.idn.ethereum.core.Difficulty;
import org.idnecology.idn.ethereum.core.ExecutionContextTestFixture;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration;
import org.idnecology.idn.ethereum.core.ImmutableMiningConfiguration.MutableInitValues;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.core.Request;
import org.idnecology.idn.ethereum.core.SealableBlockHeader;
import org.idnecology.idn.ethereum.core.Transaction;
import org.idnecology.idn.ethereum.core.TransactionReceipt;
import org.idnecology.idn.ethereum.core.TransactionTestFixture;
import org.idnecology.idn.ethereum.core.Withdrawal;
import org.idnecology.idn.ethereum.eth.manager.EthContext;
import org.idnecology.idn.ethereum.eth.manager.EthScheduler;
import org.idnecology.idn.ethereum.eth.transactions.BlobCache;
import org.idnecology.idn.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionBroadcaster;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPoolMetrics;
import org.idnecology.idn.ethereum.eth.transactions.sorter.AbstractPendingTransactionsSorter;
import org.idnecology.idn.ethereum.eth.transactions.sorter.GasPricePendingTransactionsSorter;
import org.idnecology.idn.ethereum.mainnet.BodyValidation;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.mainnet.TransactionValidator;
import org.idnecology.idn.ethereum.mainnet.TransactionValidatorFactory;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.mainnet.WithdrawalsProcessor;
import org.idnecology.idn.ethereum.mainnet.requests.DepositRequestProcessor;
import org.idnecology.idn.ethereum.mainnet.requests.RequestProcessingContext;
import org.idnecology.idn.ethereum.mainnet.systemcall.BlockProcessingContext;
import org.idnecology.idn.ethereum.transaction.TransactionInvalidReason;
import org.idnecology.idn.evm.account.Account;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.log.Log;
import org.idnecology.idn.evm.log.LogTopic;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.testutil.DeterministicEthScheduler;

import java.math.BigInteger;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class AbstractBlockCreatorTest {
  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);
  private static final SECPPrivateKey PRIVATE_KEY1 =
      SIGNATURE_ALGORITHM
          .get()
          .createPrivateKey(
              Bytes32.fromHexString(
                  "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63"));
  private static final KeyPair KEYS1 =
      new KeyPair(PRIVATE_KEY1, SIGNATURE_ALGORITHM.get().createPublicKey(PRIVATE_KEY1));

  @Mock private WithdrawalsProcessor withdrawalsProcessor;
  protected EthScheduler ethScheduler = new DeterministicEthScheduler();

  @Test
  void findDepositRequestsFromReceipts() {
    BlockDataGenerator blockDataGenerator = new BlockDataGenerator();
    TransactionReceipt receiptWithoutDeposit1 = blockDataGenerator.receipt();
    TransactionReceipt receiptWithoutDeposit2 = blockDataGenerator.receipt();
    final Log depositLog =
        new Log(
            DEFAULT_DEPOSIT_CONTRACT_ADDRESS,
            Bytes.fromHexString(
                "0x00000000000000000000000000000000000000000000000000000000000000a000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000140000000000000000000000000000000000000000000000000000000000000018000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000030b10a4a15bf67b328c9b101d09e5c6ee6672978fdad9ef0d9e2ceffaee99223555d8601f0cb3bcc4ce1af9864779a416e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200017a7fcf06faf493d30bbe2632ea7c2383cd86825e12797165de7aa35589483000000000000000000000000000000000000000000000000000000000000000800405973070000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000060a889db8300194050a2636c92a95bc7160515867614b7971a9500cdb62f9c0890217d2901c3241f86fac029428fc106930606154bd9e406d7588934a5f15b837180b17194d6e44bd6de23e43b163dfe12e369dcc75a3852cd997963f158217eb500000000000000000000000000000000000000000000000000000000000000083f3d080000000000000000000000000000000000000000000000000000000000"),
            List.of(
                LogTopic.fromHexString(
                    "0x649bbc62d0e31342afea4e5cd82d4049e7e1ee912fc0889aa790803be39038c5")));
    final TransactionReceipt receiptWithDeposit = blockDataGenerator.receipt(List.of(depositLog));
    List<TransactionReceipt> receipts =
        List.of(receiptWithoutDeposit1, receiptWithDeposit, receiptWithoutDeposit2);

    Request expectedDepositRequest =
        new Request(
            RequestType.DEPOSIT,
            Bytes.fromHexString(
                "0xb10a4a15bf67b328c9b101d09e5c6ee6672978fdad9ef0d9e2ceffaee99223555d8601f0cb3bcc4ce1af9864779a416e0017a7fcf06faf493d30bbe2632ea7c2383cd86825e12797165de7aa355894830040597307000000a889db8300194050a2636c92a95bc7160515867614b7971a9500cdb62f9c0890217d2901c3241f86fac029428fc106930606154bd9e406d7588934a5f15b837180b17194d6e44bd6de23e43b163dfe12e369dcc75a3852cd997963f158217eb53f3d080000000000"));

    var depositRequestsFromReceipts =
        new DepositRequestProcessor(DEFAULT_DEPOSIT_CONTRACT_ADDRESS)
            .process(
                new RequestProcessingContext(
                    new BlockProcessingContext(null, null, null, null, null), receipts));
    assertThat(depositRequestsFromReceipts).isEqualTo(expectedDepositRequest);
  }

  @Test
  void withProcessorAndEmptyWithdrawals_NoWithdrawalsAreProcessed() {
    final CreateOn miningOn = blockCreatorWithWithdrawalsProcessor();
    final AbstractBlockCreator blockCreator = miningOn.blockCreator;
    final BlockCreationResult blockCreationResult =
        blockCreator.createBlock(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            1L,
            false,
            miningOn.parentHeader);
    verify(withdrawalsProcessor, never()).processWithdrawals(any(), any());
    assertThat(blockCreationResult.getBlock().getHeader().getWithdrawalsRoot()).isEmpty();
    assertThat(blockCreationResult.getBlock().getBody().getWithdrawals()).isEmpty();
  }

  @Test
  void withNoProcessorAndEmptyWithdrawals_NoWithdrawalsAreNotProcessed() {
    final CreateOn miningOn = blockCreatorWithoutWithdrawalsProcessor();
    final AbstractBlockCreator blockCreator = miningOn.blockCreator;
    final BlockCreationResult blockCreationResult =
        blockCreator.createBlock(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            1L,
            false,
            miningOn.parentHeader);
    verify(withdrawalsProcessor, never()).processWithdrawals(any(), any());
    assertThat(blockCreationResult.getBlock().getHeader().getWithdrawalsRoot()).isEmpty();
    assertThat(blockCreationResult.getBlock().getBody().getWithdrawals()).isEmpty();
  }

  @Test
  void withProcessorAndWithdrawals_WithdrawalsAreProcessed() {
    final CreateOn miningOn = blockCreatorWithWithdrawalsProcessor();
    final AbstractBlockCreator blockCreator = miningOn.blockCreator;
    final List<Withdrawal> withdrawals =
        List.of(new Withdrawal(UInt64.ONE, UInt64.ONE, Address.fromHexString("0x1"), GWei.ONE));
    final BlockCreationResult blockCreationResult =
        blockCreator.createBlock(
            Optional.empty(),
            Optional.empty(),
            Optional.of(withdrawals),
            Optional.empty(),
            Optional.empty(),
            1L,
            false,
            miningOn.parentHeader);

    final Hash withdrawalsRoot = BodyValidation.withdrawalsRoot(withdrawals);
    verify(withdrawalsProcessor).processWithdrawals(eq(withdrawals), any());
    assertThat(blockCreationResult.getBlock().getHeader().getWithdrawalsRoot())
        .hasValue(withdrawalsRoot);
    assertThat(blockCreationResult.getBlock().getBody().getWithdrawals()).hasValue(withdrawals);
  }

  @Test
  void withNoProcessorAndWithdrawals_WithdrawalsAreNotProcessed() {
    final CreateOn miningOn = blockCreatorWithoutWithdrawalsProcessor();
    final AbstractBlockCreator blockCreator = miningOn.blockCreator;
    final List<Withdrawal> withdrawals =
        List.of(new Withdrawal(UInt64.ONE, UInt64.ONE, Address.fromHexString("0x1"), GWei.ONE));
    final BlockCreationResult blockCreationResult =
        blockCreator.createBlock(
            Optional.empty(),
            Optional.empty(),
            Optional.of(withdrawals),
            Optional.empty(),
            Optional.empty(),
            1L,
            false,
            miningOn.parentHeader);
    verify(withdrawalsProcessor, never()).processWithdrawals(any(), any());
    assertThat(blockCreationResult.getBlock().getHeader().getWithdrawalsRoot()).isEmpty();
    assertThat(blockCreationResult.getBlock().getBody().getWithdrawals()).isEmpty();
  }

  @Test
  public void computesGasUsageFromIncludedTransactions() {
    final CreateOn miningOn = blockCreatorWithBlobGasSupport();
    final AbstractBlockCreator blockCreator = miningOn.blockCreator;
    BlobTestFixture blobTestFixture = new BlobTestFixture();
    BlobsWithCommitments bwc = blobTestFixture.createBlobsWithCommitments(6);
    TransactionTestFixture ttf = new TransactionTestFixture();
    Transaction fullOfBlobs =
        ttf.to(Optional.of(Address.ZERO))
            .type(TransactionType.BLOB)
            .chainId(Optional.of(BigInteger.valueOf(42)))
            .gasLimit(21000)
            .maxFeePerGas(Optional.of(Wei.of(15)))
            .maxFeePerBlobGas(Optional.of(Wei.of(128)))
            .maxPriorityFeePerGas(Optional.of(Wei.of(1)))
            .versionedHashes(Optional.of(bwc.getVersionedHashes()))
            .blobsWithCommitments(Optional.of(bwc))
            .createTransaction(KEYS1);

    final BlockCreationResult blockCreationResult =
        blockCreator.createBlock(
            Optional.of(List.of(fullOfBlobs)),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            1L,
            false,
            miningOn.parentHeader);
    long blobGasUsage = blockCreationResult.getBlock().getHeader().getGasUsed();
    assertThat(blobGasUsage).isNotZero();
    BlobGas excessBlobGas = blockCreationResult.getBlock().getHeader().getExcessBlobGas().get();
    assertThat(excessBlobGas).isNotNull();
  }

  private CreateOn blockCreatorWithBlobGasSupport() {
    final var alwaysValidTransactionValidatorFactory = mock(TransactionValidatorFactory.class);
    when(alwaysValidTransactionValidatorFactory.get())
        .thenReturn(new AlwaysValidTransactionValidator());
    final ProtocolSpecAdapters protocolSpecAdapters =
        ProtocolSpecAdapters.create(
            0,
            specBuilder -> {
              specBuilder.isReplayProtectionSupported(true);
              specBuilder.withdrawalsProcessor(withdrawalsProcessor);
              specBuilder.transactionValidatorFactoryBuilder(
                  (evm, gasLimitCalculator, feeMarket) -> alwaysValidTransactionValidatorFactory);
              return specBuilder;
            });
    return createBlockCreator(protocolSpecAdapters);
  }

  private CreateOn blockCreatorWithWithdrawalsProcessor() {
    final ProtocolSpecAdapters protocolSpecAdapters =
        ProtocolSpecAdapters.create(
            0, specBuilder -> specBuilder.withdrawalsProcessor(withdrawalsProcessor));
    return createBlockCreator(protocolSpecAdapters);
  }

  private CreateOn blockCreatorWithoutWithdrawalsProcessor() {
    final ProtocolSpecAdapters protocolSpecAdapters =
        ProtocolSpecAdapters.create(0, specBuilder -> specBuilder.withdrawalsProcessor(null));
    return createBlockCreator(protocolSpecAdapters);
  }

  record CreateOn(AbstractBlockCreator blockCreator, BlockHeader parentHeader) {}

  private CreateOn createBlockCreator(final ProtocolSpecAdapters protocolSpecAdapters) {

    final var genesisConfig = GenesisConfig.fromResource("/block-creation-genesis.json");
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder(genesisConfig)
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfig.getConfigOptions(),
                        Optional.of(BigInteger.valueOf(42)),
                        protocolSpecAdapters,
                        PrivacyParameters.DEFAULT,
                        false,
                        EvmConfiguration.DEFAULT,
                        MiningConfiguration.MINING_DISABLED,
                        new BadBlockManager(),
                        false,
                        new NoOpMetricsSystem())
                    .createProtocolSchedule())
            .build();

    final MutableBlockchain blockchain = executionContextTestFixture.getBlockchain();
    BlockHeader parentHeader = blockchain.getChainHeadHeader();
    final TransactionPoolConfiguration poolConf =
        ImmutableTransactionPoolConfiguration.builder().txPoolMaxSize(100).build();
    final AbstractPendingTransactionsSorter sorter =
        new GasPricePendingTransactionsSorter(
            poolConf,
            Clock.systemUTC(),
            new NoOpMetricsSystem(),
            Suppliers.ofInstance(parentHeader));

    final EthContext ethContext = mock(EthContext.class, RETURNS_DEEP_STUBS);
    when(ethContext.getEthPeers().subscribeConnect(any())).thenReturn(1L);

    final TransactionPool transactionPool =
        new TransactionPool(
            () -> sorter,
            executionContextTestFixture.getProtocolSchedule(),
            executionContextTestFixture.getProtocolContext(),
            mock(TransactionBroadcaster.class),
            ethContext,
            new TransactionPoolMetrics(new NoOpMetricsSystem()),
            poolConf,
            new BlobCache());
    transactionPool.setEnabled();

    final MiningConfiguration miningConfiguration =
        ImmutableMiningConfiguration.builder()
            .mutableInitValues(
                MutableInitValues.builder()
                    .extraData(Bytes.fromHexString("deadbeef"))
                    .minTransactionGasPrice(Wei.ONE)
                    .minBlockOccupancyRatio(0d)
                    .coinbase(Address.ZERO)
                    .build())
            .build();

    return new CreateOn(
        new TestBlockCreator(
            miningConfiguration,
            __ -> Address.ZERO,
            __ -> Bytes.fromHexString("deadbeef"),
            transactionPool,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            ethScheduler),
        parentHeader);
  }

  static class TestBlockCreator extends AbstractBlockCreator {

    protected TestBlockCreator(
        final MiningConfiguration miningConfiguration,
        final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
        final ExtraDataCalculator extraDataCalculator,
        final TransactionPool transactionPool,
        final ProtocolContext protocolContext,
        final ProtocolSchedule protocolSchedule,
        final EthScheduler ethScheduler) {
      super(
          miningConfiguration,
          miningBeneficiaryCalculator,
          extraDataCalculator,
          transactionPool,
          protocolContext,
          protocolSchedule,
          ethScheduler);
    }

    @Override
    protected BlockHeader createFinalBlockHeader(final SealableBlockHeader sealableBlockHeader) {
      return BlockHeaderBuilder.create()
          .difficulty(Difficulty.ZERO)
          .populateFrom(sealableBlockHeader)
          .mixHash(Hash.EMPTY)
          .nonce(0L)
          .blockHeaderFunctions(blockHeaderFunctions)
          .buildBlockHeader();
    }
  }

  static class AlwaysValidTransactionValidator implements TransactionValidator {

    @Override
    public ValidationResult<TransactionInvalidReason> validate(
        final Transaction transaction,
        final Optional<Wei> baseFee,
        final Optional<Wei> blobBaseFee,
        final TransactionValidationParams transactionValidationParams) {
      return ValidationResult.valid();
    }

    @Override
    public ValidationResult<TransactionInvalidReason> validateForSender(
        final Transaction transaction,
        final Account sender,
        final TransactionValidationParams validationParams) {
      return ValidationResult.valid();
    }
  }
}
