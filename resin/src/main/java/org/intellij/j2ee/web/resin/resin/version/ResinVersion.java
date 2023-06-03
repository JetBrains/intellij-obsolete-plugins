package org.intellij.j2ee.web.resin.resin.version;

import com.intellij.javaee.oss.util.Version;
import com.intellij.openapi.util.io.FileUtil;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class ResinVersion {

  private final String name; // for debug only
  private final String versionNumber;

  protected ResinVersion(String name, @NonNls String versionNumber) {
    this.name = name;
    this.versionNumber = versionNumber;
  }

  public String toString() {
    return name;
  }

  public String getVersionNumber() {
    return versionNumber;
  }

  public Version getParsed() {
    return new Version(versionNumber);
  }

  public boolean equals(Object obj) {
    return obj instanceof ResinVersion && this.toString().equals(obj.toString());
  }

  public abstract boolean isOfVersion(File resinHome);

  @Nullable
  @NonNls
  public abstract String getStartupClass();

  public abstract boolean allowXdebug();

  public abstract boolean allowJmx();

  //Inner implementation classes
  public static final ResinVersion VERSION_2_X = new ResinVersion(ResinBundle.message("resin.version.fallback.v2"), "2.x") {

    @Override
    public boolean isOfVersion(File resinHome) {
      return new File(resinHome, FileUtil.toSystemDependentName("lib/jsdk23.jar")).exists();
    }

    @Override
    public String getStartupClass() {
      return "com.caucho.server.http.HttpServer";
    }

    @Override
    public boolean allowXdebug() {
      return false;
    }

    @Override
    public boolean allowJmx() {
      return false;
    }
  };
  public static final ResinVersion VERSION_3_X = new ResinVersion(ResinBundle.message("resin.version.fallback.v3"), "3.x") {

    @Override
    public boolean isOfVersion(File resinHome) {
      return new File(resinHome, FileUtil.toSystemDependentName("lib/jsdk-24.jar")).exists();
    }

    @Override
    public String getStartupClass() {
      return "com.caucho.server.resin.Resin";
    }

    @Override
    public boolean allowXdebug() {
      return true;
    }

    @Override
    public boolean allowJmx() {
      return true;
    }
  };
  public static final ResinVersion UNKNOWN_VERSION = new ResinVersion(ResinBundle.message("resin.version.fallback.vUnknown"), "unknown") {

    @Override
    public boolean isOfVersion(File resinHome) {
      return !VERSION_2_X.isOfVersion(resinHome) && !VERSION_3_X.isOfVersion(resinHome);
    }

    @Override
    @Nullable
    public String getStartupClass() {
      return null;
    }

    @Override
    public boolean allowXdebug() {
      return false;
    }

    @Override
    public boolean allowJmx() {
      return false;
    }
  };
}
