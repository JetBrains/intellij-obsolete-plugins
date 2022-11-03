package com.intellij.dmserver.osmorc;

import com.intellij.dmserver.install.impl.DMServerLibraryFinder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class DMSourceBundleFinder extends DMServerLibraryFinder {
  public boolean containsOnlySources(@NotNull VirtualFile libraryClassesCandidate) {
    return isSourcesBundle(libraryClassesCandidate);
  }
}
