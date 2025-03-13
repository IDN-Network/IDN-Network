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
package org.idnecology.idn.evm;

import org.idnecology.idn.evm.gascalculator.BerlinGasCalculator;
import org.idnecology.idn.evm.gascalculator.ByzantiumGasCalculator;
import org.idnecology.idn.evm.gascalculator.CancunGasCalculator;
import org.idnecology.idn.evm.gascalculator.ConstantinopleGasCalculator;
import org.idnecology.idn.evm.gascalculator.FrontierGasCalculator;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.gascalculator.HomesteadGasCalculator;
import org.idnecology.idn.evm.gascalculator.IstanbulGasCalculator;
import org.idnecology.idn.evm.gascalculator.LondonGasCalculator;
import org.idnecology.idn.evm.gascalculator.OsakaGasCalculator;
import org.idnecology.idn.evm.gascalculator.PetersburgGasCalculator;
import org.idnecology.idn.evm.gascalculator.PragueGasCalculator;
import org.idnecology.idn.evm.gascalculator.ShanghaiGasCalculator;
import org.idnecology.idn.evm.gascalculator.SpuriousDragonGasCalculator;
import org.idnecology.idn.evm.gascalculator.TangerineWhistleGasCalculator;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.evm.operation.AddModOperation;
import org.idnecology.idn.evm.operation.AddOperation;
import org.idnecology.idn.evm.operation.AddressOperation;
import org.idnecology.idn.evm.operation.AndOperation;
import org.idnecology.idn.evm.operation.BalanceOperation;
import org.idnecology.idn.evm.operation.BaseFeeOperation;
import org.idnecology.idn.evm.operation.BlobBaseFeeOperation;
import org.idnecology.idn.evm.operation.BlobHashOperation;
import org.idnecology.idn.evm.operation.BlockHashOperation;
import org.idnecology.idn.evm.operation.ByteOperation;
import org.idnecology.idn.evm.operation.CallCodeOperation;
import org.idnecology.idn.evm.operation.CallDataCopyOperation;
import org.idnecology.idn.evm.operation.CallDataLoadOperation;
import org.idnecology.idn.evm.operation.CallDataSizeOperation;
import org.idnecology.idn.evm.operation.CallFOperation;
import org.idnecology.idn.evm.operation.CallOperation;
import org.idnecology.idn.evm.operation.CallValueOperation;
import org.idnecology.idn.evm.operation.CallerOperation;
import org.idnecology.idn.evm.operation.ChainIdOperation;
import org.idnecology.idn.evm.operation.CodeCopyOperation;
import org.idnecology.idn.evm.operation.CodeSizeOperation;
import org.idnecology.idn.evm.operation.CoinbaseOperation;
import org.idnecology.idn.evm.operation.Create2Operation;
import org.idnecology.idn.evm.operation.CreateOperation;
import org.idnecology.idn.evm.operation.DataCopyOperation;
import org.idnecology.idn.evm.operation.DataLoadNOperation;
import org.idnecology.idn.evm.operation.DataLoadOperation;
import org.idnecology.idn.evm.operation.DataSizeOperation;
import org.idnecology.idn.evm.operation.DelegateCallOperation;
import org.idnecology.idn.evm.operation.DifficultyOperation;
import org.idnecology.idn.evm.operation.DivOperation;
import org.idnecology.idn.evm.operation.DupNOperation;
import org.idnecology.idn.evm.operation.DupOperation;
import org.idnecology.idn.evm.operation.EOFCreateOperation;
import org.idnecology.idn.evm.operation.EqOperation;
import org.idnecology.idn.evm.operation.ExchangeOperation;
import org.idnecology.idn.evm.operation.ExpOperation;
import org.idnecology.idn.evm.operation.ExtCallOperation;
import org.idnecology.idn.evm.operation.ExtCodeCopyOperation;
import org.idnecology.idn.evm.operation.ExtCodeHashOperation;
import org.idnecology.idn.evm.operation.ExtCodeSizeOperation;
import org.idnecology.idn.evm.operation.ExtDelegateCallOperation;
import org.idnecology.idn.evm.operation.ExtStaticCallOperation;
import org.idnecology.idn.evm.operation.GasLimitOperation;
import org.idnecology.idn.evm.operation.GasOperation;
import org.idnecology.idn.evm.operation.GasPriceOperation;
import org.idnecology.idn.evm.operation.GtOperation;
import org.idnecology.idn.evm.operation.InvalidOperation;
import org.idnecology.idn.evm.operation.IsZeroOperation;
import org.idnecology.idn.evm.operation.JumpDestOperation;
import org.idnecology.idn.evm.operation.JumpFOperation;
import org.idnecology.idn.evm.operation.JumpOperation;
import org.idnecology.idn.evm.operation.JumpiOperation;
import org.idnecology.idn.evm.operation.Keccak256Operation;
import org.idnecology.idn.evm.operation.LogOperation;
import org.idnecology.idn.evm.operation.LtOperation;
import org.idnecology.idn.evm.operation.MCopyOperation;
import org.idnecology.idn.evm.operation.MLoadOperation;
import org.idnecology.idn.evm.operation.MSizeOperation;
import org.idnecology.idn.evm.operation.MStore8Operation;
import org.idnecology.idn.evm.operation.MStoreOperation;
import org.idnecology.idn.evm.operation.ModOperation;
import org.idnecology.idn.evm.operation.MulModOperation;
import org.idnecology.idn.evm.operation.MulOperation;
import org.idnecology.idn.evm.operation.NotOperation;
import org.idnecology.idn.evm.operation.NumberOperation;
import org.idnecology.idn.evm.operation.OperationRegistry;
import org.idnecology.idn.evm.operation.OrOperation;
import org.idnecology.idn.evm.operation.OriginOperation;
import org.idnecology.idn.evm.operation.PCOperation;
import org.idnecology.idn.evm.operation.PopOperation;
import org.idnecology.idn.evm.operation.PrevRanDaoOperation;
import org.idnecology.idn.evm.operation.Push0Operation;
import org.idnecology.idn.evm.operation.PushOperation;
import org.idnecology.idn.evm.operation.RelativeJumpIfOperation;
import org.idnecology.idn.evm.operation.RelativeJumpOperation;
import org.idnecology.idn.evm.operation.RelativeJumpVectorOperation;
import org.idnecology.idn.evm.operation.RetFOperation;
import org.idnecology.idn.evm.operation.ReturnContractOperation;
import org.idnecology.idn.evm.operation.ReturnDataCopyOperation;
import org.idnecology.idn.evm.operation.ReturnDataLoadOperation;
import org.idnecology.idn.evm.operation.ReturnDataSizeOperation;
import org.idnecology.idn.evm.operation.ReturnOperation;
import org.idnecology.idn.evm.operation.RevertOperation;
import org.idnecology.idn.evm.operation.SDivOperation;
import org.idnecology.idn.evm.operation.SGtOperation;
import org.idnecology.idn.evm.operation.SLoadOperation;
import org.idnecology.idn.evm.operation.SLtOperation;
import org.idnecology.idn.evm.operation.SModOperation;
import org.idnecology.idn.evm.operation.SStoreOperation;
import org.idnecology.idn.evm.operation.SarOperation;
import org.idnecology.idn.evm.operation.SelfBalanceOperation;
import org.idnecology.idn.evm.operation.SelfDestructOperation;
import org.idnecology.idn.evm.operation.ShlOperation;
import org.idnecology.idn.evm.operation.ShrOperation;
import org.idnecology.idn.evm.operation.SignExtendOperation;
import org.idnecology.idn.evm.operation.StaticCallOperation;
import org.idnecology.idn.evm.operation.StopOperation;
import org.idnecology.idn.evm.operation.SubOperation;
import org.idnecology.idn.evm.operation.SwapNOperation;
import org.idnecology.idn.evm.operation.SwapOperation;
import org.idnecology.idn.evm.operation.TLoadOperation;
import org.idnecology.idn.evm.operation.TStoreOperation;
import org.idnecology.idn.evm.operation.TimestampOperation;
import org.idnecology.idn.evm.operation.XorOperation;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** Provides EVMs supporting the appropriate operations for mainnet hard forks. */
public class MainnetEVMs {

