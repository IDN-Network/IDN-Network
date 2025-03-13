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
package org.idnecology.idn.plugin.services.storage.rocksdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.metrics.ObservableMetricsSystem;
import org.idnecology.idn.plugin.services.metrics.Counter;
import org.idnecology.idn.plugin.services.metrics.LabelledMetric;
import org.idnecology.idn.plugin.services.metrics.OperationTimer;
import org.idnecology.idn.plugin.services.storage.rocksdb.configuration.RocksDBConfiguration;
import org.idnecology.idn.plugin.services.storage.rocksdb.configuration.RocksDBConfigurationBuilder;

import java.nio.file.Path;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Statistics;

@ExtendWith(MockitoExtension.class)
public class RocksDBMetricsTest {

  @Mock private ObservableMetricsSystem metricsSystemMock;
  @Mock private LabelledMetric<OperationTimer> labelledMetricOperationTimerMock;
  @Mock private LabelledMetric<Counter> labelledMetricCounterMock;
  @Mock private OperationTimer operationTimerMock;
  @Mock private OptimisticTransactionDB db;
  @Mock private Statistics stats;

  @TempDir public Path folder;

  @Test
  public void createStoreMustCreateMetrics() throws Exception {
    // Prepare mocks
    when(labelledMetricOperationTimerMock.labels(any())).thenReturn(operationTimerMock);
    when(metricsSystemMock.createLabelledTimer(
            eq(IdnMetricCategory.KVSTORE_ROCKSDB), anyString(), anyString(), any()))
        .thenReturn(labelledMetricOperationTimerMock);
    when(metricsSystemMock.createLabelledCounter(
            eq(IdnMetricCategory.KVSTORE_ROCKSDB), anyString(), anyString(), any()))
        .thenReturn(labelledMetricCounterMock);
    // Prepare argument captors
    final ArgumentCaptor<String> labelledTimersMetricsNameArgs =
        ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> labelledTimersHelpArgs = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> labelledCountersMetricsNameArgs =
        ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> labelledCountersHelpArgs = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> longGaugesMetricsNameArgs = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> longGaugesHelpArgs = ArgumentCaptor.forClass(String.class);

    RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS.create(metricsSystemMock, config(), db, stats);

    verify(metricsSystemMock, times(4))
        .createLabelledTimer(
            eq(IdnMetricCategory.KVSTORE_ROCKSDB),
            labelledTimersMetricsNameArgs.capture(),
            labelledTimersHelpArgs.capture(),
            any());
    assertThat(labelledTimersMetricsNameArgs.getAllValues())
        .containsExactly(
            "read_latency_seconds",
            "remove_latency_seconds",
            "write_latency_seconds",
            "commit_latency_seconds");
    assertThat(labelledTimersHelpArgs.getAllValues())
        .containsExactly(
            "Latency for read from RocksDB.",
            "Latency of remove requests from RocksDB.",
            "Latency for write to RocksDB.",
            "Latency for commits to RocksDB.");

    verify(metricsSystemMock, times(2))
        .createLongGauge(
            eq(IdnMetricCategory.KVSTORE_ROCKSDB),
            longGaugesMetricsNameArgs.capture(),
            longGaugesHelpArgs.capture(),
            any(LongSupplier.class));
    assertThat(longGaugesMetricsNameArgs.getAllValues())
        .containsExactly("rocks_db_table_readers_memory_bytes", "rocks_db_files_size_bytes");
    assertThat(longGaugesHelpArgs.getAllValues())
        .containsExactly(
            "Estimated memory used for RocksDB index and filter blocks in bytes",
            "Estimated database size in bytes");

    verify(metricsSystemMock)
        .createLabelledCounter(
            eq(IdnMetricCategory.KVSTORE_ROCKSDB),
            labelledCountersMetricsNameArgs.capture(),
            labelledCountersHelpArgs.capture(),
            any());
    assertThat(labelledCountersMetricsNameArgs.getValue()).isEqualTo("rollback_count");
    assertThat(labelledCountersHelpArgs.getValue())
        .isEqualTo("Number of RocksDB transactions rolled back.");
  }

  private RocksDBConfiguration config() throws Exception {
    return new RocksDBConfigurationBuilder().databaseDir(folder).build();
  }
}
