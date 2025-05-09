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
package org.idnecology.idn.ethereum.eth.peervalidation;

import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.ethereum.core.BlockHeader;
import org.idnecology.idn.ethereum.eth.manager.EthPeer;
import org.idnecology.idn.ethereum.eth.manager.peertask.PeerTaskExecutor;
import org.idnecology.idn.ethereum.eth.sync.SynchronizerConfiguration;
import org.idnecology.idn.ethereum.mainnet.ProtocolSchedule;
import org.idnecology.idn.plugin.services.MetricsSystem;

public class CheckpointBlocksPeerValidator extends RequiredBlocksPeerValidator {

  public CheckpointBlocksPeerValidator(
      final ProtocolSchedule protocolSchedule,
      final PeerTaskExecutor peerTaskExecutor,
      final SynchronizerConfiguration synchronizerConfiguration,
      final MetricsSystem metricsSystem,
      final long blockNumber,
      final Hash hash,
      final long chainHeightEstimationBuffer) {
    super(
        protocolSchedule,
        peerTaskExecutor,
        synchronizerConfiguration,
        metricsSystem,
        blockNumber,
        hash,
        chainHeightEstimationBuffer);
  }

  public CheckpointBlocksPeerValidator(
      final ProtocolSchedule protocolSchedule,
      final PeerTaskExecutor peerTaskExecutor,
      final SynchronizerConfiguration synchronizerConfiguration,
      final MetricsSystem metricsSystem,
      final long blockNumber,
      final Hash hash) {
    this(
        protocolSchedule,
        peerTaskExecutor,
        synchronizerConfiguration,
        metricsSystem,
        blockNumber,
        hash,
        DEFAULT_CHAIN_HEIGHT_ESTIMATION_BUFFER);
  }

  @Override
  boolean validateBlockHeader(final EthPeer ethPeer, final BlockHeader header) {
    final boolean valid = super.validateBlockHeader(ethPeer, header);
    if (valid) {
      ethPeer.setCheckpointHeader(header);
    }
    return valid;
  }
}
