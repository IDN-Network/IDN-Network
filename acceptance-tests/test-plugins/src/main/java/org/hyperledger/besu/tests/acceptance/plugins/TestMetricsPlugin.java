/*
 * Copyright contributors to Idn.
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
package org.idnecology.idn.tests.acceptance.plugins;

import org.idnecology.idn.plugin.IdnPlugin;
import org.idnecology.idn.plugin.ServiceManager;
import org.idnecology.idn.plugin.services.MetricsSystem;
import org.idnecology.idn.plugin.services.metrics.MetricCategory;
import org.idnecology.idn.plugin.services.metrics.MetricCategoryRegistry;

import java.util.Locale;
import java.util.Optional;

import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(IdnPlugin.class)
public class TestMetricsPlugin implements IdnPlugin {
  private static final Logger LOG = LoggerFactory.getLogger(TestMetricsPlugin.class);
  private ServiceManager serviceManager;

  @Override
  public void register(final ServiceManager context) {
    LOG.info("Registering TestMetricsPlugin");
    serviceManager = context;
    context
        .getService(MetricCategoryRegistry.class)
        .orElseThrow()
        .addMetricCategory(TestMetricCategory.TEST_METRIC_CATEGORY);
  }

  @Override
  public void start() {
    LOG.info("Starting TestMetricsPlugin");
    serviceManager
        .getService(MetricsSystem.class)
        .orElseThrow()
        .createGauge(
            TestMetricCategory.TEST_METRIC_CATEGORY,
            "test_metric",
            "Returns 1 on success",
            () -> 1.0);
  }

  @Override
  public void stop() {
    LOG.info("Stopping TestMetricsPlugin");
  }

  public enum TestMetricCategory implements MetricCategory {
    TEST_METRIC_CATEGORY;

    @Override
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Optional<String> getApplicationPrefix() {
      return Optional.of("plugin_test_");
    }
  }
}
