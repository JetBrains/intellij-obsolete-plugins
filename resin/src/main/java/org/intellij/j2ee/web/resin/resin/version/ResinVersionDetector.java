package org.intellij.j2ee.web.resin.resin.version;

import java.io.File;

/**
 * This class is able to detect the version of the selected resin home
 */
public final class ResinVersionDetector {

  private ResinVersionDetector() {
  }

  /**
   * This method will try to retrieve the version of the selected Resin
   *
   * @param resinHome the resin home
   * @return ResinVersion representing the detected version. null if it was unable to detect it
   */
  public static ResinVersion getResinVersion(File resinHome) {
    ResinVersion version = ClassCallDetector.getResinVersion(resinHome);
    if (version == null) {
      version = FallbackDetector.getResinVersion(resinHome);
    }
    return version;
  }
}
