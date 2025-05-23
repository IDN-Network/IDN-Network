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
package org.idnecology.idn.ethstats.report;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.BlockResult;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * This interface represents a history report. It provides methods to get the id and history of the
 * history report.
 */
@Value.Immutable
@Value.Style(allParameters = true)
@JsonSerialize(as = ImmutableHistoryReport.class)
@JsonDeserialize(as = ImmutableHistoryReport.class)
public interface HistoryReport {

  /**
   * Gets the id of the history report.
   *
   * @return the id of the history report.
   */
  @JsonProperty(value = "id")
  String getId();

  /**
   * Gets the block results of the history report.
   *
   * @return the list of block results of the history report.
   */
  @JsonProperty("history")
  List<BlockResult> getHistory();
}
