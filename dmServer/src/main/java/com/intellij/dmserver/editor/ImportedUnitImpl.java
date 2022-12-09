package com.intellij.dmserver.editor;

import org.osgi.framework.VersionRange;

/**
 * @author michael.golubev
 */
public class ImportedUnitImpl implements ImportedUnit {
  private final String mySymbolicName;
  private VersionRange myVersionRange;
  private boolean myOptional;

  public ImportedUnitImpl(String symbolicName, VersionRange versionRange, boolean optional) {
    mySymbolicName = symbolicName;
    myVersionRange = versionRange;
    myOptional = optional;
  }

  @Override
  public String getSymbolicName() {
    return mySymbolicName;
  }

  @Override
  public VersionRange getVersionRange() {
    return myVersionRange;
  }

  @Override
  public void setVersionRange(VersionRange versionRange) {
    myVersionRange = versionRange;
  }

  @Override
  public boolean isOptional() {
    return myOptional;
  }

  @Override
  public void setOptional(boolean optional) {
    myOptional = optional;
  }
}
