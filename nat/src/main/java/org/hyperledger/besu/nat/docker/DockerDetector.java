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
package org.idnecology.idn.nat.docker;

import org.idnecology.idn.nat.NatMethod;
import org.idnecology.idn.nat.core.NatMethodDetector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/** The Docker detector. */
public class DockerDetector implements NatMethodDetector {
  /** Default constructor */
  public DockerDetector() {}

  @Override
  public Optional<NatMethod> detect() {
    try (Stream<String> stream = Files.lines(Paths.get("/proc/1/cgroup"))) {
      return stream
          .filter(line -> line.contains("/docker"))
          .findFirst()
          // fallback to looking for /.dockerenv in case we are running on Docker for Mac
          .or(() -> Optional.ofNullable(Files.exists(Paths.get("/.dockerenv")) ? "docker" : null))
          .map(__ -> NatMethod.DOCKER);
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
