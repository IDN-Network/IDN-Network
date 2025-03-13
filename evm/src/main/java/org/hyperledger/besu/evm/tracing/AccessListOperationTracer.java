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
package org.idnecology.idn.evm.tracing;

import org.idnecology.idn.datatypes.AccessListEntry;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.evm.frame.MessageFrame;
import org.idnecology.idn.evm.operation.Operation.OperationResult;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Table;
import org.apache.tuweni.bytes.Bytes32;

/** The Access List Operation Tracer. */
public class AccessListOperationTracer extends EstimateGasOperationTracer {

  private Table<Address, Bytes32, Boolean> warmedUpStorage;

  /** Default constructor. */
  private AccessListOperationTracer() {
    super();
  }

  @Override
  public void tracePostExecution(final MessageFrame frame, final OperationResult operationResult) {
    super.tracePostExecution(frame, operationResult);
    warmedUpStorage = frame.getWarmedUpStorage();
  }

  /**
   * Get the access list.
   *
   * @return the access list
   */
  public List<AccessListEntry> getAccessList() {
    if (warmedUpStorage != null && !warmedUpStorage.isEmpty()) {
      final List<AccessListEntry> list = new ArrayList<>(warmedUpStorage.size());
      warmedUpStorage
          .rowMap()
          .forEach(
              (address, storageKeys) ->
                  list.add(new AccessListEntry(address, new ArrayList<>(storageKeys.keySet()))));
      return list;
    }
    return List.of();
  }

  /**
   * Create a AccessListOperationTracer.
   *
   * @return the AccessListOperationTracer
   */
  public static AccessListOperationTracer create() {
    return new AccessListOperationTracer();
  }
}
