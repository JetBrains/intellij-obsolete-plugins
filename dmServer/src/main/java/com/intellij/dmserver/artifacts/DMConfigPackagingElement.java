package com.intellij.dmserver.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DMConfigPackagingElement extends WithModulePackagingElement {

  public DMConfigPackagingElement(@NotNull DMConfigPackagingElementType type, @NotNull Project project, @Nullable Module module) {
    super(type, project, module);
  }

  @NotNull
  @Override
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DMConfigPackagingElementPresentation(getModuleName());
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
}
