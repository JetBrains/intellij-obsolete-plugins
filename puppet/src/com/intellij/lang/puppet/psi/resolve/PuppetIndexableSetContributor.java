package com.intellij.lang.puppet.psi.resolve;

import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.IndexableSetContributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

final class PuppetIndexableSetContributor extends IndexableSetContributor {
  @Override
  public @NotNull Set<VirtualFile> getAdditionalRootsToIndex() {
    VirtualFile stubsRoot = PuppetLibraryUtil.getStubsRoot();
    if (stubsRoot == null) {
      return Collections.emptySet();
    }
    return Collections.singleton(stubsRoot);
  }
}
