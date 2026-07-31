package com.intellij.gwt.clientBundle.css;

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

final class GwtCssBackspaceHandler extends BackspaceHandlerDelegate {
  @Override
  public void beforeCharDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
  }

  @Override
  public boolean charDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
    if (c == '{' && file instanceof XmlFile && UiBinderUtil.isUiXmlFile((XmlFile)file)) {
      final int offset = editor.getCaretModel().getOffset();
      final Document document = editor.getDocument();
      PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
      final PsiElement element = file.findElementAt(offset);
      if (element != null) {
        final XmlAttributeValue attributeValue = PsiTreeUtil.getParentOfType(element, XmlAttributeValue.class);
        if (attributeValue != null) {
          final int valueStart = attributeValue.getTextRange().getStartOffset();
          if (offset == valueStart + 1 && document.getCharsSequence().charAt(offset) == '}' && document.getCharsSequence().charAt(valueStart) == '"') {
            document.deleteString(offset, offset+1);
            return true;
          }
        }
      }
    }
    return false;
  }
}
