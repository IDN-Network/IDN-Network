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
package org.idnecology.idn.tests.acceptance.backup;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.config.JsonUtil;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.tests.acceptance.AbstractPreexistingNodeTest;
import org.idnecology.idn.tests.acceptance.database.DatabaseMigrationAcceptanceTest;
import org.idnecology.idn.tests.acceptance.dsl.WaitUtils;
import org.idnecology.idn.tests.acceptance.dsl.blockchain.Amount;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.acceptance.dsl.node.configuration.IdnNodeConfigurationBuilder;

import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class BackupRoundTripAcceptanceTest extends AbstractPreexistingNodeTest {

  private Path backupPath;
  private Path restorePath;
  private Path rebackupPath;

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
    backupPath = Files.createTempDirectory("backup");
    backupPath.toFile().deleteOnExit();
    restorePath = Files.createTempDirectory("restore");
    restorePath.toFile().deleteOnExit();
    rebackupPath = Files.createTempDirectory("rebackup");
    rebackupPath.toFile().deleteOnExit();

    final URL rootURL = DatabaseMigrationAcceptanceTest.class.getResource(dataPath);
    hostDataPath = copyDataDir(rootURL);
    final Path databaseArchive =
        Paths.get(
            DatabaseMigrationAcceptanceTest.class
                .getResource(String.format("%s/idn-db-archive.tar.gz", dataPath))
                .toURI());
    extract(databaseArchive, hostDataPath.toAbsolutePath().toString());
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("getParameters")
  public void backupRoundtripAndBack(
      final String testName,
      final String dataPath,
      final long expectedChainHeight,
      final List<AccountData> testAccounts)
      throws Exception {

    setUp(testName, dataPath);

    // backup from existing files
    final IdnNode backupNode =
        idn.createNode(
            "backup " + testName,
            configureNodeCommands(
                hostDataPath,
                "operator",
                "x-backup-state",
                "--backup-path=" + backupPath.toString(),
                "--block=100"));
    cluster.startNode(backupNode);
    WaitUtils.waitFor(60, () -> backupNode.verify(exitedSuccessfully));
    final ObjectNode backupManifest =
        JsonUtil.objectNodeFromString(
            Files.readString(backupPath.resolve("idn-backup-manifest.json")));

    // restore to a new directory
    final IdnNode restoreNode =
        idn.createNode(
            "restore " + testName,
            configureNodeCommands(
                restorePath, "operator", "x-restore-state", "--backup-path=" + backupPath));
    cluster.startNode(restoreNode);
    WaitUtils.waitFor(60, () -> restoreNode.verify(exitedSuccessfully));

    // start up the backed-up version and assert some details
    final IdnNode runningNode = idn.createNode(testName, this::configureNode);
    cluster.start(runningNode);

    // height matches
    blockchain.currentHeight(expectedChainHeight).verify(runningNode);

    // accounts have value
    testAccounts.forEach(
        accountData ->
            accounts
                .createAccount(Address.fromHexString(accountData.getAccountAddress()))
                .balanceAtBlockEquals(
                    Amount.wei(accountData.getExpectedBalance().toBigInteger()),
                    accountData.getBlock())
                .verify(runningNode));

    runningNode.stop();

    // backup from the restore
    final IdnNode rebackupIdnNode =
        idn.createNode(
            "rebackup " + testName,
            configureNodeCommands(
                restorePath,
                "operator",
                "x-backup-state",
                "--backup-path=" + rebackupPath.toString(),
                "--block=100"));
    cluster.startNode(rebackupIdnNode);
    WaitUtils.waitFor(60, () -> rebackupIdnNode.verify(exitedSuccessfully));
    final ObjectNode rebackupManifest =
        JsonUtil.objectNodeFromString(
            Files.readString(rebackupPath.resolve("idn-backup-manifest.json")));

    // expect that the backup and rebackup manifests match
    assertThat(rebackupManifest).isEqualTo(backupManifest);
  }

  @Nonnull
  private UnaryOperator<IdnNodeConfigurationBuilder> configureNodeCommands(
      final Path dataPath, final String... commands) {
    return nodeBuilder -> super.configureNode(nodeBuilder).dataPath(dataPath).run(commands);
  }
}
