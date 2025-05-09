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
package org.idnecology.idn.ethereum.api.jsonrpc.websocket.subscription.request;

import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.FilterParameter;

import java.util.Objects;

public class PrivateSubscribeRequest extends SubscribeRequest {

  private final String privacyGroupId;
  private final String privacyUserId;

  public PrivateSubscribeRequest(
      final SubscriptionType subscriptionType,
      final FilterParameter filterParameter,
      final Boolean includeTransaction,
      final String connectionId,
      final String privacyGroupId,
      final String privacyUserId) {
    super(subscriptionType, filterParameter, includeTransaction, connectionId);
    this.privacyGroupId = privacyGroupId;
    this.privacyUserId = privacyUserId;
  }

  public String getPrivacyGroupId() {
    return privacyGroupId;
  }

  public String getPrivacyUserId() {
    return privacyUserId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final PrivateSubscribeRequest that = (PrivateSubscribeRequest) o;
    return privacyGroupId.equals(that.privacyGroupId) && privacyUserId.equals(that.privacyUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), privacyGroupId);
  }
}
