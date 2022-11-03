package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.libraries.ServerLibrariesContext;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public final class RepositoryElement extends ArtifactFolderElementBase {
  private final RepositoryPattern myRepositoryPattern;

  public RepositoryElement(ServerLibrariesContext context, RepositoryPattern repositoryPattern) {
    super(context, repositoryPattern.getFullPattern());
    myRepositoryPattern = repositoryPattern;
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    return Arrays.asList(new LibraryFolderElement(getContext(), myRepositoryPattern),
                         new BundleFolderElement(getContext(), myRepositoryPattern));
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(myRepositoryPattern.getSource().getPath());  // TODO: may show more details
    updateIcons(presentation);
  }
}
