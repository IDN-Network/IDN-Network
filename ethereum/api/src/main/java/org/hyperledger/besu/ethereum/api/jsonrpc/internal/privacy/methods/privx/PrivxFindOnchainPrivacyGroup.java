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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.privx;

import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.privacy.PrivacyController;

// Use PrivxFindFlexiblePrivacyGroup instead
@Deprecated(since = "21.10.3")
public class PrivxFindOnchainPrivacyGroup extends PrivxFindFlexiblePrivacyGroup {

  public PrivxFindOnchainPrivacyGroup(
      final PrivacyController privacyController, final PrivacyIdProvider privacyIdProvider) {
    super(privacyController, privacyIdProvider);
  }

  @Override
  public String getName() {
    return RpcMethod.PRIVX_FIND_PRIVACY_GROUP_OLD.getMethodName();
  }
}
