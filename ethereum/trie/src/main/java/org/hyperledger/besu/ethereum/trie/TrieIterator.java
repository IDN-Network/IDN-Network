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
package org.idnecology.idn.ethereum.trie;

import org.idnecology.idn.ethereum.trie.patricia.BranchNode;
import org.idnecology.idn.ethereum.trie.patricia.ExtensionNode;
import org.idnecology.idn.ethereum.trie.patricia.LeafNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;

public class TrieIterator<V> implements PathNodeVisitor<V> {

  private final Deque<Bytes> paths = new ArrayDeque<>();
  private final LeafHandler<V> leafHandler;
  private State state = State.SEARCHING;
  private final boolean unload;

  public TrieIterator(final LeafHandler<V> leafHandler, final boolean unload) {
    this.leafHandler = leafHandler;
    this.unload = unload;
  }

  @Override
  public Node<V> visit(final ExtensionNode<V> node, final Bytes searchPath) {
    Bytes remainingPath = searchPath;
    final Bytes extensionPath;
    final Bytes commonPrefixPath;
    if (state == State.SEARCHING) {
      extensionPath = node.getPath();
      commonPrefixPath = searchPath.slice(0, Math.min(searchPath.size(), extensionPath.size()));
      remainingPath = searchPath.slice(commonPrefixPath.size());
      if (node.getPath().compareTo(commonPrefixPath) > 0) {
        remainingPath = MutableBytes.create(remainingPath.size());
      }
    }

    paths.push(node.getPath());
    node.getChild().accept(this, remainingPath);
    if (unload) {
      node.getChild().unload();
    }
    paths.pop();
    return node;
  }

  @Override
  public Node<V> visit(final BranchNode<V> node, final Bytes searchPath) {
    byte iterateFrom = 0;
    Bytes remainingPath = searchPath;
    if (state == State.SEARCHING) {
      iterateFrom = searchPath.get(0);
      if (iterateFrom == CompactEncoding.LEAF_TERMINATOR) {
        return node;
      }
      remainingPath = searchPath.slice(1);
    }
    paths.push(node.getPath());
    for (int i = iterateFrom; i < node.maxChild() && state.continueIterating(); i++) {
      paths.push(Bytes.of(i));
      final Node<V> child = node.child((byte) i);
      if (i == iterateFrom) {
        child.accept(this, remainingPath);
      } else {
        child.accept(this, MutableBytes.create(remainingPath.size()));
      }

      if (unload) {
        child.unload();
      }
      paths.pop();
    }
    paths.pop();
    return node;
  }

  @Override
  public Node<V> visit(final LeafNode<V> node, final Bytes path) {
    paths.push(node.getPath());
    state = State.CONTINUE;
    state = leafHandler.onLeaf(keyHash(), node);
    paths.pop();
    return node;
  }

  @Override
  public Node<V> visit(final NullNode<V> node, final Bytes path) {
    state = State.CONTINUE;
    return node;
  }

  private Bytes32 keyHash() {
    final Iterator<Bytes> iterator = paths.descendingIterator();
    Bytes fullPath = iterator.next();
    while (iterator.hasNext()) {
      fullPath = Bytes.wrap(fullPath, iterator.next());
    }
    return fullPath.isZero()
        ? Bytes32.ZERO
        : Bytes32.wrap(CompactEncoding.pathToBytes(fullPath), 0);
  }

  public interface LeafHandler<V> {

    State onLeaf(Bytes32 keyHash, Node<V> node);
  }

  public enum State {
    SEARCHING,
    CONTINUE,
    STOP;

    public boolean continueIterating() {
      return this != STOP;
    }
  }
}
