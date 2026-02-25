// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.i18n;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.i18n.I18nizeAction;
import com.intellij.codeInspection.i18n.JavaI18nUtil;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspExpressionTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;

final class GspI18nIntention implements IntentionAction {

  @Override
  public @NotNull String getText() {
    return GrailsBundle.message("intention.text.extract.selected.text.to.message.properties");
  }

  @Override
  public @NotNull String getFamilyName() {
    return getText();
  }

  private static boolean isGroovyStringLiteral(Editor editor, PsiFile file) {
    if (file instanceof GspFile) {
      file = ((GspFile)file).getGroovyLanguageRoot();
    }

    if (file instanceof GroovyFileBase) {
      if (file.getViewProvider() instanceof GspFileViewProvider || GrailsI18nizeProvider.isApplicableGroovyFile((GroovyFileBase)file)) {
        if (GrailsI18nGroovyQuickFixHandler.calculatePropertyValue(editor, file) != null) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    if (isGroovyStringLiteral(editor, psiFile)) {
      return true;
    }

    if (!(psiFile instanceof GspFile || psiFile instanceof GspHtmlFileImpl)) return false;

    SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection()) return false;

    final TextRange selectedRange = JavaI18nUtil.getSelectedRange(editor, psiFile);
    if (selectedRange == null) return false;

    PsiElement e = psiFile.getViewProvider().findElementAt(selectedRange.getStartOffset(), HTMLLanguage.INSTANCE);
    do {
      if (e == null) return false;

      if (e instanceof OuterLanguageElement) {
        PsiElement gspElement = psiFile.getViewProvider().findElementAt(e.getTextOffset(), GspLanguage.INSTANCE);
        if (gspElement == null) return false;

        PsiElement exprTag = gspElement.getParent();
        if (!(exprTag instanceof GspExpressionTag)) return false;

        TextRange exprTagTextRange = exprTag.getTextRange();

        if (!selectedRange.contains(exprTagTextRange)) return false;
        if (selectedRange.getEndOffset() == exprTagTextRange.getEndOffset()) break;

        e = psiFile.getViewProvider().findElementAt(exprTagTextRange.getEndOffset(), HTMLLanguage.INSTANCE);
      }
      else {
        if (e.getTextOffset() + e.getTextLength() >= selectedRange.getEndOffset()) break;
        e = PsiTreeUtil.nextLeaf(e);
      }
    } while (true);

    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    if (isGroovyStringLiteral(editor, psiFile)) {
      I18nizeAction.doI18nSelectedString(project, editor, psiFile, GrailsI18nGroovyQuickFixHandler.INSTANCE);
      return;
    }

    if (psiFile instanceof GspFile) {
      psiFile = psiFile.getViewProvider().getPsi(HTMLLanguage.INSTANCE);
    }
    assert psiFile instanceof GspHtmlFileImpl;
    PsiFile gspFile = psiFile.getViewProvider().getPsi(GspLanguage.INSTANCE);
    assert gspFile != null;
    I18nizeAction.doI18nSelectedString(project, editor, gspFile, GrailsI18nQuickFixHandler.INSTANCE);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
