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

import org.idnecology.idn.plugin.services.metrics.Counter;

import java.util.concurrent.atomic.AtomicLong;

/** Counter that triggers a specific task each time a step is hit. */
public class RunnableCounter implements Counter {

  /** The Backed counter. */
  protected final Counter backedCounter;

  /** The Task. */
  protected final Runnable task;

  /** The Step. */
  protected final int step;

  /** The Step counter. */
  protected final AtomicLong stepCounter;

  /**
   * Instantiates a new Runnable counter.
   *
   * @param backedCounter the backed counter
   * @param task the task
   * @param step the step
   */
  public RunnableCounter(final Counter backedCounter, final Runnable task, final int step) {
    this.backedCounter = backedCounter;
    this.task = task;
    this.step = step;
    this.stepCounter = new AtomicLong(0);
  }

  /**
   * Increments the stepCounter by 1
   *
   * <p>{@link #inc(long) inc} method
   */
  @Override
  public void inc() {
    this.inc(1);
  }

  /**
   * Increments the stepCounter by amount. Triggers the runnable if the step is hit.
   *
   * @param amount the value to add to the stepCounter.
   */
  @Override
  public void inc(final long amount) {
    backedCounter.inc(amount);
    if (stepCounter.addAndGet(amount) % step == 0) {
      task.run();
    }
  }

  /**
   * Get Step Counter.
   *
   * @return the long
   */
  public long get() {
    return stepCounter.get();
  }
}
