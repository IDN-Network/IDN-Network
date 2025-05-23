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
package org.idnecology.idn.tests.acceptance.database;

import static java.util.Collections.singletonList;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.tests.acceptance.AbstractPreexistingNodeTest;
import org.idnecology.idn.tests.acceptance.dsl.blockchain.Amount;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DatabaseMigrationAcceptanceTest
    extends org.idnecology.idn.tests.acceptance.AbstractPreexistingNodeTest {
  private IdnNode node;

  public static Stream<Arguments> getParameters() {
    // First 10 blocks of ropsten
    return Stream.of(
        Arguments.of(
            "After versioning was enabled and using multiple RocksDB columns",
            "version1",
            0xA,
            singletonList(
                new AccountData(
                    "0xd1aeb42885a43b72b518182ef893125814811048",
                    BigInteger.valueOf(0xA),
                    Wei.fromHexString("0x2B5E3AF16B1880000")))));
  }

  public void setUp(final String testName, final String dataPath) throws Exception {
    final URL rootURL = DatabaseMigrationAcceptanceTest.class.getResource(dataPath);
    hostDataPath = copyDataDir(rootURL);
    final Path databaseArchive =
        Paths.get(
            DatabaseMigrationAcceptanceTest.class
                .getResource(String.format("%s/idn-db-archive.tar.gz", dataPath))
                .toURI());
    AbstractPreexistingNodeTest.extract(databaseArchive, hostDataPath.toAbsolutePath().toString());
    node = idn.createNode(testName, this::configureNode);
    cluster.start(node);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("getParameters")
  public void shouldReturnCorrectBlockHeight(
      final String testName,
      final String dataPath,
      final long expectedChainHeight,
      final List<AccountData> testAccounts)
      throws Exception {
    setUp(testName, dataPath);
    blockchain.currentHeight(expectedChainHeight).verify(node);
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("getParameters")
  public void shouldReturnCorrectAccountBalance(
      final String testName,
      final String dataPath,
      final long expectedChainHeight,
      final List<AccountData> testAccounts)
      throws Exception {
    setUp(testName, dataPath);
    testAccounts.forEach(
        accountData ->
            accounts
                .createAccount(Address.fromHexString(accountData.getAccountAddress()))
                .balanceAtBlockEquals(
                    Amount.wei(accountData.getExpectedBalance().toBigInteger()),
                    accountData.getBlock())
                .verify(node));
  }
}
