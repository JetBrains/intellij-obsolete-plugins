package com.intellij.gwt.make;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class GwtCompilerWorkspaceConfiguration {
  public static GwtCompilerWorkspaceConfiguration getInstance(@NotNull Project project) {
    return project.getService(GwtCompilerWorkspaceConfiguration.class);
  }

  public abstract boolean isShowCompilerOutput(@NotNull GwtFacet facet);

  public abstract void setShowCompilerOutput(@NotNull GwtFacet facet, boolean show);
}
