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
package org.idnecology.idn.evmtool;

import static org.idnecology.idn.cli.DefaultCommandValues.getDefaultIdnDataPath;

import org.idnecology.idn.controller.IdnController;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.plugin.services.IdnConfiguration;
import org.idnecology.idn.services.IdnConfigurationImpl;

import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * This class, EvmToolCommandOptionsModule, is a Dagger module that provides dependencies for the
 * EvmToolCommand. It contains options for setting up the EVM tool, such as whether revert reasons
 * should be persisted, the fork to evaluate, the key-value storage to be used, the data path, the
 * block number to evaluate against, and the world state update mode.
 *
 * <p>The class uses PicoCLI annotations to define these options, which can be provided via the
 * command line when running the EVM tool. Each option has a corresponding provider method that
 * Dagger uses to inject the option's value where needed.
 */
@SuppressWarnings("WeakerAccess")
@Module
public class EvmToolCommandOptionsModule {

  @Option(
      names = {"--revert-reason-enabled"},
      paramLabel = "<Boolean>",
      description = "Should revert reasons be persisted. (default: ${FALLBACK-VALUE})",
      arity = "0..1",
      fallbackValue = "true")
  final Boolean revertReasonEnabled = true;

  @Provides
  @Named("RevertReasonEnabled")
  boolean provideRevertReasonEnabled() {
    return revertReasonEnabled;
  }

  @Option(
      names = {"--fork"},
      paramLabel = "<String>",
      description = "Fork to evaluate, overriding network setting.")
  String fork = null;

  @Provides
  @Named("Fork")
  Optional<String> provideFork() {
    return Optional.ofNullable(fork);
  }

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @Option(
      names = {"--key-value-storage"},
      description =
          "Identity for the key-value storage to be used (default: 'memory' alternate: 'rocksdb')",
      arity = "1")
  private String keyValueStorageName = "memory";

  @Provides
  @Named("KeyValueStorageName")
  String provideKeyValueStorageName() {
    return keyValueStorageName;
  }

  @CommandLine.Option(
      names = {"--data-path"},
      paramLabel = "<PATH>",
      description =
          "If using RocksDB storage, the path to Idn data directory (default: ${DEFAULT-VALUE})")
  final Path dataPath = getDefaultIdnDataPath(this);

  @Provides
  @Singleton
  IdnConfiguration provideIdnConfiguration() {
    final var idnConfiguration = new IdnConfigurationImpl();
    idnConfiguration.init(dataPath, dataPath.resolve(IdnController.DATABASE_PATH), null);
    return idnConfiguration;
  }

  @Option(
      names = {"--block-number"},
      description =
          "Block number to evaluate against (default: 'PENDING', or 'EARLIEST', 'LATEST', or a number)",
      arity = "1")
  private final BlockParameter blockParameter = BlockParameter.PENDING;

  @Provides
  @Singleton
  BlockParameter provideBlockParameter() {
    return blockParameter;
  }

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"})
  @CommandLine.Option(
      names = {"--Xevm-jumpdest-cache-weight-kb"},
      description =
          "size in kilobytes to allow the cache "
              + "of valid jump destinations to grow to before evicting the least recently used entry",
      fallbackValue = "32000",
      defaultValue = "32000",
      hidden = true,
      arity = "1")
  private Long jumpDestCacheWeightKilobytes =
      32_000L; // 10k contracts, (25k max contract size / 8 bit) + 32byte hash

  @CommandLine.Option(
      names = {"--Xevm-worldstate-update-mode"},
      description = "How to handle worldstate updates within a transaction",
      fallbackValue = "STACKED",
      defaultValue = "STACKED",
      hidden = true,
      arity = "1")
  private EvmConfiguration.WorldUpdaterMode worldstateUpdateMode =
      EvmConfiguration.WorldUpdaterMode
          .STACKED; // Stacked Updater.  Years of battle tested correctness.

  @Provides
  @Singleton
  EvmConfiguration provideEvmConfiguration() {
    return new EvmConfiguration(jumpDestCacheWeightKilobytes, worldstateUpdateMode);
  }

  /** Default constructor for the EvmToolCommandOptionsModule class. */
  public EvmToolCommandOptionsModule() {
    // This is only here because of JavaDoc linting
  }
}
