package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.libraries.BundleDefinition;
import com.intellij.dmserver.libraries.BundleWrapper;
import com.intellij.dmserver.libraries.LibraryDefinition;
import com.intellij.dmserver.libraries.ServerLibrariesContext;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.manifest.lang.psi.Clause;

import java.util.*;

public final class LibraryElement extends ArtifactFolderElementBase {
  private static final Comparator<BundleDefinition> BY_SYMBOLIC_NAME = (o1, o2) -> {
    String name1 = o1.getSymbolicName();
    String name2 = o2.getSymbolicName();
    return name1.compareTo(name2);
  };

  private final LibraryDefinition myLibraryDefinition;

  public LibraryElement(ServerLibrariesContext context, LibraryDefinition libraryDefinition) {
    super(context, createKey(libraryDefinition));
    myLibraryDefinition = libraryDefinition;
  }

  public LibraryDefinition getLibraryDefinition() {
    return myLibraryDefinition;
  }

  @NonNls
  private static String createKey(LibraryDefinition libraryDefinition) {
    return "library::" + libraryDefinition.getLibDefFile().getPath();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(myLibraryDefinition.getSymbolicName() + " (" + myLibraryDefinition.getVersion() + ")");
    updateIcons(presentation);
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    List<BundleItemElementBase<?>> result = new ArrayList<>();

    // TODO: move into index
    Map<String, BundleWrapper> bundles = new HashMap<>();
    for (BundleWrapper bundle : AvailableBundlesProvider.getInstance(getProject()).getAllRepositoryBundles()) {
      bundles.put(createBundleKey(bundle.getSymbolicName(), bundle.getVersion()), bundle);
    }

    ArrayList<BundleDefinition> bundleDefs = new ArrayList<>(myLibraryDefinition.getBundleDefs());

    bundleDefs.sort(BY_SYMBOLIC_NAME);

    for (BundleDefinition bundleDef : bundleDefs) {
      BundleWrapper bundle = bundles.get(createBundleKey(bundleDef.getSymbolicName(), bundleDef.getVersion()));
      boolean missing = bundle == null;
      result.add(new LibraryBundleElement(getProject(), bundleDef, missing, missing ? null : bundle.getJarFile()));
    }

    for (Clause clause : myLibraryDefinition.getUnparsableClauses()) {
      result.add(new UnparsableLibraryBundleElement(getProject(), clause));
    }

    return result;
  }

  private static String createBundleKey(String symbolicName, String version) {
    return symbolicName + "::" + version;
  }
}

