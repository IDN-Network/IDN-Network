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
package org.idnecology.idn.metrics;

import org.idnecology.idn.plugin.services.metrics.MetricCategory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/** The enum Idn metric category. */
public enum IdnMetricCategory implements MetricCategory {
  /** Blockchain idn metric category. */
  BLOCKCHAIN("blockchain"),
  /** Ethereum idn metric category. */
  ETHEREUM("ethereum", false),
  /** Executors idn metric category. */
  EXECUTORS("executors"),
  /** Network idn metric category. */
  NETWORK("network"),
  /** Peers idn metric category. */
  PEERS("peers"),
  /** Permissioning idn metric category. */
  PERMISSIONING("permissioning"),
  /** Kvstore rocksdb idn metric category. */
  KVSTORE_ROCKSDB("rocksdb"),
  /** Kvstore private rocksdb idn metric category. */
  KVSTORE_PRIVATE_ROCKSDB("private_rocksdb"),
  /** Kvstore rocksdb stats idn metric category. */
  KVSTORE_ROCKSDB_STATS("rocksdb", false),
  /** Kvstore private rocksdb stats idn metric category. */
  KVSTORE_PRIVATE_ROCKSDB_STATS("private_rocksdb", false),
  /** Pruner idn metric category. */
  PRUNER("pruner"),
  /** Rpc idn metric category. */
  RPC("rpc"),
  /** Synchronizer idn metric category. */
  SYNCHRONIZER("synchronizer"),
  /** Transaction pool idn metric category. */
  TRANSACTION_POOL("transaction_pool"),
  /** Stratum idn metric category. */
  STRATUM("stratum"),
  /** Block processing idn metric category. */
  BLOCK_PROCESSING("block_processing");

  private static final Optional<String> BESU_PREFIX = Optional.of("idn_");

  /** The constant DEFAULT_METRIC_CATEGORIES. */
  public static final Set<MetricCategory> DEFAULT_METRIC_CATEGORIES;

  static {
    // Why not KVSTORE_ROCKSDB and KVSTORE_ROCKSDB_STATS, KVSTORE_PRIVATE_ROCKSDB_STATS,
    // KVSTORE_PRIVATE_ROCKSDB_STATS?  They hurt performance under load.
    final EnumSet<IdnMetricCategory> idnCategories =
        EnumSet.complementOf(
            EnumSet.of(
                KVSTORE_ROCKSDB,
                KVSTORE_ROCKSDB_STATS,
                KVSTORE_PRIVATE_ROCKSDB,
                KVSTORE_PRIVATE_ROCKSDB_STATS));

    DEFAULT_METRIC_CATEGORIES =
        ImmutableSet.<MetricCategory>builder()
            .addAll(idnCategories)
            .addAll(EnumSet.allOf(StandardMetricCategory.class))
            .build();
  }

  private final String name;
  private final boolean idnSpecific;

  IdnMetricCategory(final String name) {
    this(name, true);
  }

  IdnMetricCategory(final String name, final boolean idnSpecific) {
    this.name = name;
    this.idnSpecific = idnSpecific;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<String> getApplicationPrefix() {
    return idnSpecific ? BESU_PREFIX : Optional.empty();
  }
}
