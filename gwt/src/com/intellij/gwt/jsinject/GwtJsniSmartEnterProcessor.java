package com.intellij.gwt.jsinject;

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.jsinject.JsInjector.JSNI_COMMENT_PREFIX;
import static com.intellij.gwt.jsinject.JsInjector.JSNI_COMMENT_SUFFIX;
import static com.intellij.gwt.jsinject.JsInjector.isJsniMethod;

public final class GwtJsniSmartEnterProcessor extends SmartEnterProcessor {
  @Override
  public boolean process(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
    if (!(psiFile instanceof PsiJavaFile) || !ProjectFacetManager.getInstance(project).hasFacets(GwtFacetType.ID)) return false;

    final VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null || GwtModulesManager.getInstance(project).findGwtModulesByClientSourceFile(virtualFile).isEmpty()) return false;

    final PsiElement element = getStatementAtCaret(editor, psiFile);
    final PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
    if (method == null || !method.hasModifierProperty(PsiModifier.NATIVE)) return false;

    if (isJsniMethod(method)) return false;


    final Document document = editor.getDocument();
    int endOffset = method.getTextRange().getEndOffset();
    if (StringUtil.endsWithChar(method.getText(), ';')) {
      document.deleteString(endOffset - 1, endOffset);
      endOffset--;
    }
    else {
      final PsiElement lastChild = method.getLastChild();
      if (lastChild instanceof PsiComment) {
        String commentText = lastChild.getText();
        for (int i = JSNI_COMMENT_PREFIX.length(); i >= 1; i--) {
          if (commentText.substring(0, i).equals(JSNI_COMMENT_PREFIX.substring(0, i))) {
            endOffset = lastChild.getTextRange().getStartOffset();
            document.deleteString(endOffset, endOffset+i);
            break;
          }
        }
      }
    }
    if (document.getCharsSequence().charAt(endOffset-1) == ' ') {
      document.deleteString(endOffset - 1, endOffset);
      endOffset--;
    }

    final String beforeCaret = " " + JSNI_COMMENT_PREFIX;
    final String afterCaret = "\n" + JSNI_COMMENT_SUFFIX + ";";
    document.insertString(endOffset, beforeCaret + afterCaret);
    editor.getCaretModel().moveToOffset(endOffset + beforeCaret.length());
    commit(editor);

    final PsiElement newElement = psiFile.findElementAt(editor.getCaretModel().getOffset());
    final PsiMethod newMethod = PsiTreeUtil.getParentOfType(newElement, PsiMethod.class);
    if (newMethod != null) {
      reformat(newMethod);
    }
    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_START_NEW_LINE)
      .execute(editor, editor.getCaretModel().getCurrentCaret(), EditorUtil.getEditorDataContext(editor));
    return true;
  }
}
