package com.intellij.gwt.clientBundle.css;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

public final class GwtCssTypedHandler extends TypedHandlerDelegate {
  @Override
  public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (c == '{' && file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file) && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
      final int offset = editor.getCaretModel().getOffset();
      final Document document = editor.getDocument();
      PsiDocumentManager.getInstance(project).commitDocument(document);
      final PsiElement element = file.findElementAt(offset);
      if (element != null) {
        final XmlAttributeValue attributeValue = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
        if (attributeValue != null) {
          final int valueStart = attributeValue.getTextRange().getStartOffset();
          if (offset == valueStart + 2 && document.getCharsSequence().charAt(valueStart) == '"') {
            document.insertString(offset, "}");
            return Result.STOP;
          }
        }
      }
    }

    return super.charTyped(c, project, editor, file);
  }
}
