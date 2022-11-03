package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.libraries.ServerLibrariesContext;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

public abstract class ArtifactFolderElementBase extends AbstractTreeNode<String> {

  private final ServerLibrariesContext myContext;

  public ArtifactFolderElementBase(ServerLibrariesContext context, @NotNull String key) {
    super(context.getProject(), key);
    myContext = context;
  }

  protected static void updateIcons(PresentationData presentation) {
    presentation.setIcon(PlatformIcons.FOLDER_ICON);
  }

  protected final ServerLibrariesContext getContext() {
    return myContext;
  }
}
