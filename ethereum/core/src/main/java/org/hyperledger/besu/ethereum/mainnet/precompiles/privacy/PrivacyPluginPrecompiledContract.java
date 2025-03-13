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
package org.idnecology.idn.ethereum.mainnet.precompiles.privacy;

import static org.idnecology.idn.ethereum.trie.diffbased.common.provider.WorldStateQueryParams.withStateRootAndUpdateNodeHead;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.MutableWorldState;
import org.idnecology.idn.ethereum.core.PrivacyParameters;
import org.idnecology.idn.ethereum.mainnet.PrivateStateUtils;
import org.idnecology.idn.ethereum.privacy.PrivateTransaction;
import org.idnecology.idn.ethereum.privacy.PrivateTransactionReceipt;
import org.idnecology.idn.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.idnecology.idn.ethereum.processing.TransactionProcessingResult;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.gascalculator.GasCalculator;
import org.idnecology.idn.evm.worldstate.WorldUpdater;

import java.util.Optional;
import javax.annotation.Nonnull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(since = "24.12.0")
public class PrivacyPluginPrecompiledContract extends PrivacyPrecompiledContract {
  private static final Logger LOG = LoggerFactory.getLogger(PrivacyPluginPrecompiledContract.class);
  private final PrivacyParameters privacyParameters;

  public PrivacyPluginPrecompiledContract(
      final GasCalculator gasCalculator, final PrivacyParameters privacyParameters) {
    super(gasCalculator, privacyParameters, "PluginPrivacy");
    this.privacyParameters = privacyParameters;
  }

  @Nonnull
  @Override
  public PrecompileContractResult computePrecompile(
      final Bytes input, @Nonnull final MessageFrame messageFrame) {
    if (skipContractExecution(messageFrame)) {
      return NO_RESULT;
    }

    final Optional<org.idnecology.idn.plugin.data.PrivateTransaction> pluginPrivateTransaction =
        privacyParameters
            .getPrivacyService()
            .getPayloadProvider()
            .getPrivateTransactionFromPayload(
                messageFrame.getContextVariable(PrivateStateUtils.KEY_TRANSACTION));

    if (pluginPrivateTransaction.isEmpty()) {
      return NO_RESULT;
    }

    final PrivateTransaction privateTransaction =
        PrivateTransaction.readFrom(pluginPrivateTransaction.get());

    final Bytes32 privacyGroupId = privateTransaction.determinePrivacyGroupId();
    final Hash pmtHash = messageFrame.getContextVariable(PrivateStateUtils.KEY_TRANSACTION_HASH);

    LOG.debug(
        "Processing unrestricted private transaction {} in privacy group {}",
        pmtHash,
        privacyGroupId);

    final PrivateMetadataUpdater privateMetadataUpdater =
        messageFrame.getContextVariable(PrivateStateUtils.KEY_PRIVATE_METADATA_UPDATER);
    final Hash lastRootHash =
        privateStateRootResolver.resolveLastStateRoot(privacyGroupId, privateMetadataUpdater);

    final MutableWorldState disposablePrivateState =
        privateWorldStateArchive.getWorldState(withStateRootAndUpdateNodeHead(lastRootHash)).get();

    final WorldUpdater privateWorldStateUpdater = disposablePrivateState.updater();

    maybeApplyGenesisToPrivateWorldState(
        lastRootHash,
        disposablePrivateState,
        privateWorldStateUpdater,
        privacyGroupId,
        messageFrame.getBlockValues().getNumber());

    final TransactionProcessingResult result =
        processPrivateTransaction(
            messageFrame, privateTransaction, privacyGroupId, privateWorldStateUpdater);

    if (result.isInvalid() || !result.isSuccessful()) {
      LOG.error(
          "Failed to process unrestricted private transaction {}: {}",
          pmtHash,
          result.getValidationResult().getErrorMessage());

      privateMetadataUpdater.putTransactionReceipt(pmtHash, new PrivateTransactionReceipt(result));

      return NO_RESULT;
    }

    if (messageFrame.getContextVariable(PrivateStateUtils.KEY_IS_PERSISTING_PRIVATE_STATE, false)) {

      privateWorldStateUpdater.commit();
      disposablePrivateState.persist(null);

      storePrivateMetadata(
          pmtHash, privacyGroupId, disposablePrivateState, privateMetadataUpdater, result);
    }

    return new PrecompileContractResult(
        result.getOutput(), true, MessageFrame.State.CODE_EXECUTING, Optional.empty());
  }
}
