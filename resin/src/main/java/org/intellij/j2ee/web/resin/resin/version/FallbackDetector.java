package org.intellij.j2ee.web.resin.resin.version;

import java.io.File;

/**
 * This class is able to detect the version of the selected resin home.
 * The detection method is a simple text match with a jar file in the resin-home/lib
 */
final class FallbackDetector {

  /**
   * This method will try to retrieve the version of the selected Resin
   *
   * @param resinHome the resin home
   * @return ResinVersion representing the detected version. null if it was unable to detect it
   */
  public static ResinVersion getResinVersion(File resinHome) {
    //Old detection method version
    if (ResinVersion.VERSION_3_X.isOfVersion(resinHome)) {
      return ResinVersion.VERSION_3_X;
    }
    if (ResinVersion.VERSION_2_X.isOfVersion(resinHome)) {
      return ResinVersion.VERSION_2_X;
    }
    return ResinVersion.UNKNOWN_VERSION;
  }
}