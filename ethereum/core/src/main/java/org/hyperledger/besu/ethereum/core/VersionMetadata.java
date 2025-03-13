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
package org.idnecology.idn.ethereum.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionMetadata implements Comparable<VersionMetadata> {
  private static final Logger LOG = LoggerFactory.getLogger(VersionMetadata.class);

  /** Represents an unknown Idn version in the version metadata file */
  public static final String BESU_VERSION_UNKNOWN = "UNKNOWN";

  private static final String METADATA_FILENAME = "VERSION_METADATA.json";
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final String idnVersion;

  /**
   * Get the version of Idn that is running.
   *
   * @return the version of Idn
   */
  public static String getRuntimeVersionString() {
    return VersionMetadata.class.getPackage().getImplementationVersion() == null
        ? BESU_VERSION_UNKNOWN
        : VersionMetadata.class.getPackage().getImplementationVersion();
  }

  public static VersionMetadata getRuntimeVersion() {
    return new VersionMetadata(getRuntimeVersionString());
  }

  @JsonCreator
  public VersionMetadata(@JsonProperty("idnVersion") final String idnVersion) {
    this.idnVersion = idnVersion;
  }

  public String getIdnVersion() {
    return idnVersion;
  }

  public static VersionMetadata lookUpFrom(final Path dataDir) throws IOException {
    LOG.info("Lookup version metadata file in data directory: {}", dataDir);
    return resolveVersionMetadata(getDefaultMetadataFile(dataDir));
  }

  public void writeToDirectory(final Path dataDir) throws IOException {
    MAPPER.writeValue(getDefaultMetadataFile(dataDir), this);
  }

  private static File getDefaultMetadataFile(final Path dataDir) {
    File metaDataFile = dataDir.resolve(METADATA_FILENAME).toFile();

    // Create the data dir here if it doesn't exist yet
    if (!metaDataFile.getParentFile().exists()) {
      LOG.info("Data directory {} does not exist - creating it", dataDir);
      metaDataFile.getParentFile().mkdirs();
    }
    return metaDataFile;
  }

  private static VersionMetadata resolveVersionMetadata(final File metadataFile)
      throws IOException {
    VersionMetadata versionMetadata;
    try {
      versionMetadata = MAPPER.readValue(metadataFile, VersionMetadata.class);
      LOG.info("Existing version data detected. Idn version {}", versionMetadata.idnVersion);
    } catch (FileNotFoundException fnfe) {
      versionMetadata = new VersionMetadata(BESU_VERSION_UNKNOWN);
    } catch (JsonProcessingException jpe) {
      throw new IllegalStateException(
          String.format("Invalid metadata file %s", metadataFile.getAbsolutePath()), jpe);
    }
    return versionMetadata;
  }

  /**
   * This function is designed to protect a Idn instance from being unintentionally started at a
   * version of Idn that might be incompatible with the version that last modified the specified
   * data directory. Currently this check is limited to checking that the version is >= the previous
   * version, to avoid accidentally running a lower version of Idn and potentially corrupting data,
   * but the method could be extended to perform any other version-to-version compatibility checks
   * necessary. If the --version-compatibility-protection flag is set to true and the compatibility
   * checks pass, the version metadata is updated to the current version of Idn.
   */
  public static void versionCompatibilityChecks(
      final boolean enforceCompatibilityProtection, final Path dataDir) throws IOException {
    final VersionMetadata metadataVersion = VersionMetadata.lookUpFrom(dataDir);
    final VersionMetadata runtimeVersion = getRuntimeVersion();
    if (metadataVersion.getIdnVersion().equals(VersionMetadata.BESU_VERSION_UNKNOWN)) {
      // The version isn't known, potentially because the file doesn't exist. Write the latest
      // version to the metadata file.
      LOG.info(
          "No version data detected. Writing Idn version {} to metadata file",
          runtimeVersion.getIdnVersion());
      runtimeVersion.writeToDirectory(dataDir);
    } else {
      // Check the runtime version against the most recent version as recorded in the version
      // metadata file
      final int versionComparison = runtimeVersion.compareTo(metadataVersion);
      if (versionComparison == 0) {
        // Versions match - no-op
      } else if (versionComparison < 0) {
        if (!enforceCompatibilityProtection) {
          LOG.warn(
              "Idn version {} is lower than version {} that last started. Allowing startup because --version-compatibility-protection has been disabled.",
              runtimeVersion.getIdnVersion(),
              metadataVersion.getIdnVersion());
          // We've allowed startup at an older version of Idn. Since the version in the metadata
          // file records the latest version of
          // Idn to write to the database we'll update the metadata version to this
          // downgraded-version.
          runtimeVersion.writeToDirectory(dataDir);
        } else {
          final String message =
              "Idn version "
                  + runtimeVersion.getIdnVersion()
                  + " is lower than version "
                  + metadataVersion.getIdnVersion()
                  + " that last started. Remove --version-compatibility-protection option to allow Idn to start at "
                  + " the lower version (warning - this may have unrecoverable effects on the database).";
          LOG.error(message);
          throw new IllegalStateException(message);
        }
      } else {
        LOG.info(
            "Idn version {} is higher than version {} that last started. Updating version metadata.",
            runtimeVersion.getIdnVersion(),
            metadataVersion.getIdnVersion());
        runtimeVersion.writeToDirectory(dataDir);
      }
    }
  }

  @Override
  public int compareTo(@Nonnull final VersionMetadata versionMetadata) {
    final String thisVersion = this.getIdnVersion().split("-", 2)[0];
    final String metadataVersion = versionMetadata.getIdnVersion().split("-", 2)[0];
    return new ComparableVersion(thisVersion).compareTo(new ComparableVersion(metadataVersion));
  }
}
