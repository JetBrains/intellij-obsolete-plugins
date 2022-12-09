package com.intellij.dmserver.libraries.obr.data;

public class LibraryBundleData extends BundleData {

  private boolean myIncluded = false;

  public LibraryBundleData(String name, String version, String link) {
    super(name, version, link);
  }

  public void setIncluded(boolean included) {
    myIncluded = included;
  }

  public Boolean isIncluded() {
    return myIncluded;
  }
}
