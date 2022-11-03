package com.intellij.dmserver.libraries.tree;

import com.intellij.dmserver.libraries.BundleWrapper;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ExistingJarElement extends ExistingBundleElementBase<VirtualFile> {

  public ExistingJarElement(Project project, BundleWrapper bundle) {
    super(project, bundle.getJarFile(), bundle, bundle.getJarFile());
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    StringBuilder nameBuilder = new StringBuilder();

    String symbolicName = getDefinition().getSymbolicName();
    if (symbolicName == null) {
      symbolicName = getValue().getNameWithoutExtension();
    }
    nameBuilder.append(symbolicName);

    String version = getDefinition().getVersion();
    if (version != null) {
      nameBuilder.append(" (");
      nameBuilder.append(version);
      nameBuilder.append(")");
    }

    String name = nameBuilder.toString();

    presentation.setPresentableText(name);

    updateIcons(presentation);
  }
}
