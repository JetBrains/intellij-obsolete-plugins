package com.intellij.jboss.bpmn.jbpm.chart.editor;

import com.intellij.jboss.bpmn.jbpm.model.BpmnDomModelManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider;
import org.jetbrains.annotations.NotNull;

final class BpmnDesignerFileEditorProvider extends PerspectiveFileEditorProvider {
  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    return ReadAction.compute(() -> {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      return psiFile instanceof XmlFile && BpmnDomModelManager.getInstance(project).isBpmnDomModel((XmlFile)psiFile);
    });
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  @Override
  public boolean acceptRequiresReadAction() {
    return false;
  }

  @Override
  public @NotNull PerspectiveFileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new BpmnDesignerFileEditor(project, file);
  }

  @Override
  public double getWeight() {
    return 0;
  }
}
