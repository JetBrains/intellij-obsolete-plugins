package com.intellij.dmserver.facet;

public class DMUnitDescriptor {
  private final DMUnitType myType;
  private final String mySymbolicName;
  private final String myVersion;

  public DMUnitDescriptor(DMUnitType type, String symbolicName, String version) {
    myType = type;
    mySymbolicName = symbolicName;
    myVersion = version;
  }

  public DMUnitType getType() {
    return myType;
  }

  public String getSymbolicName() {
    return mySymbolicName;
  }

  public String getVersion() {
    return myVersion;
  }
}
