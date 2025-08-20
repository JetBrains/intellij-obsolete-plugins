package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ChartFileSource implements ChartSource {
  @NotNull
  private final VirtualFile virtualFile;

  public ChartFileSource(@NotNull VirtualFile file) {
    virtualFile = file;
  }

  @Nullable
  @Override
  public VirtualFile getFile() {
    return virtualFile;
  }
}
