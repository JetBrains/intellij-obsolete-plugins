package com.intellij.dmserver.artifacts;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DMContainerPackagingElement extends WithModulePackagingElement {
  private static final Logger LOG = Logger.getInstance(DMContainerPackagingElement.class);
  @NonNls
  private static final String FOLDER_WEB_INF = "WEB-INF";
  @NonNls
  private static final String FOLDER_CLASSES = "classes";

  public DMContainerPackagingElement(@NotNull DMContainerPackagingElementType type, @NotNull Project project, @Nullable Module module) {
    super(type, project, module);
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return super.isEqualTo(element);
  }

  @NotNull
  @Override
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DMContainerPackagingElementPresentation(getModuleName());
  }

  @Override
  public boolean canBeRenamed() {
    return false;
  }

  @Override
  public String getName() {
    //TODO: where is it in UI
    return "";
  }

  @Override
  public void rename(@NotNull String newName) {
    throw new IllegalStateException("I told you I can't be renamed");
  }

  private void error(@NonNls String problem) {
    //throw new IllegalStateException(problem);
    //TODO: throwing is not allowed, in future there will be check() method in API to provide the validation
    //right now we can only log that
    LOG.warn(problem);
  }
}
