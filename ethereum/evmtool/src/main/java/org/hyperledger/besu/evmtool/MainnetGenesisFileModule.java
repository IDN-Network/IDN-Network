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

import org.idnecology.idn.config.GenesisConfigOptions;
import org.idnecology.idn.config.StubGenesisConfigOptions;
import org.idnecology.idn.crypto.SignatureAlgorithmFactory;
import org.idnecology.idn.crypto.SignatureAlgorithmType;
import org.idnecology.idn.ethereum.chain.BadBlockManager;
import org.idnecology.idn.ethereum.core.BlockHeaderFunctions;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.idnecology.idn.ethereum.mainnet.MainnetProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.ProtocolScheduleBuilder;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpecAdapters;
import org.idnecology.idn.evm.internal.EvmConfiguration;
import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Named;

import picocli.CommandLine;

class MainnetGenesisFileModule extends GenesisFileModule {

  MainnetGenesisFileModule(final String genesisConfig) {
    super(genesisConfig);
  }

  @Override
  BlockHeaderFunctions blockHashFunction() {
    return new MainnetBlockHeaderFunctions();
  }

  @Override
  ProtocolSchedule provideProtocolSchedule(
      final GenesisConfigOptions configOptions,
      @Named("Fork") final Optional<String> fork,
      @Named("RevertReasonEnabled") final boolean revertReasonEnabled,
      final EvmConfiguration evmConfiguration) {

    final Optional<String> ecCurve = configOptions.getEcCurve();
    if (ecCurve.isEmpty()) {
      SignatureAlgorithmFactory.setDefaultInstance();
    } else {
      try {
        SignatureAlgorithmFactory.setInstance(SignatureAlgorithmType.create(ecCurve.get()));
      } catch (final IllegalArgumentException e) {
        throw new CommandLine.InitializationException(
            "Invalid genesis file configuration for ecCurve. " + e.getMessage());
      }
    }

    if (fork.isPresent()) {
      var schedules = createSchedules(configOptions.getChainId().orElse(BigInteger.valueOf(1337)));
      var schedule = schedules.get(fork.get().toLowerCase(Locale.getDefault()));
      if (schedule != null) {
        return schedule.get();
      }
    }

    return MainnetProtocolSchedule.fromConfig(
        configOptions,
        evmConfiguration,
        MiningConfiguration.newDefault(),
        new BadBlockManager(),
        false,
        new NoOpMetricsSystem());
  }

  public static Map<String, Supplier<ProtocolSchedule>> createSchedules(final BigInteger chainId) {
    return Map.ofEntries(
        Map.entry("frontier", createSchedule(new StubGenesisConfigOptions().chainId(chainId))),
        Map.entry("homestead", createSchedule(new StubGenesisConfigOptions().homesteadBlock(0))),
        Map.entry("eip150", createSchedule(new StubGenesisConfigOptions().eip150Block(0))),
        Map.entry("eip158", createSchedule(new StubGenesisConfigOptions().eip158Block(0))),
        Map.entry("byzantium", createSchedule(new StubGenesisConfigOptions().byzantiumBlock(0))),
        Map.entry(
            "constantinople",
            createSchedule(new StubGenesisConfigOptions().constantinopleBlock(0))),
        Map.entry(
            "constantinoplefix", createSchedule(new StubGenesisConfigOptions().petersburgBlock(0))),
        Map.entry("petersburg", createSchedule(new StubGenesisConfigOptions().petersburgBlock(0))),
        Map.entry(
            "istanbul",
            createSchedule(new StubGenesisConfigOptions().istanbulBlock(0).chainId(chainId))),
        Map.entry(
            "muirglacier",
            createSchedule(new StubGenesisConfigOptions().muirGlacierBlock(0).chainId(chainId))),
        Map.entry(
            "berlin",
            createSchedule(new StubGenesisConfigOptions().berlinBlock(0).chainId(chainId))),
        Map.entry(
            "london",
            createSchedule(
                new StubGenesisConfigOptions()
                    .londonBlock(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "arrowglacier",
            createSchedule(
                new StubGenesisConfigOptions()
                    .arrowGlacierBlock(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "grayglacier",
            createSchedule(
                new StubGenesisConfigOptions()
                    .grayGlacierBlock(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "merge",
            createSchedule(
                new StubGenesisConfigOptions()
                    .mergeNetSplitBlock(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "shanghai",
            createSchedule(
                new StubGenesisConfigOptions()
                    .shanghaiTime(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "cancun",
            createSchedule(
                new StubGenesisConfigOptions().cancunTime(0).baseFeePerGas(0x0a).chainId(chainId))),
        Map.entry(
            "cancuneof",
            createSchedule(
                new StubGenesisConfigOptions()
                    .cancunEOFTime(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "prague",
            createSchedule(
                new StubGenesisConfigOptions()
                    .pragueTime(0)
                    .osakaTime(0) // TODO remove this once osaka_devnet_0 launches
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "osaka",
            createSchedule(
                new StubGenesisConfigOptions().osakaTime(0).baseFeePerGas(0x0a).chainId(chainId))),
        Map.entry(
            "futureeips",
            createSchedule(
                new StubGenesisConfigOptions()
                    .futureEipsTime(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))),
        Map.entry(
            "experimentaleips",
            createSchedule(
                new StubGenesisConfigOptions()
                    .experimentalEipsTime(0)
                    .baseFeePerGas(0x0a)
                    .chainId(chainId))));
  }

  private static Supplier<ProtocolSchedule> createSchedule(final GenesisConfigOptions options) {
    return () ->
        new ProtocolScheduleBuilder(
                options,
                options.getChainId(),
                ProtocolSpecAdapters.create(0, Function.identity()),
                PrivacyParameters.DEFAULT,
                false,
                EvmConfiguration.DEFAULT,
                MiningConfiguration.MINING_DISABLED,
                new BadBlockManager(),
                false,
                new NoOpMetricsSystem())
            .createProtocolSchedule();
  }
}
