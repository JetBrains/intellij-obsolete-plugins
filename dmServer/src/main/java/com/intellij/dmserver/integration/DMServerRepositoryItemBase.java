package com.intellij.dmserver.integration;

public abstract class DMServerRepositoryItemBase implements DMServerRepositoryItem {

  private String myPath;

  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public void setPath(String path) {
    myPath = path;
  }
}
