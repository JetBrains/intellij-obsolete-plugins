package com.intellij.lang.puppet.project.meta;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class PuppetEnvironmentMetadata implements PuppetMetadata {
  private final @NotNull String myName;

  public PuppetEnvironmentMetadata(@NotNull VirtualFile envRoot) {
    myName = envRoot.getName();
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public @NotNull String getPresentableName() {
    return getName();
  }
}
