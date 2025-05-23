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
package org.idnecology.idn.plugin.services.storage;

import org.idnecology.idn.plugin.Unstable;
import org.idnecology.idn.plugin.services.exception.StorageException;

import java.io.Closeable;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Responsible for storing values against keys.
 *
 * <p>Behaviour expected with regard to key to value mapping is that of a map, one key maps to one
 * value, when a new value is added with an existing key, that key now points at the new value.
 *
 * <p>All keys and values must be non-null.
 */
@Unstable
public interface KeyValueStorage extends Closeable {

  /**
   * Deletes all keys and values from the storage.
   *
   * @throws StorageException problem encountered when attempting to clear storage.
   */
  void clear() throws StorageException;

  /**
   * Whether the key-value storage contains the given key.
   *
   * @param key a key that might be contained in the key-value storage.
   * @return <code>true</code> when the given key is present in keyset, <code>false</code>
   *     otherwise.
   * @throws StorageException problem encountered when interacting with the key set.
   */
  boolean containsKey(byte[] key) throws StorageException;

  /**
   * Retrieves the value associated with a given key.
   *
   * @param key whose associated value is being retrieved.
   * @return an {@link Optional} containing the value associated with the specified key, otherwise
   *     empty.
   * @throws StorageException problem encountered during the retrieval attempt.
   */
  Optional<byte[]> get(byte[] key) throws StorageException;

  /**
   * Returns a stream of all keys and values.
   *
   * @return A stream of all keys and values in storage.
   * @throws StorageException problem encountered during the retrieval attempt.
   */
  Stream<Pair<byte[], byte[]>> stream() throws StorageException;

  /**
   * Returns a stream of key-value pairs starting from the specified key. This method is used to
   * retrieve a stream of data from the storage, starting from the given key. If no data is
   * available from the specified key onwards, an empty stream is returned.
   *
   * @param startKey The key from which the stream should start.
   * @return A stream of key-value pairs starting from the specified key.
   * @throws StorageException If an error occurs while accessing the storage.
   */
  Stream<Pair<byte[], byte[]>> streamFromKey(final byte[] startKey);

  /**
   * Returns a stream of key-value pairs starting from the specified key, ending at the specified
   * key. This method is used to retrieve a stream of data from the storage, starting from the given
   * key. If no data is available from the specified key onwards, an empty stream is returned.
   *
   * @param startKey The key from which the stream should start.
   * @param endKey The key at which the stream should stop.
   * @return A stream of key-value pairs starting from the specified key.
   */
  Stream<Pair<byte[], byte[]>> streamFromKey(final byte[] startKey, final byte[] endKey);

  /**
   * Returns a stream of all keys.
   *
   * @return A stream of all keys in storage.
   * @throws StorageException problem encountered during the retrieval attempt.
   */
  Stream<byte[]> streamKeys() throws StorageException;

  /**
   * Delete the value corresponding to the given key if a write lock can be instantly acquired on
   * the underlying storage. Do nothing otherwise.
   *
   * @param key The key to delete.
   * @return false if the lock on the underlying storage could not be instantly acquired, true
   *     otherwise
   * @throws StorageException any problem encountered during the deletion attempt.
   */
  boolean tryDelete(byte[] key) throws StorageException;

  /**
   * Performs an evaluation against each key in the store, returning the set of entries that pass.
   *
   * @param returnCondition predicate to evaluate each key against, unless the result is {@code
   *     null}, the key is added to the returned list of keys.
   * @return the set of keys that pass the condition.
   */
  Set<byte[]> getAllKeysThat(Predicate<byte[]> returnCondition);

  /**
   * Gets all values from keys that matches the predicate.
   *
   * @param returnCondition the return condition
   * @return the all values from keys that
   */
  Set<byte[]> getAllValuesFromKeysThat(final Predicate<byte[]> returnCondition);

  /**
   * Begins a fresh transaction, for sequencing operations for later atomic execution.
   *
   * @return transaction to sequence key-value operations.
   * @throws StorageException problem encountered when starting a new transaction.
   */
  KeyValueStorageTransaction startTransaction() throws StorageException;

  /**
   * Return Whether the underlying storage is closed.
   *
   * @return boolean indicating whether the storage is closed.
   */
  boolean isClosed();
}
