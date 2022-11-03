package com.intellij.dmserver.libraries.tree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.osmorc.manifest.lang.psi.Clause;

public class UnparsableLibraryBundleElement extends BundleItemElementBase<Clause> {

  public UnparsableLibraryBundleElement(Project project, Clause clause) {
    super(project, clause);
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(getValue().getText().trim());
    updateIcons(presentation);
  }
}