  /** The constant DEV_NET_CHAIN_ID. */
  public static final BigInteger DEV_NET_CHAIN_ID = BigInteger.valueOf(1337);

  private MainnetEVMs() {
    // utility class
  }

  /**
   * Frontier evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM frontier(final EvmConfiguration evmConfiguration) {
    return frontier(new FrontierGasCalculator(), evmConfiguration);
  }

  /**
   * Frontier evm.
   *
   * @param gasCalculator the gas calculator
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM frontier(
      final GasCalculator gasCalculator, final EvmConfiguration evmConfiguration) {
    return new EVM(
        frontierOperations(gasCalculator),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.FRONTIER);
  }

  /**
   * Operation registry for frontier's operations.
   *
   * @param gasCalculator the gas calculator
   * @return the operation registry
   */
  public static OperationRegistry frontierOperations(final GasCalculator gasCalculator) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerFrontierOperations(operationRegistry, gasCalculator);
    return operationRegistry;
  }

  /**
   * Register frontier operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   */
  public static void registerFrontierOperations(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    for (int i = 0; i < 255; i++) {
      registry.put(new InvalidOperation(i, gasCalculator));
    }
    registry.put(new AddOperation(gasCalculator));
    registry.put(new MulOperation(gasCalculator));
    registry.put(new SubOperation(gasCalculator));
    registry.put(new DivOperation(gasCalculator));
    registry.put(new SDivOperation(gasCalculator));
    registry.put(new ModOperation(gasCalculator));
    registry.put(new SModOperation(gasCalculator));
    registry.put(new ExpOperation(gasCalculator));
    registry.put(new AddModOperation(gasCalculator));
    registry.put(new MulModOperation(gasCalculator));
    registry.put(new SignExtendOperation(gasCalculator));
    registry.put(new LtOperation(gasCalculator));
    registry.put(new GtOperation(gasCalculator));
    registry.put(new SLtOperation(gasCalculator));
    registry.put(new SGtOperation(gasCalculator));
    registry.put(new EqOperation(gasCalculator));
    registry.put(new IsZeroOperation(gasCalculator));
    registry.put(new AndOperation(gasCalculator));
    registry.put(new OrOperation(gasCalculator));
    registry.put(new XorOperation(gasCalculator));
    registry.put(new NotOperation(gasCalculator));
    registry.put(new ByteOperation(gasCalculator));
    registry.put(new Keccak256Operation(gasCalculator));
    registry.put(new AddressOperation(gasCalculator));
    registry.put(new BalanceOperation(gasCalculator));
    registry.put(new OriginOperation(gasCalculator));
    registry.put(new CallerOperation(gasCalculator));
    registry.put(new CallValueOperation(gasCalculator));
    registry.put(new CallDataLoadOperation(gasCalculator));
    registry.put(new CallDataSizeOperation(gasCalculator));
    registry.put(new CallDataCopyOperation(gasCalculator));
    registry.put(new CodeSizeOperation(gasCalculator));
    registry.put(new CodeCopyOperation(gasCalculator));
    registry.put(new GasPriceOperation(gasCalculator));
    registry.put(new ExtCodeCopyOperation(gasCalculator, false));
    registry.put(new ExtCodeSizeOperation(gasCalculator, false));
    registry.put(new BlockHashOperation(gasCalculator));
    registry.put(new CoinbaseOperation(gasCalculator));
    registry.put(new TimestampOperation(gasCalculator));
    registry.put(new NumberOperation(gasCalculator));
    registry.put(new DifficultyOperation(gasCalculator));
    registry.put(new GasLimitOperation(gasCalculator));
    registry.put(new PopOperation(gasCalculator));
    registry.put(new MLoadOperation(gasCalculator));
    registry.put(new MStoreOperation(gasCalculator));
    registry.put(new MStore8Operation(gasCalculator));
    registry.put(new SLoadOperation(gasCalculator));
    registry.put(new SStoreOperation(gasCalculator, SStoreOperation.FRONTIER_MINIMUM));
    registry.put(new JumpOperation(gasCalculator));
    registry.put(new JumpiOperation(gasCalculator));
    registry.put(new PCOperation(gasCalculator));
    registry.put(new MSizeOperation(gasCalculator));
    registry.put(new GasOperation(gasCalculator));
    registry.put(new JumpDestOperation(gasCalculator));
    registry.put(new ReturnOperation(gasCalculator));
    registry.put(new InvalidOperation(gasCalculator));
    registry.put(new StopOperation(gasCalculator));
    registry.put(new SelfDestructOperation(gasCalculator));
    registry.put(new CreateOperation(gasCalculator));
    registry.put(new CallOperation(gasCalculator));
    registry.put(new CallCodeOperation(gasCalculator));

    // Register the PUSH1, PUSH2, ..., PUSH32 operations.
    for (int i = 1; i <= 32; ++i) {
      registry.put(new PushOperation(i, gasCalculator));
    }

    // Register the DUP1, DUP2, ..., DUP16 operations.
    for (int i = 1; i <= 16; ++i) {
      registry.put(new DupOperation(i, gasCalculator));
    }

    // Register the SWAP1, SWAP2, ..., SWAP16 operations.
    for (int i = 1; i <= 16; ++i) {
      registry.put(new SwapOperation(i, gasCalculator));
    }

    // Register the LOG0, LOG1, ..., LOG4 operations.
    for (int i = 0; i < 5; ++i) {
      registry.put(new LogOperation(i, gasCalculator));
    }
  }

  /**
   * Homestead evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM homestead(final EvmConfiguration evmConfiguration) {
    return homestead(new HomesteadGasCalculator(), evmConfiguration);
  }

  /**
   * Homestead evm.
   *
   * @param gasCalculator the gas calculator
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM homestead(
      final GasCalculator gasCalculator, final EvmConfiguration evmConfiguration) {
    return new EVM(
        homesteadOperations(gasCalculator),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.HOMESTEAD);
  }

  /**
   * Operation registry for homestead's operations.
   *
   * @param gasCalculator the gas calculator
   * @return the operation registry
   */
  public static OperationRegistry homesteadOperations(final GasCalculator gasCalculator) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerHomesteadOperations(operationRegistry, gasCalculator);
    return operationRegistry;
  }

  /**
   * Register homestead operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   */
  public static void registerHomesteadOperations(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registerFrontierOperations(registry, gasCalculator);
    registry.put(new DelegateCallOperation(gasCalculator));
  }

  /**
   * Spurious dragon evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM spuriousDragon(final EvmConfiguration evmConfiguration) {
    GasCalculator gasCalculator = new SpuriousDragonGasCalculator();
    return new EVM(
        homesteadOperations(gasCalculator),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.SPURIOUS_DRAGON);
  }

  /**
   * Tangerine whistle evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM tangerineWhistle(final EvmConfiguration evmConfiguration) {
    GasCalculator gasCalculator = new TangerineWhistleGasCalculator();
    return new EVM(
        homesteadOperations(gasCalculator),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.TANGERINE_WHISTLE);
  }

  /**
   * Byzantium evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM byzantium(final EvmConfiguration evmConfiguration) {
    return byzantium(new ByzantiumGasCalculator(), evmConfiguration);
  }

  /**
   * Byzantium evm.
   *
   * @param gasCalculator the gas calculator
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM byzantium(
      final GasCalculator gasCalculator, final EvmConfiguration evmConfiguration) {
    return new EVM(
        byzantiumOperations(gasCalculator),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.BYZANTIUM);
  }

  /**
   * Operation registry for byzantium's operations.
   *
   * @param gasCalculator the gas calculator
   * @return the operation registry
   */
  public static OperationRegistry byzantiumOperations(final GasCalculator gasCalculator) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerByzantiumOperations(operationRegistry, gasCalculator);
    return operationRegistry;
  }

  /**
   * Register byzantium operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   */
  public static void registerByzantiumOperations(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registerHomesteadOperations(registry, gasCalculator);
    registry.put(new ReturnDataCopyOperation(gasCalculator));
    registry.put(new ReturnDataSizeOperation(gasCalculator));
    registry.put(new RevertOperation(gasCalculator));
    registry.put(new StaticCallOperation(gasCalculator));
  }

  /**
   * Constantinople evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM constantinople(final EvmConfiguration evmConfiguration) {
    return constantinople(new ConstantinopleGasCalculator(), evmConfiguration);
  }

  /**
   * Constantinople evm.
   *
   * @param gasCalculator the gas calculator
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM constantinople(
      final GasCalculator gasCalculator, final EvmConfiguration evmConfiguration) {
    var version = EvmSpecVersion.CONSTANTINOPLE;
    return constantiNOPEl(gasCalculator, evmConfiguration, version);
  }

  private static EVM constantiNOPEl(
      final GasCalculator gasCalculator,
      final EvmConfiguration evmConfiguration,
      final EvmSpecVersion version) {
    return new EVM(
        constantinopleOperations(gasCalculator), gasCalculator, evmConfiguration, version);
  }

  /**
   * Operation registry for constantinople's operations.
   *
   * @param gasCalculator the gas calculator
   * @return the operation registry
   */
  public static OperationRegistry constantinopleOperations(final GasCalculator gasCalculator) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerConstantinopleOperations(operationRegistry, gasCalculator);
    return operationRegistry;
  }

  /**
   * Register constantinople operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   */
  public static void registerConstantinopleOperations(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registerByzantiumOperations(registry, gasCalculator);
    registry.put(new Create2Operation(gasCalculator));
    registry.put(new SarOperation(gasCalculator));
    registry.put(new ShlOperation(gasCalculator));
    registry.put(new ShrOperation(gasCalculator));
    registry.put(new ExtCodeHashOperation(gasCalculator, false));
  }

  /**
   * Petersburg evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM petersburg(final EvmConfiguration evmConfiguration) {
    return constantiNOPEl(
        new PetersburgGasCalculator(), evmConfiguration, EvmSpecVersion.PETERSBURG);
  }

  /**
   * Istanbul evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM istanbul(final EvmConfiguration evmConfiguration) {
    return istanbul(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Istanbul evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM istanbul(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return istanbul(new IstanbulGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Istanbul evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM istanbul(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        istanbulOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.ISTANBUL);
  }

  /**
   * Operation registry for istanbul's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry istanbulOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerIstanbulOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register istanbul operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   */
  public static void registerIstanbulOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainId) {
    registerConstantinopleOperations(registry, gasCalculator);
    registry.put(
        new ChainIdOperation(gasCalculator, Bytes32.leftPad(Bytes.of(chainId.toByteArray()))));
    registry.put(new SelfBalanceOperation(gasCalculator));
    registry.put(new SStoreOperation(gasCalculator, SStoreOperation.EIP_1706_MINIMUM));
  }

  /**
   * Berlin evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM berlin(final EvmConfiguration evmConfiguration) {
    return berlin(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Berlin evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM berlin(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return berlin(new BerlinGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Berlin evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM berlin(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        istanbulOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.BERLIN);
  }

  /**
   * London evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM london(final EvmConfiguration evmConfiguration) {
    return london(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * London evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM london(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return london(new LondonGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * London evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM london(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        londonOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.LONDON);
  }

  /**
   * Operation registry for london's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry londonOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerLondonOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register london operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   */
  public static void registerLondonOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainId) {
    registerIstanbulOperations(registry, gasCalculator, chainId);
    registry.put(new BaseFeeOperation(gasCalculator));
  }

  /**
   * Paris evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM paris(final EvmConfiguration evmConfiguration) {
    return paris(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Paris evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM paris(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return paris(new LondonGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Paris evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM paris(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        parisOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.PARIS);
  }

  /**
   * Operation registry for paris's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry parisOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerParisOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register paris operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerParisOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerLondonOperations(registry, gasCalculator, chainID);
    registry.put(new PrevRanDaoOperation(gasCalculator));
  }

  /**
   * Shanghai evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM shanghai(final EvmConfiguration evmConfiguration) {
    return shanghai(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Shanghai evm
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM shanghai(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return shanghai(new ShanghaiGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * shanghai evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM shanghai(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        shanghaiOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.SHANGHAI);
  }

  /**
   * shanghai operations registry.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry shanghaiOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerShanghaiOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register Shanghai operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerShanghaiOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerParisOperations(registry, gasCalculator, chainID);
    registry.put(new Push0Operation(gasCalculator));
  }

  /**
   * Cancun evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM cancun(final EvmConfiguration evmConfiguration) {
    return cancun(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Cancun evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM cancun(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return cancun(new CancunGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Cancun evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM cancun(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        cancunOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.CANCUN);
  }

  /**
   * Operation registry for cancun's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry cancunOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerCancunOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register cancun operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerCancunOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerShanghaiOperations(registry, gasCalculator, chainID);

    // EIP-1153 TSTORE/TLOAD
    registry.put(new TStoreOperation(gasCalculator));
    registry.put(new TLoadOperation(gasCalculator));

    // EIP-4844 BLOBHASH
    registry.put(new BlobHashOperation(gasCalculator));

    // EIP-5656 MCOPY
    registry.put(new MCopyOperation(gasCalculator));

    // EIP-6780 nerf self destruct
    registry.put(new SelfDestructOperation(gasCalculator, true));

    // EIP-7516 BLOBBASEFEE
    registry.put(new BlobBaseFeeOperation(gasCalculator));
  }

  /**
   * CancunEOF evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM cancunEOF(final EvmConfiguration evmConfiguration) {
    return cancunEOF(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * CancunEOF evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM cancunEOF(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return cancunEOF(new CancunGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * CancunEOF evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM cancunEOF(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        cancunEOFOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.CANCUN_EOF);
  }

  /**
   * Operation registry for PragueEOF's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry cancunEOFOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerCancunEOFOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register CancunEOF's operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerCancunEOFOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerCancunOperations(registry, gasCalculator, chainID);

    registerEOFOperations(registry, gasCalculator);
  }

  /**
   * Prague evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM prague(final EvmConfiguration evmConfiguration) {
    return prague(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Prague evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM prague(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return prague(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Prague evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM prague(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        pragueOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.PRAGUE);
  }

  /**
   * Operation registry for prague's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry pragueOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerPragueOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register prague operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerPragueOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerCancunOperations(registry, gasCalculator, chainID);
  }

  /**
   * Osaka evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM osaka(final EvmConfiguration evmConfiguration) {
    return osaka(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Osaka evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM osaka(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return osaka(new OsakaGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Osaka evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM osaka(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        osakaOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.OSAKA);
  }

  /**
   * Operation registry for Osaka's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry osakaOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerOsakaOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register Osaka's operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerOsakaOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerPragueOperations(registry, gasCalculator, chainID);

    registerEOFOperations(registry, gasCalculator);
  }

  private static void registerEOFOperations(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    // EIP-663 Unlimited Swap and Dup
    registry.put(new DupNOperation(gasCalculator));
    registry.put(new SwapNOperation(gasCalculator));
    registry.put(new ExchangeOperation(gasCalculator));

    // EIP-3540 EOF Aware EXTCODE* operations
    registry.put(new ExtCodeCopyOperation(gasCalculator, true));
    registry.put(new ExtCodeHashOperation(gasCalculator, true));
    registry.put(new ExtCodeSizeOperation(gasCalculator, true));

    // EIP-4200 relative jump
    registry.put(new RelativeJumpOperation(gasCalculator));
    registry.put(new RelativeJumpIfOperation(gasCalculator));
    registry.put(new RelativeJumpVectorOperation(gasCalculator));

    // EIP-4750 EOF Code Sections
    registry.put(new CallFOperation(gasCalculator));
    registry.put(new RetFOperation(gasCalculator));

    // EIP-6209 JUMPF Instruction
    registry.put(new JumpFOperation(gasCalculator));

    // EIP-7069 Revamped EOF Call
    registry.put(new ExtCallOperation(gasCalculator));
    registry.put(new ExtDelegateCallOperation(gasCalculator));
    registry.put(new ExtStaticCallOperation(gasCalculator));
    registry.put(new ReturnDataLoadOperation(gasCalculator));

    // EIP-7480 EOF Data Section Access
    registry.put(new DataLoadOperation(gasCalculator));
    registry.put(new DataLoadNOperation(gasCalculator));
    registry.put(new DataSizeOperation(gasCalculator));
    registry.put(new DataCopyOperation(gasCalculator));

    // EIP-7620 EOF Create and Return Contract operation
    registry.put(new EOFCreateOperation(gasCalculator));
    registry.put(new ReturnContractOperation(gasCalculator));
  }

  /**
   * Amsterdam evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM amsterdam(final EvmConfiguration evmConfiguration) {
    return amsterdam(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Amsterdam evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM amsterdam(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return amsterdam(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Amsterdam evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM amsterdam(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        amsterdamOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.AMSTERDAM);
  }

  /**
   * Operation registry for amsterdam's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry amsterdamOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerAmsterdamOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register amsterdam operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerAmsterdamOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerOsakaOperations(registry, gasCalculator, chainID);
  }

  /**
   * Bogota evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM bogota(final EvmConfiguration evmConfiguration) {
    return bogota(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Bogota evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM bogota(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return bogota(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Bogota evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM bogota(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        bogotaOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.BOGOTA);
  }

  /**
   * Bogota operation registry.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry bogotaOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerBogotaOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register bogota operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerBogotaOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerAmsterdamOperations(registry, gasCalculator, chainID);
  }

  /**
   * Polis evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM polis(final EvmConfiguration evmConfiguration) {
    return polis(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Polis evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM polis(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return polis(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Polis evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM polis(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        polisOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.POLIS);
  }

  /**
   * Operation registry for Polis's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry polisOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerPolisOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register polis operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerPolisOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerBogotaOperations(registry, gasCalculator, chainID);
  }

  /**
   * Bangkok evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM bangkok(final EvmConfiguration evmConfiguration) {
    return bangkok(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Bangkok evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM bangkok(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return bangkok(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Bangkok evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM bangkok(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        bangkokOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.BANGKOK);
  }

  /**
   * Operation registry for bangkok's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry bangkokOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerBangkokOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register bangkok operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerBangkokOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerPolisOperations(registry, gasCalculator, chainID);
  }

  /**
   * Future eips evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM futureEips(final EvmConfiguration evmConfiguration) {
    return futureEips(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Future eips evm.
   *
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM futureEips(final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return futureEips(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Future eips evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM futureEips(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        futureEipsOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.FUTURE_EIPS);
  }

  /**
   * Future Operation registry for eIPs's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry futureEipsOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerFutureEipsOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register FutureEIPs operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerFutureEipsOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerBogotaOperations(registry, gasCalculator, chainID);
  }

  /**
   * Experimental eips evm.
   *
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM experimentalEips(final EvmConfiguration evmConfiguration) {
    return experimentalEips(DEV_NET_CHAIN_ID, evmConfiguration);
  }

  /**
   * Experimental eips evm.
   *
   * @param chainId the chain Id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM experimentalEips(
      final BigInteger chainId, final EvmConfiguration evmConfiguration) {
    return experimentalEips(new PragueGasCalculator(), chainId, evmConfiguration);
  }

  /**
   * Experimental eips evm.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @param evmConfiguration the evm configuration
   * @return the evm
   */
  public static EVM experimentalEips(
      final GasCalculator gasCalculator,
      final BigInteger chainId,
      final EvmConfiguration evmConfiguration) {
    return new EVM(
        experimentalEipsOperations(gasCalculator, chainId),
        gasCalculator,
        evmConfiguration,
        EvmSpecVersion.EXPERIMENTAL_EIPS);
  }

  /**
   * Operation registry for experimental's operations.
   *
   * @param gasCalculator the gas calculator
   * @param chainId the chain id
   * @return the operation registry
   */
  public static OperationRegistry experimentalEipsOperations(
      final GasCalculator gasCalculator, final BigInteger chainId) {
    OperationRegistry operationRegistry = new OperationRegistry();
    registerExperimentalEipsOperations(operationRegistry, gasCalculator, chainId);
    return operationRegistry;
  }

  /**
   * Register experimental eips operations.
   *
   * @param registry the registry
   * @param gasCalculator the gas calculator
   * @param chainID the chain id
   */
  public static void registerExperimentalEipsOperations(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainID) {
    registerFutureEipsOperations(registry, gasCalculator, chainID);
  }
}
