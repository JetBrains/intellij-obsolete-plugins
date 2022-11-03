package com.intellij.dmserver.editor;

import com.intellij.dmserver.util.VersionUtils;
import org.osgi.framework.Version;

public class ExportedUnitImpl implements ExportedUnit {

  private final String mySymbolicName;
  private final Version myVersion;

  public ExportedUnitImpl(String symbolicName, Version version) {
    mySymbolicName = symbolicName;
    myVersion = version;
  }

  public ExportedUnitImpl(String symbolicName, String version) {
    this(symbolicName, VersionUtils.loadVersion(version));
  }

  @Override
  public String getSymbolicName() {
    return mySymbolicName;
  }

  @Override
  public Version getVersion() {
    return myVersion;
  }
}
