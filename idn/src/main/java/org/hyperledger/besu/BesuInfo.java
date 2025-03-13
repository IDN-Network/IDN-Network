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
package org.idnecology.idn;

import org.idnecology.idn.util.platform.PlatformDetector;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Represent Idn information such as version, OS etc. Used with --version option and during Idn
 * start.
 */
public final class IdnInfo {
  private static final String CLIENT = "idn";
  private static final String VERSION = IdnInfo.class.getPackage().getImplementationVersion();
  private static final String OS = PlatformDetector.getOS();
  private static final String VM = PlatformDetector.getVM();
  private static final String COMMIT;

  static {
    String className = IdnInfo.class.getSimpleName() + ".class";
    String classPath = IdnInfo.class.getResource(className).toString();

    String commit;
    try {
      URL url = new URL(classPath);
      JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
      Manifest manifest = jarConnection.getManifest();
      Attributes attributes = manifest.getMainAttributes();
      commit = attributes.getValue("Commit-Hash");
    } catch (Exception e) {
      commit = null;
    }
    COMMIT = commit;
  }

  private IdnInfo() {}

  /**
   * Generate version-only Idn version
   *
   * @return Idn version in format such as "v23.1.0" or "v23.1.1-dev-ac23d311"
   */
  public static String shortVersion() {
    return VERSION;
  }

  /**
   * Generate full Idn version
   *
   * @return Idn version in format such as "idn/v23.1.1-dev-ac23d311/osx-x86_64/graalvm-java-17"
   *     or "idn/v23.1.0/osx-aarch_64/corretto-java-19"
   */
  public static String version() {
    return String.format("%s/v%s/%s/%s", CLIENT, VERSION, OS, VM);
  }

  /**
   * Generate node name including identity.
   *
   * @param maybeIdentity optional node identity to include in the version string.
   * @return Version with optional identity if provided.
   */
  public static String nodeName(final Optional<String> maybeIdentity) {
    return maybeIdentity
        .map(identity -> String.format("%s/%s/v%s/%s/%s", CLIENT, identity, VERSION, OS, VM))
        .orElse(version());
  }

  /**
   * Generate the commit hash for this idn version
   *
   * @return the commit hash for this idn version
   */
  public static String commit() {
    return COMMIT;
  }
}
