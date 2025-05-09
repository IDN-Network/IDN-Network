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
package org.idnecology.idn.cli.options;

import static org.assertj.core.api.Assertions.assertThat;

import org.idnecology.idn.metrics.IdnMetricCategory;
import org.idnecology.idn.metrics.MetricCategoryRegistryImpl;
import org.idnecology.idn.metrics.StandardMetricCategory;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;

import java.util.EnumSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MetricsOptionsTest
    extends AbstractCLIOptionsTest<MetricsConfiguration.Builder, MetricsOptions> {
  private MetricCategoryRegistryImpl categoryRegistry;

  @BeforeEach
  public void setUp() {
    categoryRegistry = new MetricCategoryRegistryImpl();
    categoryRegistry.addCategories(IdnMetricCategory.class);
    categoryRegistry.addCategories(StandardMetricCategory.class);
  }

  @Override
  protected MetricsConfiguration.Builder createDefaultDomainObject() {
    return MetricsConfiguration.builder();
  }

  @Override
  protected MetricsConfiguration.Builder createCustomizedDomainObject() {
    return MetricsConfiguration.builder()
        .timersEnabled(!MetricsConfiguration.DEFAULT_METRICS_TIMERS_ENABLED)
        .idleTimeout(MetricsConfiguration.DEFAULT_METRICS_IDLE_TIMEOUT_SECONDS);
  }

  @Override
  protected MetricsOptions optionsFromDomainObject(
      final MetricsConfiguration.Builder domainObject) {
    final var options = MetricsOptions.fromConfiguration(domainObject.build());
    options.setMetricCategoryRegistry(categoryRegistry);
    return options;
  }

  @Override
  protected MetricsOptions getOptionsFromIdnCommand(final TestIdnCommand idnCommand) {
    return idnCommand.getMetricsOptions();
  }

  @Override
  protected String[] getNonOptionFields() {
    return new String[] {"metricCategoryRegistry"};
  }

  @Test
  public void enableRocksDbCategories() {
    final var rocksDbMetricsCategories =
        EnumSet.of(
            IdnMetricCategory.KVSTORE_ROCKSDB,
            IdnMetricCategory.KVSTORE_ROCKSDB_STATS,
            IdnMetricCategory.KVSTORE_PRIVATE_ROCKSDB,
            IdnMetricCategory.KVSTORE_PRIVATE_ROCKSDB_STATS);

    internalTestSuccess(
        metricsConfBuilder -> {
          assertThat(metricsConfBuilder.build().getMetricCategories())
              .containsExactlyInAnyOrderElementsOf(rocksDbMetricsCategories);
        },
        "--metrics-categories",
        rocksDbMetricsCategories.stream().map(Enum::name).collect(Collectors.joining(",")));
  }
}
