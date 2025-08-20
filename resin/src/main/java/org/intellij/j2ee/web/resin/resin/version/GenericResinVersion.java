package org.intellij.j2ee.web.resin.resin.version;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Generic class for autodetected resin version
 */
public class GenericResinVersion extends ResinVersion {

  private final String myStartupClass;
  private final boolean myAllowXdebug;
  private final boolean myAllowJmx;

  public GenericResinVersion(String name, String versionNumber, @NotNull String startupClass, boolean allowXdebug, boolean allowJmx) {
    super(name, versionNumber);
    myStartupClass = startupClass;
    myAllowXdebug = allowXdebug;
    myAllowJmx = allowJmx;
  }

  @Override
  public boolean isOfVersion(File resinHome) {
    ResinVersion ver = ResinVersionDetector.getResinVersion(resinHome);
    return this.equals(ver);
  }

  @Override
  public String getStartupClass() {
    return myStartupClass;
  }

  @Override
  public boolean allowXdebug() {
    return myAllowXdebug;
  }

  @Override
  public boolean allowJmx() {
    return myAllowJmx;
  }
}
