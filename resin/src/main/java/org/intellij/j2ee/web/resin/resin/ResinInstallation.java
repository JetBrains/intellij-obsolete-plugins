package org.intellij.j2ee.web.resin.resin;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.util.io.FileUtil;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.intellij.j2ee.web.resin.resin.version.ResinLibCollector;
import org.intellij.j2ee.web.resin.resin.version.ResinVersion;
import org.intellij.j2ee.web.resin.resin.version.ResinVersionDetector;

import java.io.File;

public final class ResinInstallation {

  public static ResinInstallation create(String homePath) throws ExecutionException {
    File home = new File(FileUtil.toSystemDependentName(homePath));
    if (!isExistingDir(home)) {
      throw new ExecutionException(ResinBundle.message("message.error.resin.home.doesnt.exist"));
    }
    File bin = new File(home, "bin");
    if (!isExistingDir(bin)) {
      throw new ExecutionException(ResinBundle.message("message.error.resin.bin.doesnt.exist"));
    }
    File lib = new File(home, "lib");
    if (!isExistingDir(lib)) {
      throw new ExecutionException(ResinBundle.message("message.error.resin.lib.doesnt.exist"));
    }
    return new ResinInstallation(home, lib);
  }

  private static boolean isExistingDir(File dirCandidate) {
    return dirCandidate.exists() && dirCandidate.isDirectory();
  }

  private final File myHome;

  private final File myLib;

  private ResinInstallation(File home, File lib) {
    myHome = home;
    myLib = lib;
  }

  public ResinVersion getVersion() {
    //New feature: auto detect resin version
    return ResinVersionDetector.getResinVersion(myHome);
  }

  public boolean isVersionDetected() {
    ResinVersion ver = getVersion();
    return ver != null && !ver.equals(ResinVersion.UNKNOWN_VERSION);
  }

  public File getResinHome() {
    return myHome;
  }

  public String getDisplayName() {
    return getVersion().toString();
  }

  public File[] getLibFiles(boolean all) {
    return ResinLibCollector.getLibFiles(myLib, all);
  }
}
