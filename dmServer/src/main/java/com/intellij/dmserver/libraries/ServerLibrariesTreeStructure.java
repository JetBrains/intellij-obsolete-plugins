package com.intellij.dmserver.libraries;

import com.intellij.dmserver.libraries.tree.RootElement;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.util.treeView.AbstractTreeStructureBase;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ServerLibrariesTreeStructure extends AbstractTreeStructureBase {

  private final RootElement myRootElement;

  public ServerLibrariesTreeStructure(ServerLibrariesContext context) {
    super(context.getProject());
    myRootElement = new RootElement(context);
  }

  @Override
  public List<TreeStructureProvider> getProviders() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Object getRootElement() {
    return myRootElement;
  }

  @Override
  public void commit() {

  }

  @Override
  public boolean hasSomethingToCommit() {
    return false;
  }
}
