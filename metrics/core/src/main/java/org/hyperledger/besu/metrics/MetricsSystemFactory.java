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

import static org.idnecology.idn.metrics.MetricsProtocol.OPENTELEMETRY;
import static org.idnecology.idn.metrics.MetricsProtocol.PROMETHEUS;

import org.idnecology.idn.metrics.noop.NoOpMetricsSystem;
import org.idnecology.idn.metrics.opentelemetry.OpenTelemetrySystem;
import org.idnecology.idn.metrics.prometheus.MetricsConfiguration;
import org.idnecology.idn.metrics.prometheus.PrometheusMetricsSystem;

import java.util.concurrent.atomic.AtomicBoolean;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a new metric system based on configuration. */
public class MetricsSystemFactory {

  private static final Logger LOG = LoggerFactory.getLogger(MetricsSystemFactory.class);
  private static final AtomicBoolean globalOpenTelemetryDisabled = new AtomicBoolean(false);

  private MetricsSystemFactory() {}

  private static void disableGlobalOpenTelemetry() {
    if (!globalOpenTelemetryDisabled.compareAndExchange(false, true)) {
      GlobalOpenTelemetry.set(OpenTelemetry.noop());
    }
  }

  /**
   * Creates and starts a new metric system to observe the behavior of the client
   *
   * @param metricsConfiguration the configuration of the metric system
   * @return a new metric system
   */
  public static ObservableMetricsSystem create(final MetricsConfiguration metricsConfiguration) {
    LOG.trace("Creating a metric system with {}", metricsConfiguration.getProtocol());
    if (!metricsConfiguration.isEnabled() && !metricsConfiguration.isPushEnabled()) {
      disableGlobalOpenTelemetry();
      return new NoOpMetricsSystem();
    }
    if (PROMETHEUS.equals(metricsConfiguration.getProtocol())) {
      final PrometheusMetricsSystem metricsSystem =
          new PrometheusMetricsSystem(
              metricsConfiguration.getMetricCategories(), metricsConfiguration.isTimersEnabled());
      metricsSystem.init();
      disableGlobalOpenTelemetry();
      return metricsSystem;
    } else if (OPENTELEMETRY.equals(metricsConfiguration.getProtocol())) {
      final OpenTelemetrySystem metricsSystem =
          new OpenTelemetrySystem(
              metricsConfiguration.getMetricCategories(),
              metricsConfiguration.isTimersEnabled(),
              metricsConfiguration.getPrometheusJob(),
              true);
      metricsSystem.initDefaults();
      return metricsSystem;
    } else {
      throw new IllegalArgumentException(
          "Invalid metrics protocol " + metricsConfiguration.getProtocol());
    }
  }
}
