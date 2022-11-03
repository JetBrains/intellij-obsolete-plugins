package com.intellij.dmserver.libraries;

public class BundleDefinition {

  private final String mySymbolicName;
  private final String myVersion;

  public BundleDefinition(String symbolicName, String version) {
    mySymbolicName = symbolicName;
    myVersion = version;
  }

  public String getSymbolicName() {
    return mySymbolicName;
  }

  public String getVersion() {
    return myVersion;
  }
}
