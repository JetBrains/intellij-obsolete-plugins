package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.libraries.BundleWrapper;
import com.intellij.dmserver.libraries.ServerLibrariesContext;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

final class BundleFolderElement extends RepositoryFolderElementBase {
  private static final Comparator<BundleWrapper> BY_BUNDLE_FILE_NAME = (o1, o2) -> {
    String name1 = o1.getJarFile().getName();
    String name2 = o2.getJarFile().getName();
    return name1.compareTo(name2);
  };

  public BundleFolderElement(ServerLibrariesContext context, RepositoryPattern repositoryPattern) {
    super(context, repositoryPattern, "bundles-folder");
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(DmServerBundle.message("BundleFolderElement.name"));
    updateIcons(presentation);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    List<BundleItemElementBase<?>> result = new ArrayList<>();

    List<BundleWrapper> bundles = new ArrayList<>(
      AvailableBundlesProvider.getInstance(getProject()).getRepositoryBundles(getRepositoryPattern()));
    bundles.sort(BY_BUNDLE_FILE_NAME);

    for (BundleWrapper bundle : bundles) {
      result.add(new ExistingJarElement(getProject(), bundle));
    }

    return result;
  }
}
