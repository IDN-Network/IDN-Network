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
package org.idnecology.idn.ethereum.trie.patricia;

import static org.idnecology.idn.crypto.Hash.keccak256;

import org.idnecology.idn.ethereum.rlp.BytesValueRLPOutput;
import org.idnecology.idn.ethereum.rlp.RLP;
import org.idnecology.idn.ethereum.trie.CompactEncoding;
import org.idnecology.idn.ethereum.trie.LocationNodeVisitor;
import org.idnecology.idn.ethereum.trie.Node;
import org.idnecology.idn.ethereum.trie.NodeFactory;
import org.idnecology.idn.ethereum.trie.NodeVisitor;
import org.idnecology.idn.ethereum.trie.PathNodeVisitor;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class LeafNode<V> implements Node<V> {
  private final Optional<Bytes> location;
  private final Bytes path;
  protected final V value;
  private final NodeFactory<V> nodeFactory;
  protected final Function<V, Bytes> valueSerializer;
  protected WeakReference<Bytes> encodedBytes;
  private SoftReference<Bytes32> hash;
  private boolean dirty = false;

  public LeafNode(
      final Bytes location,
      final Bytes path,
      final V value,
      final NodeFactory<V> nodeFactory,
      final Function<V, Bytes> valueSerializer) {
    this.location = Optional.ofNullable(location);
    this.path = path;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  public LeafNode(
      final Bytes path,
      final V value,
      final NodeFactory<V> nodeFactory,
      final Function<V, Bytes> valueSerializer) {
    this.location = Optional.empty();
    this.path = path;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final Bytes path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    visitor.visit(this);
  }

  @Override
  public void accept(final Bytes location, final LocationNodeVisitor<V> visitor) {
    visitor.visit(location, this);
  }

  @Override
  public Optional<Bytes> getLocation() {
    return location;
  }

  @Override
  public Bytes getPath() {
    return path;
  }

  @Override
  public Optional<V> getValue() {
    return Optional.of(value);
  }

  @Override
  public List<Node<V>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public Bytes getEncodedBytes() {
    if (encodedBytes != null) {
      final Bytes encoded = encodedBytes.get();
      if (encoded != null) {
        return encoded;
      }
    }

    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    out.writeBytes(CompactEncoding.encode(path));
    out.writeBytes(valueSerializer.apply(value));
    out.endList();
    final Bytes encoded = out.encoded();
    encodedBytes = new WeakReference<>(encoded);
    return encoded;
  }

  @Override
  public Bytes getEncodedBytesRef() {
    if (isReferencedByHash()) {
      return RLP.encodeOne(getHash());
    } else {
      return getEncodedBytes();
    }
  }

  @Override
  public Bytes32 getHash() {
    if (hash != null) {
      final Bytes32 hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    final Bytes32 hashed = keccak256(getEncodedBytes());
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  @Override
  public Node<V> replacePath(final Bytes path) {
    return nodeFactory.createLeaf(path, value);
  }

  @Override
  public String print() {
    return "Leaf:"
        + "\n\tRef: "
        + getEncodedBytesRef()
        + "\n\tPath: "
        + CompactEncoding.encode(path)
        + "\n\tValue: "
        + getValue().map(Object::toString).orElse("empty");
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }

  @Override
  public boolean isHealNeeded() {
    return false;
  }

  @Override
  public void markHealNeeded() {
    // nothing to do a leaf don't have child
  }
}
