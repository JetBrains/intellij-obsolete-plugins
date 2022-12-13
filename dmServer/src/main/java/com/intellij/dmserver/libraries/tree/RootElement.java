package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.libraries.ServerLibrariesContext;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RootElement extends AbstractTreeNode<ServerLibrariesContext> {

  public RootElement(@NotNull ServerLibrariesContext context) {
    super(context.getProject(), context);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    List<AbstractTreeNode<?>> result = new ArrayList<>();
    for (RepositoryPattern repositoryPattern : getValue().getInstallation().collectRepositoryPatterns()) {
      result.add(new RepositoryElement(getValue(), repositoryPattern));
    }
    return result;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {

  }
}
