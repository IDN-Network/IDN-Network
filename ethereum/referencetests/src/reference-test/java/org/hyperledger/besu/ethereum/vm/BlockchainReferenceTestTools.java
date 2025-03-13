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
package org.idnecology.idn.ethereum.vm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.idnecology.idn.ethereum.ProtocolContext;
import org.idnecology.idn.ethereum.chain.MutableBlockchain;
import org.idnecology.idn.ethereum.core.Block;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.core.BlockImporter;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.mainnet.BlockImportResult;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.ethereum.mainnet.HeaderValidationMode;
import org.idnecology.idn.ethereum.mainnet.ProtocolSpec;
import org.idnecology.idn.ethereum.referencetests.BlockchainReferenceTestCaseSpec;
import org.idnecology.idn.ethereum.referencetests.ReferenceTestProtocolSchedules;
import org.idnecology.idn.ethereum.rlp.RLPException;
import org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams;
import org.idnecology.idn.evm.EVM;
import org.idnecology.idn.evm.EvmSpecVersion;
import org.idnecology.idn.evm.account.AccountState;
import org.idnecology.idn.evm.internal.EvmConfiguration.WorldUpdaterMode;
import org.idnecology.idn.testutil.JsonTestParameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.tuweni.bytes.Bytes32;
import org.assertj.core.api.Assertions;

public class BlockchainReferenceTestTools {

  private static final List<String> NETWORKS_TO_RUN;

  static {
    final String networks =
        System.getProperty(
            "test.ethereum.blockchain.eips",
            "FrontierToHomesteadAt5,HomesteadToEIP150At5,HomesteadToDaoAt5,EIP158ToByzantiumAt5,CancunToPragueAtTime15k"
                + "Frontier,Homestead,EIP150,EIP158,Byzantium,Constantinople,ConstantinopleFix,Istanbul,Berlin,"
                + "London,Merge,Paris,Shanghai,Cancun,Prague,Osaka,Amsterdam,Bogota,Polis,Bangkok");
    NETWORKS_TO_RUN = Arrays.asList(networks.split(","));
  }

  private static final JsonTestParameters<?, ?> params =
      JsonTestParameters.create(BlockchainReferenceTestCaseSpec.class)
          .generator(
              (testName, fullPath, spec, collector) -> {
                final String eip = spec.getNetwork();
                collector.add(
                    testName + "[" + eip + "]", fullPath, spec, NETWORKS_TO_RUN.contains(eip));
              });

  static {
    if (NETWORKS_TO_RUN.isEmpty()) {
      params.ignoreAll();
    }

    // Consumes a huge amount of memory
    params.ignore("static_Call1MB1024Calldepth_d1g0v0_\\w+");
    params.ignore("ShanghaiLove_");

    // Absurd amount of gas, doesn't run in parallel
    params.ignore("randomStatetest94_\\w+");

    // Don't do time-consuming tests
    params.ignore("CALLBlake2f_MaxRounds");
    params.ignore("loopMul_");

    // Inconclusive fork choice rule, since in merge CL should be choosing forks and setting the
    // chain head.
    // Perfectly valid test pre-merge.
    params.ignore(
        "UncleFromSideChain_(Merge|Paris|Shanghai|Cancun|Prague|Osaka|Amsterdam|Bogota|Polis|Bangkok)");

    // EOF tests don't have Prague stuff like deposits right now
    params.ignore("/stEOF/");

    // These are for the older reference tests but EIP-2537 is covered by eip2537_bls_12_381_precompiles in the execution-spec-tests
    params.ignore("/stEIP2537/");
  }

  private BlockchainReferenceTestTools() {
    // utility class
  }

  public static Collection<Object[]> generateTestParametersForConfig(final String[] filePath) {
    return params.generate(filePath);
  }

  @SuppressWarnings("java:S5960") // this is actually test code
  public static void executeTest(final BlockchainReferenceTestCaseSpec spec) {
    final BlockHeader genesisBlockHeader = spec.getGenesisBlockHeader();
    final MutableWorldState worldState =
        spec.getWorldStateArchive()
            .getWorldState(WorldStateQueryParams.withStateRootAndBlockHashAndUpdateNodeHead(genesisBlockHeader.getStateRoot(), genesisBlockHeader.getHash()))
            .orElseThrow();

    final ProtocolSchedule schedule =
        ReferenceTestProtocolSchedules.getInstance().getByName(spec.getNetwork());

    final MutableBlockchain blockchain = spec.getBlockchain();
    final ProtocolContext context = spec.getProtocolContext();

    for (final BlockchainReferenceTestCaseSpec.CandidateBlock candidateBlock :
        spec.getCandidateBlocks()) {
      if (!candidateBlock.isExecutable()) {
        return;
      }

      try {
        final Block block = candidateBlock.getBlock();

        final ProtocolSpec protocolSpec = schedule.getByBlockHeader(block.getHeader());
        final BlockImporter blockImporter = protocolSpec.getBlockImporter();

        verifyJournaledEVMAccountCompatability(worldState, protocolSpec);

        final HeaderValidationMode validationMode =
            "NoProof".equalsIgnoreCase(spec.getSealEngine())
                ? HeaderValidationMode.LIGHT
                : HeaderValidationMode.FULL;
        final BlockImportResult importResult =
            blockImporter.importBlock(context, block, validationMode, validationMode);

        assertThat(importResult.isImported()).isEqualTo(candidateBlock.isValid());
      } catch (final RLPException e) {
        assertThat(candidateBlock.isValid()).isFalse();
      }
    }

    Assertions.assertThat(blockchain.getChainHeadHash()).isEqualTo(spec.getLastBlockHash());
  }

  static void verifyJournaledEVMAccountCompatability(
          final MutableWorldState worldState, final ProtocolSpec protocolSpec) {
    EVM evm = protocolSpec.getEvm();
    if (evm.getEvmConfiguration().worldUpdaterMode() == WorldUpdaterMode.JOURNALED) {
      assumeFalse(
              worldState
                      .streamAccounts(Bytes32.ZERO, Integer.MAX_VALUE).anyMatch(AccountState::isEmpty),
              "Journaled account configured and empty account detected");
      assumeFalse(EvmSpecVersion.SPURIOUS_DRAGON.compareTo(evm.getEvmVersion()) > 0,
              "Journaled account configured and fork prior to the merge specified");
    }
  }
}
