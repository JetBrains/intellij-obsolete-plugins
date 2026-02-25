// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.i18n;

import com.intellij.codeInspection.i18n.I18nQuickFixHandler;
import com.intellij.codeInspection.i18n.I18nizeAction;
import com.intellij.codeInspection.i18n.JavaI18nUtil;
import com.intellij.codeInspection.i18n.JavaI18nizeQuickFixDialog;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.PropertyCreationHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringContent;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrStringImpl;
import org.jetbrains.uast.UExpression;

import java.util.Collection;

@ApiStatus.Internal
public final class GrailsI18nGroovyQuickFixHandler implements I18nQuickFixHandler<UExpression> {

  private static final Logger LOG = Logger.getInstance(GrailsI18nGroovyQuickFixHandler.class);

  public static final GrailsI18nGroovyQuickFixHandler INSTANCE = new GrailsI18nGroovyQuickFixHandler();

  @Override
  public void checkApplicability(PsiFile psiFile, Editor editor) throws IncorrectOperationException {
    if (calculatePropertyValue(editor, psiFile) == null) {
      LOG.error("Method checkApplicability() must not be called if file is not applicable, GrailsI18nizeProvider must check applicable " +
                "before returning GrailsI18nGroovyQuickFixHandler");
      throw new IncorrectOperationException("Failed to extract i18n property");
    }
  }

  private static TextRange rangeInnerString(@NotNull PsiElement element) {
    return ElementManipulators.getValueTextRange(element).shiftRight(element.getTextOffset());
  }

  @VisibleForTesting
  public static @Nullable Trinity<String, String, PsiElement> calculatePropertyValue(Editor editor, PsiFile psiFile) {
    TextRange selectedRange = JavaI18nUtil.getSelectedRange(editor, psiFile);
    if (selectedRange == null) return null;

    PsiElement elementAt = psiFile.getViewProvider().findElementAt(selectedRange.getStartOffset());
    if (elementAt == null || elementAt.getLanguage() != GroovyLanguage.INSTANCE) {
      return null;
    }

    elementAt = elementAt.getParent();
    if (elementAt instanceof GrStringContent) {
      elementAt = elementAt.getParent();
    }

    if (elementAt instanceof GrLiteralImpl) {
      if (!selectedRange.equals(elementAt.getTextRange()) && !selectedRange.equals(rangeInnerString(elementAt))) {
        return null;
      }

      Object value = ((GrLiteralImpl)elementAt).getValue();
      if (!(value instanceof String)) return null;

      return Trinity.create((String)value, "", elementAt);
    }

    if (elementAt.getClass() == GrStringImpl.class) { // Don't use instanceof, because GrRegexImpl is instance of GrStringImpl.
      if (editor.getSelectionModel().hasSelection()) {
        if (!selectedRange.equals(elementAt.getTextRange()) && !selectedRange.equals(rangeInnerString(elementAt))) return null;
      }

      GrStringImpl grString = (GrStringImpl)elementAt;

      StringBuilder propertyValueText = new StringBuilder();
      StringBuilder argsText = new StringBuilder();
      int injectionCount = 0;
      
      for (PsiElement e = grString.getFirstChild(); e != null; e = e.getNextSibling()) {
        if (PsiImplUtil.isLeafElementOfType(e, GroovyTokenTypes.mGSTRING_CONTENT) || e instanceof GrStringContent) {
          propertyValueText.append(e.getText());
        }
        else if (e instanceof GrStringInjection) {
          propertyValueText.append('{').append(injectionCount++).append('}');
  
          if (!argsText.isEmpty()) {
            argsText.append(", ");
          }

          PsiElement expression = ((GrStringInjection)e).getExpression();
          if (expression == null) {
            GrClosableBlock closableBlock = ((GrStringInjection)e).getClosableBlock();
            if (closableBlock == null || closableBlock.hasParametersSection()) return null;

            PsiElement rBrace = closableBlock.getRBrace();
            if (rBrace == null) return null;
            
            expression = rBrace.getPrevSibling();
            if (!(expression instanceof GrExpression) || !(expression.getPrevSibling() instanceof GrParameterList)) return null;
          }

          argsText.append(expression.getText().trim());
        }
      }
      
      return Trinity.create(propertyValueText.toString(), argsText.toString(), elementAt);
    }

    return null;
  }

  //public static boolean isApplicable(PsiFile psiFile, Editor editor) {
  //
  //}

  @Override
  public void performI18nization(PsiFile psiFile,
                                 Editor editor,
                                 UExpression literalExpression,
                                 Collection<PropertiesFile> propertiesFiles,
                                 String key,
                                 String value,
                                 String i18nizedText,
                                 UExpression[] parameters,
                                 PropertyCreationHandler propertyCreationHandler) throws IncorrectOperationException {

    Project project = psiFile.getProject();

    Trinity<String, String, PsiElement> trinity = calculatePropertyValue(editor, psiFile);
    assert trinity != null;
    TextRange textRange = trinity.third.getTextRange();

    propertyCreationHandler.createProperty(project, propertiesFiles, key, value, parameters);
    editor.getDocument().replaceString(textRange.getStartOffset(), textRange.getEndOffset(), i18nizedText);
  }

  @Override
  public UExpression getEnclosingLiteral(PsiFile file, Editor editor) {
    return I18nizeAction.getEnclosingStringLiteral(file, editor);
  }

  @Override
  public JavaI18nizeQuickFixDialog<UExpression> createDialog(Project project, Editor editor, PsiFile psiFile) {
    final Trinity<String, String, PsiElement> trinity = calculatePropertyValue(editor, psiFile);
    if (trinity == null) return null;

    return new GrailsI18nizeQuickFixDialog(project, psiFile, trinity.first) {
      @Override
      protected String getArgs() {
        return trinity.second;
      }

      @Override
      protected @NotNull String getTemplateName() {
        return !trinity.second.isEmpty() ? "I18nized Groovy string with injections.gsp" : "I18nized Groovy string.gsp";
      }
    };
  }
}
