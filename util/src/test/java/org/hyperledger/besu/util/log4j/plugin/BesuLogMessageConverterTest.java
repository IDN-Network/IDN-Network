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
package org.idnecology.idn.util.log4j.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class IdnLogMessageConverterTest {

  @Test
  public void logCleanup() {
    final StringBuilder testDataBuilder = new StringBuilder("log ");
    for (int i = 0; i <= 0x001F; i++) {
      testDataBuilder.append((char) i);
    }
    for (int i = 0x007F; i <= 0x009F; i++) {
      testDataBuilder.append((char) i);
    }
    testDataBuilder.append((char) 0x0D).append((char) 0x0A).append("message");

    String testData = testDataBuilder.toString();
    String cleanedData = IdnLogMessageConverter.formatIdnLogMessage(testData);
    String expectedData = String.format("log %c%c%c%cmessage", 0x09, 0x0A, 0x0D, 0x0A);
    assertThat(cleanedData).isEqualTo(expectedData);
  }
}
