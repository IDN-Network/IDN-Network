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
package org.idnecology.idn.ethereum.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.mainnet.TransactionValidator;
import org.idnecology.idn.ethereum.mainnet.TransactionValidationParams;
import org.idnecology.idn.ethereum.mainnet.ValidationResult;
import org.idnecology.idn.ethereum.referencetests.ReferenceTestProtocolSchedules;
import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.transaction.TransactionInvalidReason;
import org.idnecology.idn.evm.gascalculator.BerlinGasCalculator;
import org.idnecology.idn.evm.gascalculator.ByzantiumGasCalculator;
import org.idnecology.idn.evm.gascalculator.CancunGasCalculator;
import org.idnecology.idn.evm.gascalculator.ConstantinopleGasCalculator;
import org.idnecology.idn.evm.gascalculator.FrontierGasCalculator;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.gascalculator.HomesteadGasCalculator;
import org.idnecology.idn.evm.gascalculator.IstanbulGasCalculator;
import org.idnecology.idn.evm.gascalculator.LondonGasCalculator;
import org.idnecology.idn.evm.gascalculator.PetersburgGasCalculator;
import org.idnecology.idn.evm.gascalculator.PragueGasCalculator;
import org.idnecology.idn.evm.gascalculator.ShanghaiGasCalculator;
import org.idnecology.idn.evm.gascalculator.SpuriousDragonGasCalculator;
import org.idnecology.idn.evm.gascalculator.TangerineWhistleGasCalculator;
import org.idnecology.idn.testutil.JsonTestParameters;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TransactionTest {

  private static TransactionValidator transactionValidator(final String name) {
    return ReferenceTestProtocolSchedules.getInstance()
        .getByName(name)
        .getByBlockHeader(BlockHeaderBuilder.createDefault().buildBlockHeader())
        .getTransactionValidatorFactory()
        .get();
  }

  private static final String TEST_CONFIG_FILE_DIR_PATH = "TransactionTests/";

  public static Stream<Arguments> getTestParametersForConfig() {
    return JsonTestParameters.create(TransactionTestCaseSpec.class)
        .generator((name, fullPath, spec, collector) -> collector.add(name, fullPath, spec, true))
        .generate(TEST_CONFIG_FILE_DIR_PATH)
        .stream()
        .map(params -> Arguments.of(params[0], params[1]));
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void frontier(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Frontier", new FrontierGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void homestead(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Homestead", new HomesteadGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void eIP150(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "EIP150", new TangerineWhistleGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void eIP158(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "EIP158", new SpuriousDragonGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void byzantium(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Byzantium", new ByzantiumGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void constantinople(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Constantinople", new ConstantinopleGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void petersburg(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "ConstantinopleFix", new PetersburgGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void istanbul(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Istanbul", new IstanbulGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void berlin(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Berlin", new BerlinGasCalculator(), Optional.empty());
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void london(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "London", new LondonGasCalculator(), Optional.of(Wei.of(0)));
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void merge(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Merge", new LondonGasCalculator(), Optional.of(Wei.of(0)));
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void shanghai(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Shanghai", new ShanghaiGasCalculator(), Optional.of(Wei.of(0)));
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void cancun(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Cancun", new CancunGasCalculator(), Optional.of(Wei.of(0)));
  }

  @ParameterizedTest(name = "Name: {0}")
  @MethodSource("getTestParametersForConfig")
  public void prague(final String name, final TransactionTestCaseSpec spec) {
    milestone(spec, name, "Prague", new PragueGasCalculator(), Optional.of(Wei.of(0)));
  }

  public void milestone(
      final TransactionTestCaseSpec spec,
      final String name,
      final String milestone,
      final GasCalculator gasCalculator,
      final Optional<Wei> baseFee) {

    final TransactionTestCaseSpec.Expectation expected = spec.expectation(milestone);

    try {
      Bytes rlp = spec.getRlp();

      // non-frontier transactions need to be opaque for parsing to work
      if (rlp.get(0) > 0) {
        final BytesValueRLPOutput output = new BytesValueRLPOutput();
        output.writeBytes(rlp);
        rlp = output.encoded();
      }

      // Test transaction deserialization (will throw an exception if it fails).
      final Transaction transaction = Transaction.readFrom(RLP.input(rlp));
      final ValidationResult<TransactionInvalidReason> validation =
          transactionValidator(milestone)
              .validate(transaction, baseFee, Optional.empty(), TransactionValidationParams.processingBlock());
      if (!validation.isValid()) {
        throw new RuntimeException(
            String.format(
                "Transaction is invalid %s - %s", validation.getInvalidReason(), transaction));
      }

      // Test rlp encoding
      final Bytes actualRlp = RLP.encode(transaction::writeTo);
      assertThat(expected.isSucceeds())
          .withFailMessage("Transaction " + name + "/" + milestone + " was supposed to be invalid")
          .isTrue();

      assertThat(actualRlp).isEqualTo(rlp);

      assertThat(transaction.getSender()).isEqualTo(expected.getSender());
      assertThat(transaction.getHash()).isEqualTo(expected.getHash());
      final long baselineGas =
        transaction.getAccessList().map(gasCalculator::accessListGasCost).orElse(0L) +
          gasCalculator.delegateCodeGasCost(transaction.codeDelegationListSize());
      final long intrinsicGasCost = gasCalculator.transactionIntrinsicGasCost(
        transaction.getPayload(),
        transaction.isContractCreation(),
        baselineGas);
      assertThat(intrinsicGasCost).isEqualTo(expected.getIntrinsicGas());
    } catch (final Exception e) {
      if (expected.isSucceeds()) {
        throw e;
      }
    }
  }
}
