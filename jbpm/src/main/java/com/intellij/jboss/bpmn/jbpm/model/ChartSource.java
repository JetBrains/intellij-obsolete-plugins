package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

public interface ChartSource {
  void addChangeListener(ChartSourceChangeListener listener);

  @Nullable
  VirtualFile getFile();

  @Nullable
  PsiFile getPsiFile();

  interface ChartSourceChangeListener extends EventListener {
    void chartSourceHasChanged();
  }
}
