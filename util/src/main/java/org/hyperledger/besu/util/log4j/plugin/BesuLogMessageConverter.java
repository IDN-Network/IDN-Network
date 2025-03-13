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
package org.idnecology.idn.util.log4j.plugin;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * Idn Log4j2 plugin for cleaner message logging.
 *
 * <p>Usage: In the pattern layout configuration, replace {@code %msg} with {@code %msgc}.
 */
@Plugin(name = "IdnLogMessageConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"msgc"})
public class IdnLogMessageConverter extends LogEventPatternConverter {

  private IdnLogMessageConverter() {
    super("IdnLogMessageConverter", null);
  }

  /**
   * Creates new instance of this class. Required by Log4j2.
   *
   * @param options Array of options
   * @return instance of this class
   */
  @SuppressWarnings("unused") // used by Log4j2
  public static IdnLogMessageConverter newInstance(final String[] options) {
    return new IdnLogMessageConverter();
  }

  @Override
  public void format(final LogEvent event, final StringBuilder toAppendTo) {
    final String filteredString = formatIdnLogMessage(event.getMessage().getFormattedMessage());
    toAppendTo.append(filteredString);
  }

  /**
   * Format Idn log message.
   *
   * @param input The log message
   * @return The formatted log message
   */
  public static String formatIdnLogMessage(final String input) {
    final StringBuilder builder = new StringBuilder(input.length());
    char prevChar = 0;

    for (int i = 0; i < input.length(); i++) {
      final char c = input.charAt(i);

      if (c == 0x0A) {
        if (prevChar == 0x0D) {
          builder.append(prevChar);
        }
        builder.append(c);
      } else if (c == 0x09 || !Character.isISOControl(c)) {
        builder.append(c);
      }

      prevChar = c;
    }

    return builder.toString();
  }
}
