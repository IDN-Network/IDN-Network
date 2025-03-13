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
package org.idnecology.idn.ethereum.trie.diffbased.bonsai;

import org.idnecology.idn.datatypes.AccountValue;
import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.rlp.RLPException;
import org.idnecology.idn.ethereum.rlp.RLPInput;
import org.idnecology.idn.ethereum.rlp.RLPOutput;
import org.idnecology.idn.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.idnecology.idn.ethereum.trie.diffbased.common.DiffBasedAccount;
import org.idnecology.idn.ethereum.trie.diffbased.common.worldview.DiffBasedWorldView;
import org.idnecology.idn.evm.ModificationNotAllowedException;
import org.idnecology.idn.evm.account.AccountStorageEntry;
import org.idnecology.idn.evm.worldstate.UpdateTrackingAccount;

import java.util.NavigableMap;
import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class BonsaiAccount extends DiffBasedAccount {
  private Hash storageRoot;

  public BonsaiAccount(
      final DiffBasedWorldView context,
      final Address address,
      final Hash addressHash,
      final long nonce,
      final Wei balance,
      final Hash storageRoot,
      final Hash codeHash,
      final boolean mutable) {
    super(context, address, addressHash, nonce, balance, codeHash, mutable);
    this.storageRoot = storageRoot;
  }

  public BonsaiAccount(
      final DiffBasedWorldView context,
      final Address address,
      final AccountValue stateTrieAccount,
      final boolean mutable) {
    super(
        context,
        address,
        address.addressHash(),
        stateTrieAccount.getNonce(),
        stateTrieAccount.getBalance(),
        stateTrieAccount.getCodeHash(),
        mutable);
    this.storageRoot = stateTrieAccount.getStorageRoot();
  }

  public BonsaiAccount(final BonsaiAccount toCopy) {
    this(toCopy, toCopy.context, false);
  }

  public BonsaiAccount(
      final BonsaiAccount toCopy, final DiffBasedWorldView context, final boolean mutable) {
    super(
        context,
        toCopy.address,
        toCopy.addressHash,
        toCopy.nonce,
        toCopy.balance,
        toCopy.codeHash,
        toCopy.code,
        mutable);
    this.storageRoot = toCopy.storageRoot;
    updatedStorage.putAll(toCopy.updatedStorage);
  }

  public BonsaiAccount(
      final DiffBasedWorldView context, final UpdateTrackingAccount<BonsaiAccount> tracked) {
    super(
        context,
        tracked.getAddress(),
        tracked.getAddressHash(),
        tracked.getNonce(),
        tracked.getBalance(),
        tracked.getCodeHash(),
        tracked.getCode(),
        true);
    this.storageRoot = Hash.EMPTY_TRIE_HASH;
    updatedStorage.putAll(tracked.getUpdatedStorage());
  }

  public static BonsaiAccount fromRLP(
      final DiffBasedWorldView context,
      final Address address,
      final Bytes encoded,
      final boolean mutable)
      throws RLPException {
    final RLPInput in = RLP.input(encoded);
    in.enterList();

    final long nonce = in.readLongScalar();
    final Wei balance = Wei.of(in.readUInt256Scalar());
    final Hash storageRoot = Hash.wrap(in.readBytes32());
    final Hash codeHash = Hash.wrap(in.readBytes32());

    in.leaveList();

    return new BonsaiAccount(
        context, address, address.addressHash(), nonce, balance, storageRoot, codeHash, mutable);
  }

  @Override
  public boolean isStorageEmpty() {
    return Hash.EMPTY_TRIE_HASH.equals(storageRoot);
  }

  @Override
  public NavigableMap<Bytes32, AccountStorageEntry> storageEntriesFrom(
      final Bytes32 startKeyHash, final int limit) {
    return ((BonsaiWorldStateKeyValueStorage) context.getWorldStateStorage())
        .storageEntriesFrom(this.addressHash, startKeyHash, limit);
  }

  @Override
  public void writeTo(final RLPOutput out) {
    out.startList();

    out.writeLongScalar(nonce);
    out.writeUInt256Scalar(balance);
    out.writeBytes(storageRoot);
    out.writeBytes(codeHash);

    out.endList();
  }

  @Override
  public Hash getStorageRoot() {
    return storageRoot;
  }

  public void setStorageRoot(final Hash storageRoot) {
    if (immutable) {
      throw new ModificationNotAllowedException();
    }
    this.storageRoot = storageRoot;
  }

  @Override
  public String toString() {
    return "AccountState{"
        + "address="
        + address
        + ", nonce="
        + nonce
        + ", balance="
        + balance
        + ", storageRoot="
        + storageRoot
        + ", codeHash="
        + codeHash
        + '}';
  }

  /**
   * Throws an exception if the two accounts represent different stored states
   *
   * @param source The bonsai account to compare
   * @param account The State Trie account to compare
   * @param context a description to be added to the thrown exceptions
   * @throws IllegalStateException if the stored values differ
   */
  public static void assertCloseEnoughForDiffing(
      final BonsaiAccount source, final AccountValue account, final String context) {
    if (source == null) {
      throw new IllegalStateException(context + ": source is null but target isn't");
    } else {
      if (source.nonce != account.getNonce()) {
        throw new IllegalStateException(context + ": nonces differ");
      }
      if (!Objects.equals(source.balance, account.getBalance())) {
        throw new IllegalStateException(context + ": balances differ");
      }
      if (!Objects.equals(source.storageRoot, account.getStorageRoot())) {
        throw new IllegalStateException(context + ": Storage Roots differ");
      }
    }
  }
}
