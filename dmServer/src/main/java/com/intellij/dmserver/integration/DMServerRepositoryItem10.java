package com.intellij.dmserver.integration;

public class DMServerRepositoryItem10 extends DMServerRepositoryItemBase {

  @Override
  public RepositoryPattern createPattern(PathResolver pathResolver) {
    return RepositoryPattern.create(this, pathResolver.path2Absolute(getPath()));
  }
}
