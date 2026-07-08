package com.intellij.lang.puppet.project.roots;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public class PuppetDirectoryInfo {
  public static final PuppetDirectoryInfo UNAVAILABLE = new PuppetDirectoryInfo(null);

  private final @Nullable VirtualFile myPuppetRoot;

  public PuppetDirectoryInfo(@Nullable VirtualFile puppetRoot) {
    myPuppetRoot = puppetRoot;
  }

  public boolean isAvailable() {
    return getPuppetRoot() != null;
  }

  public @Nullable VirtualFile getPuppetRoot() {
    return myPuppetRoot;
  }
}
