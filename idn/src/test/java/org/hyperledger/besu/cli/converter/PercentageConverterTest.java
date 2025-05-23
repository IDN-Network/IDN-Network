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
package org.idnecology.idn.cli.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.idnecology.idn.cli.converter.exception.PercentageConversionException;
import org.idnecology.idn.util.number.Percentage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PercentageConverterTest {

  private final PercentageConverter percentageConverter = new PercentageConverter();

  @Test
  public void assertThatConvertHandlesProperlyAValidString() throws PercentageConversionException {
    final Percentage percentage = percentageConverter.convert("58");
    assertThat(percentage).isNotNull();
    assertThat(percentage.getValue()).isEqualTo(58);
  }

  @Test
  public void assertThatConvertHandlesProperlyAnInvalidStringNotANumber() {
    final Throwable thrown = catchThrowable(() -> percentageConverter.convert("invalid"));
    assertThat(thrown).isInstanceOf(PercentageConversionException.class);
  }

  @Test
  public void assertThatConvertHandlesProperlyAnInvalidStringOutOfRange() {
    final Throwable thrown = catchThrowable(() -> percentageConverter.convert("150"));
    assertThat(thrown).isInstanceOf(PercentageConversionException.class);
  }
}
