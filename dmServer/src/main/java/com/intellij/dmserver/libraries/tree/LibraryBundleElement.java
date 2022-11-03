package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.libraries.BundleDefinition;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class LibraryBundleElement extends ExistingBundleElementBase<String> {

  private final boolean myMissing;

  public LibraryBundleElement(Project project,
                              @NotNull BundleDefinition definition,
                              boolean missing,
                              VirtualFile bundleJar) {
    super(project, createKey(definition), definition, bundleJar);
    myMissing = missing;
  }

  @NonNls
  private static String createKey(@NotNull BundleDefinition definition) {
    return "library-bundle::" + definition.getSymbolicName() + "::" + definition.getVersion(); // + "::" + registered
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    @NlsSafe
    String name = getDefinition().getSymbolicName() + " (" + getDefinition().getVersion() + ")";
    if (myMissing) {
      name = DmServerBundle.message("LibraryBundleElement.name.missing", name);
    }

    updateStyledText(presentation, name, false);

    updateIcons(presentation);
  }
}
