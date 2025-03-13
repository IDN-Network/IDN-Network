/*
 * Copyright contributors to Hyperledger Idn.
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
package org.idnecology.idn.util.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class LogUtilTest {

  static final String exceptionMessage = "test message";
  static final String contextMessage = "context message";

  @Test
  public void testMessageSummarizeStackTrace() {
    Throwable throwable = new Throwable(exceptionMessage);
    String namespace = "org.idnecology.idn";
    String result = LogUtil.summarizeStackTrace(contextMessage, throwable, namespace);
    var lines =
        assertFoundInTrace(result, LogUtil.BESU_NAMESPACE, contextMessage, exceptionMessage, 1);
    assertThat(lines.size()).isEqualTo(3);
  }

  @Test
  public void testSummarizeStackTraceToArbitrary() {
    Throwable throwable = new Throwable("test message");
    String namespace = "java.lang";
    String result = LogUtil.summarizeStackTrace(contextMessage, throwable, namespace);
    var lines = assertFoundInTrace(result, namespace, contextMessage, exceptionMessage, 1);
    assertThat(lines.size()).isGreaterThan(3);
  }

  @Test
  public void testSummarizeLambdaStackTrace() {
    String result = "";
    try {
      Supplier<Object> lambda = () -> idnStackHelper(null);
      assertThat(lambda.get()).isNotNull();
    } catch (Exception e) {
      String namespace = "org.idnecology.idn";
      result = LogUtil.summarizeStackTrace(contextMessage, e, namespace);
    }
    var lines =
        assertFoundInTrace(
            result, LogUtil.BESU_NAMESPACE, contextMessage, "NullPointerException", 1);
    assertThat(lines).hasSize(5);
  }

  @Test
  public void assertSummarizeStopsAtFirstIdn() {
    String result = "";
    try {
      Supplier<Object> lambda = () -> idnStackHelper((null));
      Supplier<Object> lambda2 = lambda::get;
      assertThat(lambda2.get()).isNotNull();
    } catch (Exception e) {
      result = LogUtil.summarizeIdnStackTrace("idnSummary", e);
    }
    var lines =
        assertFoundInTrace(
            result, LogUtil.BESU_NAMESPACE, "idnSummary", "NullPointerException", 1);
    assertThat(lines).hasSize(5);
    assertThat(lines.get(0)).contains("idnSummary");
  }

  @Test
  public void assertMaxDepth() {
    String result = "";
    try {
      recurseTimesAndThrow(30);
    } catch (Exception ex) {
      result = LogUtil.summarizeStackTrace("idnSummary", ex, "java.lang");
    }
    System.out.println(result);
    List<String> lines = Arrays.asList(result.split("\n"));
    assertThat(lines).hasSize(22);
    assertThat(lines.get(0)).contains("idnSummary");
  }

  private List<String> assertFoundInTrace(
      final String result,
      final String namespace,
      final String ctxMessage,
      final String excMessage,
      final int times) {
    System.out.println(result);
    assertThat(result).containsOnlyOnce(ctxMessage);
    assertThat(result).containsOnlyOnce(excMessage);

    List<String> lines = Arrays.asList(result.split("\n"));
    assertThat(
            lines.stream()
                .filter(s -> s.contains("at: "))
                .filter(s -> s.contains(namespace))
                .count())
        .isEqualTo(times);
    return lines;
  }

  @SuppressWarnings("InfiniteRecursion")
  private void recurseTimesAndThrow(final int times) {
    if (times < 1) {
      throw new RuntimeException("FakeStackOverflowError");
    }
    recurseTimesAndThrow(times - 1);
  }

  private Optional<Object> idnStackHelper(final Object o) {
    return Optional.of(o);
  }
}
