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
package org.idnecology.idn.tests.acceptance;

import static org.apache.logging.log4j.util.LoaderUtil.getClassLoader;

import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.tests.acceptance.dsl.AcceptanceTestBase;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;

public class QuorumIBFTMigrationTest extends AcceptanceTestBase {

  public static void copyKeyFilesToNodeDataDirs(final IdnNode... nodes) throws IOException {
    for (IdnNode node : nodes) {
      copyKeyFile(node, "key");
      copyKeyFile(node, "key.pub");
    }
  }

  private static void copyKeyFile(final IdnNode node, final String keyFileName)
      throws IOException {
    String resourceFileName = "qbft/migration-ibft1/" + node.getName() + keyFileName;
    try (InputStream keyFileStream = getClassLoader().getResourceAsStream(resourceFileName)) {
      if (keyFileStream == null) {
        throw new IOException("Resource not found: " + resourceFileName);
      }
      Path targetPath = node.homeDirectory().resolve(keyFileName);
      Files.createDirectories(targetPath.getParent());
      Files.copy(keyFileStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public static void runIdnCommand(final Path dataPath) throws IOException, InterruptedException {
    ProcessBuilder processBuilder =
        new ProcessBuilder(
            "../../build/install/idn/bin/idn",
            "--genesis-file",
            "src/test/resources/qbft/migration-ibft1/qbft-migration.json",
            "--data-path",
            dataPath.toString(),
            "--data-storage-format",
            "FOREST",
            "blocks",
            "import",
            "src/test/resources/qbft/migration-ibft1/ibft.blocks");

    processBuilder.directory(new File(System.getProperty("user.dir")));
    processBuilder.inheritIO(); // This will redirect the output to the console

    Process process = processBuilder.start();
    int exitCode = process.waitFor();
    if (exitCode == 0) {
      System.out.println("Import command executed successfully.");
    } else {
      throw new RuntimeException("Import command execution failed with exit code: " + exitCode);
    }
  }

  @Test
  public void shouldImportIBFTBlocksAndTransitionToQBFT() throws Exception {

    // Create a mix of Bonsai and Forest DB nodes
    final IdnNode minerNode1 =
        idn.createQbftMigrationNode("miner1", false, DataStorageFormat.FOREST);
    final IdnNode minerNode2 =
        idn.createQbftMigrationNode("miner2", false, DataStorageFormat.FOREST);
    final IdnNode minerNode3 =
        idn.createQbftMigrationNode("miner3", false, DataStorageFormat.FOREST);
    final IdnNode minerNode4 =
        idn.createQbftMigrationNode("miner4", false, DataStorageFormat.FOREST);
    final IdnNode minerNode5 =
        idn.createQbftMigrationNode("miner5", false, DataStorageFormat.FOREST);

    // Copy key files to the node datadirs
    // Use the key files saved in resources directory
    copyKeyFilesToNodeDataDirs(minerNode1, minerNode2, minerNode3, minerNode4, minerNode5);

    // start one node and import blocks from import file
    // Use import file, genesis saved in resources directory

    runIdnCommand(minerNode1.homeDirectory());

    // After the import is done, start the rest of the nodes using the same genesis and respective
    // node keys

    cluster.start(minerNode1, minerNode2, minerNode3, minerNode4, minerNode5);

    // Check that the chain is progressing as expected
    cluster.verify(blockchain.reachesHeight(minerNode2, 1, 120));
  }

  @Override
  public void tearDownAcceptanceTestBase() {
    cluster.stop();
    super.tearDownAcceptanceTestBase();
  }
}
