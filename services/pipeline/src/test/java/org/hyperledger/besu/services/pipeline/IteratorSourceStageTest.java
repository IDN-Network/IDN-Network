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
package org.idnecology.idn.services.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.idnecology.idn.metrics.noop.NoOpMetricsSystem.NO_OP_COUNTER;

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;

public class IteratorSourceStageTest {

  private final Pipe<String> output =
      new Pipe<>(10, NO_OP_COUNTER, NO_OP_COUNTER, NO_OP_COUNTER, "output_pipe");

  private final IteratorSourceStage<String> stage =
      new IteratorSourceStage<>("name", Iterators.forArray("a", "b", "c", "d"), output);

  @Test
  public void shouldOutputEntriesThenClosePipe() {
    stage.run();
    assertThat(output.isOpen()).isFalse();
    assertThat(output.hasMore()).isTrue();
    assertThat(output.get()).isEqualTo("a");
    assertThat(output.hasMore()).isTrue();
    assertThat(output.get()).isEqualTo("b");
    assertThat(output.hasMore()).isTrue();
    assertThat(output.get()).isEqualTo("c");
    assertThat(output.hasMore()).isTrue();
    assertThat(output.get()).isEqualTo("d");
    assertThat(output.hasMore()).isFalse();
  }
}
