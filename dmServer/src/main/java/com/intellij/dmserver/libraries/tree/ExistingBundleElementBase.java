package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.libraries.BundleDefinition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public abstract class ExistingBundleElementBase<T> extends BundleItemElementBase<T> {

  private final BundleDefinition myDefinition;
  private final VirtualFile myBundleJar;

  public ExistingBundleElementBase(Project project,
                                   @NotNull T key,
                                   BundleDefinition definition,
                                   VirtualFile bundleJar) {
    super(project, key);
    myDefinition = definition;
    myBundleJar = bundleJar;
  }

  public VirtualFile getBundleJar() {
    return myBundleJar;
  }

  public BundleDefinition getDefinition() {
    return myDefinition;
  }
}
