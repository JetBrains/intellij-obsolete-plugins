package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.libraries.ServerLibrariesContext;
import org.jetbrains.annotations.NonNls;

public abstract class RepositoryFolderElementBase extends ArtifactFolderElementBase {

  private final RepositoryPattern myRepositoryPattern;

  public RepositoryFolderElementBase(ServerLibrariesContext context, RepositoryPattern repositoryPattern, @NonNls String keyPrefix) {
    super(context, keyPrefix + "::" + repositoryPattern.getFullPattern());
    myRepositoryPattern = repositoryPattern;
  }

  protected final RepositoryPattern getRepositoryPattern() {
    return myRepositoryPattern;
  }
}
