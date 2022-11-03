package com.intellij.dmserver.integration;

public interface DMServerRepositoryItem {

  String getPath();

  void setPath(String path);

  RepositoryPattern createPattern(PathResolver pathResolver);
}
