package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.GwtUiXmlNamespaceCompletionContributor.XMLNS_VALUE_PATTERN;
import static com.intellij.gwt.uiBinder.GwtUiXmlNamespaceCompletionContributor.invokeNamespaceAutoPopup;
import static com.intellij.gwt.uiBinder.UiBinderUtil.isUiXmlFile;

public final class GwtUiXmlTypedHandler extends TypedHandlerDelegate {

  private static void autoPopupNamespaceLookup(Project project, final Editor editor) {
    AutoPopupController.getInstance(project).scheduleAutoPopup(editor, file -> {
      if (!(file instanceof XmlFile)) {
        return false;
      }
      if (!isUiXmlFile((XmlFile) file)) {
        return false;
      }

      int offset = editor.getCaretModel().getOffset();
      PsiElement psiElement = file.findElementAt(offset - 1);

      return XMLNS_VALUE_PATTERN.accepts(psiElement);
    });
  }

  @Override
  public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor,
                                         @NotNull PsiFile file, @NotNull FileType fileType) {
    if (file instanceof XmlFile && isUiXmlFile((XmlFile) file)) {
      if (invokeNamespaceAutoPopup(c)) {
        autoPopupNamespaceLookup(project, editor);
      }
    }
    return super.beforeCharTyped(c, project, editor, file, fileType);
  }
}
