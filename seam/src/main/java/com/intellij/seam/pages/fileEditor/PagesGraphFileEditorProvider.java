package com.intellij.seam.pages.fileEditor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.pages.xml.PagesDomModelManager;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider;
import org.jetbrains.annotations.NotNull;

public class PagesGraphFileEditorProvider extends PerspectiveFileEditorProvider {

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    if (!file.isValid()) return false;
    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

    return psiFile instanceof XmlFile && PagesDomModelManager.getInstance(project).isPages((XmlFile)psiFile);
  }

  @Override
  @NotNull
  public PerspectiveFileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new PagesGraphFileEditor(project, file);
  }

  @Override
  public double getWeight() {
    return 0;
  }
}

