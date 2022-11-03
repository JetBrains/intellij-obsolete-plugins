package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.libraries.LibraryDefinition;
import com.intellij.dmserver.libraries.ServerLibrariesContext;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class LibraryFolderElement extends RepositoryFolderElementBase {

  private static final Comparator<LibraryElement> BY_LIBRARY_FILE_NAME = (o1, o2) -> {
    String name1 = o1.getLibraryDefinition().getLibDefFile().getName();
    String name2 = o2.getLibraryDefinition().getLibDefFile().getName();
    return name1.compareTo(name2);
  };

  public LibraryFolderElement(ServerLibrariesContext context, RepositoryPattern repositoryPattern) {
    super(context, repositoryPattern, "libraries-folder");
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(DmServerBundle.message("LibraryFolderElement.name"));
    updateIcons(presentation);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    List<LibraryElement> result = new ArrayList<>();
    for (LibraryDefinition libraryDefinition
      : AvailableBundlesProvider.getInstance(getProject()).getRepositoryLibraries(getRepositoryPattern())) {
      result.add(new LibraryElement(getContext(), libraryDefinition));
    }

    result.sort(BY_LIBRARY_FILE_NAME);

    return result;
  }
}
