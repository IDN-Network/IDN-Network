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
package org.idnecology.idn.services.tasks;

/**
 * The interface Task.
 *
 * @param <T> the type parameter
 */
public interface Task<T> {
  /**
   * Gets data.
   *
   * @return the data
   */
  T getData();

  /** Mark this task as completed. */
  void markCompleted();

  /** Mark this task as failed and requeue. */
  void markFailed();
}
