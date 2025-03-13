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
package org.idnecology.idn.ethereum.eth.sync.fastsync.worldstate;

import org.idnecology.idn.ethereum.worldstate.WorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.worldstate.WorldStateStorageCoordinator;
import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.services.pipeline.Pipe;
import org.idnecology.idn.services.tasks.Task;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;

public class LoadLocalDataStep {

  private final WorldStateStorageCoordinator worldStateStorageCoordinator;
  private final Counter existingNodeCounter;

  public LoadLocalDataStep(
      final WorldStateStorageCoordinator worldStateStorageCoordinator,
      final MetricsSystem metricsSystem) {
    this.worldStateStorageCoordinator = worldStateStorageCoordinator;
    existingNodeCounter =
        metricsSystem.createCounter(
            IdnMetricCategory.SYNCHRONIZER,
            "world_state_existing_nodes_total",
            "Total number of node data requests completed using existing data");
  }

  public Stream<Task<NodeDataRequest>> loadLocalData(
      final Task<NodeDataRequest> task, final Pipe<Task<NodeDataRequest>> completedTasks) {
    final NodeDataRequest request = task.getData();
    final Optional<Bytes> existingData = request.getExistingData(worldStateStorageCoordinator);
    if (existingData.isPresent()) {
      existingNodeCounter.inc();
      request.setData(existingData.get());
      request.setRequiresPersisting(false);
      final WorldStateKeyValueStorage.Updater updater = worldStateStorageCoordinator.updater();
      request.persist(updater);
      updater.commit();

      completedTasks.put(task);
      return Stream.empty();
    }
    return Stream.of(task);
  }
}
