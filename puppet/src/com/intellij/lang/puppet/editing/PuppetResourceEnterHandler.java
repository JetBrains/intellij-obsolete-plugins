package com.intellij.lang.puppet.editing;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PsiPuppetResourceDeclaration;
import com.intellij.lang.puppet.psi.PsiPuppetResourceInstanceDeclaration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class PuppetResourceEnterHandler implements EnterHandlerDelegate {
  @Override
  public Result preprocessEnter(@NotNull PsiFile file,
                                @NotNull Editor editor,
                                @NotNull Ref<Integer> caretOffset,
                                @NotNull Ref<Integer> caretAdvance,
                                @NotNull DataContext dataContext,
                                EditorActionHandler originalHandler) {
    if (file.getLanguage() != PuppetLanguage.INSTANCE) {
      return Result.Continue;
    }

    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
    PsiElement element = file.findElementAt(caretOffset.get());
    final PsiPuppetResourceDeclaration resource = PsiTreeUtil.getParentOfType(element, PsiPuppetResourceDeclaration.class);
    if (resource == null) {
      return Result.Continue;
    }

    if (!ensureTextually(editor.getDocument().getCharsSequence(), caretOffset.get())) {
      return Result.Continue;
    }

    do {
      element = PsiTreeUtil.prevVisibleLeaf(element);
    }
    while (element instanceof PsiComment);

    if (element == null) {
      return Result.Continue;
    }

    if (element.getNode().getElementType() == PuppetTokenTypes.COLON
        && PsiTreeUtil.getParentOfType(element, PsiPuppetResourceInstanceDeclaration.class) != null) {
      final Document document = editor.getDocument();
      int offsetWithRBrace = caretOffset.get();
      while (document.getCharsSequence().charAt(offsetWithRBrace) != '}') {
        offsetWithRBrace++;
      }

      document.insertString(offsetWithRBrace, "\n");
      final Project project = editor.getProject();
      if (project != null) {
        PsiDocumentManager.getInstance(project).commitDocument(document);
        CodeStyleManager.getInstance(project).adjustLineIndent(file, 1 + offsetWithRBrace);
      }
      return Result.DefaultForceIndent;
    }


    return EnterHandlerDelegate.super.preprocessEnter(file, editor, caretOffset, caretAdvance, dataContext, originalHandler);
  }

  private static boolean ensureTextually(CharSequence text, int position) {
    final int colonPos = StringUtil.lastIndexOf(text, ':', 0, position);
    if (colonPos == -1) {
      return false;
    }

    final int nextPos = StringUtil.indexOfAny(text, "\n}", colonPos, text.length());

    return nextPos != -1 && text.charAt(nextPos) == '}';
  }
}
