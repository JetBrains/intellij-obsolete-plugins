package com.intellij.gwt.dev.psiViewer;

import com.intellij.dev.psiViewer.PsiViewerExtension;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public final class GwtUiXmlPsiViewerExtension implements PsiViewerExtension {
  @Override
  public @NotNull String getName() {
    return GwtBundle.message("psi.viewer.name.gwt.ui.xml.file");
  }

  @Override
  public @NotNull Icon getIcon() {
    return GwtIcons.GoogleSmall;
  }

  @Override
  public @NotNull PsiElement createElement(@NotNull Project project, @NotNull String text) {
    PsiFileFactory fileFactory = PsiFileFactory.getInstance(project);
    return fileFactory.createFileFromText("Dummy.ui.xml", XmlFileType.INSTANCE, text, System.currentTimeMillis(), false);
  }

  @Override
  public @NotNull FileType getDefaultFileType() {
    return XmlFileType.INSTANCE;
  }
}
