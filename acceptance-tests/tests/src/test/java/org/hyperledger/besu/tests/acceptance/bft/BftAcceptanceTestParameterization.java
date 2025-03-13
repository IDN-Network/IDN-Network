/*
 * Copyright 2020 ConsenSys AG.
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
package org.idnecology.idn.tests.acceptance.bft;

import org.idnecology.idn.plugin.services.storage.DataStorageFormat;
import org.idnecology.idn.tests.acceptance.dsl.node.IdnNode;
import org.idnecology.idn.tests.acceptance.dsl.node.configuration.IdnNodeFactory;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

public class BftAcceptanceTestParameterization {

  public static Stream<Arguments> getFactories() {
    return Stream.of(
        Arguments.of(
            "ibft2",
            new BftAcceptanceTestParameterization(
                IdnNodeFactory::createIbft2Node, IdnNodeFactory::createIbft2NodeWithValidators)),
        Arguments.of(
            "qbft",
            new BftAcceptanceTestParameterization(
                IdnNodeFactory::createQbftNode, IdnNodeFactory::createQbftNodeWithValidators)));
  }

  @FunctionalInterface
  public interface NodeCreator {

    IdnNode create(
        IdnNodeFactory factory, String name, boolean fixedPort, DataStorageFormat storageFormat)
        throws Exception;
  }

  @FunctionalInterface
  public interface FixedPortNodeCreator {

    IdnNode createFixedPort(IdnNodeFactory factory, String name, boolean fixedPort)
        throws Exception;
  }

  @FunctionalInterface
  public interface NodeWithValidatorsCreator {

    IdnNode create(IdnNodeFactory factory, String name, String[] validators) throws Exception;
  }

  private final NodeCreator creatorFn;
  private final NodeWithValidatorsCreator createorWithValidatorFn;

  public BftAcceptanceTestParameterization(
      final NodeCreator creatorFn, final NodeWithValidatorsCreator createorWithValidatorFn) {
    this.creatorFn = creatorFn;
    this.createorWithValidatorFn = createorWithValidatorFn;
  }

  public IdnNode createNode(IdnNodeFactory factory, String name) throws Exception {
    return creatorFn.create(factory, name, false, DataStorageFormat.FOREST);
  }

  public IdnNode createBonsaiNodeFixedPort(IdnNodeFactory factory, String name) throws Exception {
    return creatorFn.create(factory, name, true, DataStorageFormat.BONSAI);
  }

  public IdnNode createForestNodeFixedPort(IdnNodeFactory factory, String name) throws Exception {
    return creatorFn.create(factory, name, true, DataStorageFormat.FOREST);
  }

  public IdnNode createNodeWithValidators(
      IdnNodeFactory factory, String name, String[] validators) throws Exception {
    return createorWithValidatorFn.create(factory, name, validators);
  }
}
