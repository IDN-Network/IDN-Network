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

import org.idnecology.idn.cli.config.EthNetworkConfig;
import org.idnecology.idn.cli.config.NetworkName;
import org.idnecology.idn.config.GenesisConfig;
import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.datatypes.HardforkId.MainnetHardforkId;
import org.idnecology.idn.ethereum.chain.GenesisState;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.evm.internal.EvmConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.json.JsonObject;

/**
 * This class, GenesisFileModule, is a Dagger module that provides dependencies for the GenesisFile.
 * It contains options for setting up the GenesisFile, such as the genesis configuration, genesis
 * state, block header functions, and the genesis block.
 *
 * <p>The class uses Dagger annotations to define these options, which can be provided via the
 * command line when running the EVM tool. Each option has a corresponding provider method that
 * Dagger uses to inject the option's value where needed.
 */
@Module
public class GenesisFileModule {

  private final String genesisConfig;

  /**
   * Constructs a new GenesisFileModule with the specified genesis configuration.
   *
   * @param genesisConfig The configuration for the genesis file. This is typically a JSON string
   *     that specifies various parameters for the genesis block of the blockchain.
   */
  protected GenesisFileModule(final String genesisConfig) {
    this.genesisConfig = genesisConfig;
  }

  @Singleton
  @Provides
  GenesisConfig providesGenesisConfig() {
    return GenesisConfig.fromConfig(genesisConfig);
  }

  @Singleton
  @Provides
  GenesisConfigOptions provideGenesisConfigOptions(final GenesisConfig genesisConfig) {
    return genesisConfig.getConfigOptions();
  }

  @Singleton
  @Provides
  @SuppressWarnings("UnusedVariable")
  ProtocolSchedule provideProtocolSchedule(
      final GenesisConfigOptions configOptions,
      @Named("Fork") final Optional<String> fork,
      @Named("RevertReasonEnabled") final boolean revertReasonEnabled,
      final EvmConfiguration evmConfiguration) {
    throw new RuntimeException("Abstract");
  }

  @Singleton
  @Provides
  GenesisState provideGenesisState(
      final GenesisConfig genesisConfig, final ProtocolSchedule protocolSchedule) {
    return GenesisState.fromConfig(genesisConfig, protocolSchedule);
  }

  @Singleton
  @Provides
  BlockHeaderFunctions blockHashFunction() {
    throw new RuntimeException("Abstract");
  }

  @Singleton
  @Provides
  @Named("GenesisBlock")
  Block provideGenesisBlock(final GenesisState genesisState) {
    return genesisState.getBlock();
  }

  static GenesisFileModule createGenesisModule(final NetworkName networkName) {
    return createGenesisModule(EthNetworkConfig.jsonConfig(networkName));
  }

  static GenesisFileModule createGenesisModule(final File genesisFile) throws IOException {
    return createGenesisModule(Files.readString(genesisFile.toPath(), Charset.defaultCharset()));
  }

  static GenesisFileModule createGenesisModule() {
    final JsonObject genesis = new JsonObject();
    final JsonObject config = new JsonObject();
    genesis.put("config", config);
    config.put("chainId", 1337);
    config.put(MainnetHardforkId.mostRecent().toString().toLowerCase(Locale.ROOT) + "Time", 0);
    genesis.put("baseFeePerGas", "0x3b9aca00");
    genesis.put("gasLimit", "0x2540be400");
    genesis.put("difficulty", "0x0");
    genesis.put("mixHash", "0x0000000000000000000000000000000000000000000000000000000000000000");
    genesis.put("coinbase", "0x0000000000000000000000000000000000000000");
    return createGenesisModule(genesis.toString());
  }

  private static GenesisFileModule createGenesisModule(final String genesisConfig) {
    final JsonObject genesis = new JsonObject(genesisConfig);
    final JsonObject config = genesis.getJsonObject("config");
    if (config.containsKey("ibft") || config.containsKey("clique") || config.containsKey("qbft")) {
      throw new RuntimeException("Only Ethash and Merge configs accepted as genesis files");
    }
    return new MainnetGenesisFileModule(genesisConfig);
  }
}
