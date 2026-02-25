// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.i18n;

import com.intellij.codeInspection.i18n.I18nQuickFixHandler;
import com.intellij.codeInspection.i18n.I18nizeAction;
import com.intellij.codeInspection.i18n.JavaI18nUtil;
import com.intellij.codeInspection.i18n.JavaI18nizeQuickFixDialog;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.PropertyCreationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterGroovyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspExpressionTag;
import org.jetbrains.uast.UExpression;

import java.util.Collection;

@ApiStatus.Internal
public final class GrailsI18nQuickFixHandler implements I18nQuickFixHandler<UExpression> {

  public static final GrailsI18nQuickFixHandler INSTANCE = new GrailsI18nQuickFixHandler();

  private GrailsI18nQuickFixHandler() {
  }

  @Override
  public void checkApplicability(PsiFile psiFile, Editor editor) throws IncorrectOperationException {
    final FileViewProvider viewProvider = psiFile.getViewProvider();

    final PsiFile gspHtmlFile = viewProvider.getPsi(HTMLLanguage.INSTANCE);
    assert gspHtmlFile != null;

    final TextRange selectedRange = JavaI18nUtil.getSelectedRange(editor, psiFile);
    assert selectedRange != null;
    // must contain no or balanced tags only
    // must not contain scriptlets or custom tags
    gspHtmlFile.accept(new PsiRecursiveElementVisitor() {

      private static void throwError() {
        throw new IncorrectOperationException("""
                                                Please select GSP text to I18nize.
                                                Make sure you have not selected any scriptlets, custom tags or other foreign languages elements.
                                                Also, HTML tags inside selection must be balanced.""");
      }

      @Override
      public void visitElement(@NotNull PsiElement element) {
        TextRange elementRange = element.getTextRange();
        if (elementRange.intersectsStrict(selectedRange)) {
          if (isInapplicableOuterElement(element, elementRange, selectedRange, viewProvider)) {
            throwError();
          }

          if (element instanceof XmlTag) {
            if (!selectedRange.contains(elementRange)) {
              if (!elementRange.contains(selectedRange)) {
                throwError();
              }

              XmlTag xmlTag = (XmlTag)element;

              if (!xmlTag.getValue().getTextRange().contains(selectedRange) && !isAttributeValueSelected(xmlTag, selectedRange)) {
                throwError();
              }
            }
          }
        }

        super.visitElement(element);
      }
    });
  }

  private static boolean isAttributeValueSelected(XmlTag xmlTag, TextRange selectedRange) {
    for (XmlAttribute attribute : xmlTag.getAttributes()) {
      XmlAttributeValue valueElement = attribute.getValueElement();
      if (valueElement != null && valueElement.getTextRange().contains(selectedRange)) {
        if (valueElement.getValueTextRange().contains(selectedRange)) { // getValueTextRange() is a slow method, check by getTextRange().contains(selectedRange) before.
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isInapplicableOuterElement(PsiElement element,
                                                    TextRange elementRange,
                                                    TextRange selectedRange,
                                                    FileViewProvider viewProvider) {
    if (element instanceof OuterLanguageElement) {
      PsiElement gspElement = viewProvider.findElementAt(element.getTextOffset(), GspLanguage.INSTANCE);
      if (gspElement == null) return true;

      PsiElement exprTag = gspElement.getParent();
      if (!(exprTag instanceof GspExpressionTag)) return true;

      if (!exprTag.getTextRange().equals(elementRange)) return false;

      return selectedRange.contains(elementRange);
    }

    return false;
  }

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
    TextRange selectedText = JavaI18nUtil.getSelectedRange(editor, psiFile);
    if (selectedText == null) return;
    propertyCreationHandler.createProperty(project, propertiesFiles, key, value, parameters);
    editor.getDocument().replaceString(selectedText.getStartOffset(), selectedText.getEndOffset(), i18nizedText);
  }

  @Override
  public UExpression getEnclosingLiteral(PsiFile file, Editor editor) {
    return I18nizeAction.getEnclosingStringLiteral(file, editor);
  }

  public static @Nullable Couple<String> calculatePropertyValue(Editor editor, PsiFile psiFile) {
    TextRange selectedRange = JavaI18nUtil.getSelectedRange(editor, psiFile);
    if (selectedRange == null) return null;

    StringBuilder propertyValueText = new StringBuilder();
    final StringBuilder argsText = new StringBuilder();

    int injectionCount = 0;

    PsiElement e = psiFile.getViewProvider().findElementAt(selectedRange.getStartOffset(), HTMLLanguage.INSTANCE);

    while (e != null && e.getTextOffset() < selectedRange.getEndOffset()) {
      if (e instanceof OuterLanguageElement) {
        PsiElement gspElement = psiFile.getViewProvider().findElementAt(e.getTextOffset(), GspLanguage.INSTANCE);
        if (gspElement == null) return null;

        PsiElement exprTag = gspElement.getParent();
        if (!(exprTag instanceof GspExpressionTag)) return null;

        TextRange exprTagTextRange = exprTag.getTextRange();
        if (!selectedRange.contains(exprTagTextRange)) return null;

        propertyValueText.append('{').append(injectionCount++).append('}');

        if (!argsText.isEmpty()) {
          argsText.append(", ");
        }

        PsiElement firstChild = exprTag.getFirstChild();
        if (!PsiImplUtil.isLeafElementOfType(firstChild, GspTokenTypes.JEXPR_BEGIN) &&
            !PsiImplUtil.isLeafElementOfType(firstChild, GspTokenTypes.GEXPR_BEGIN)) return null;
        assert firstChild != null;

        PsiElement expr = firstChild.getNextSibling();

        if (!(expr instanceof GspOuterGroovyElement)) return null;

        PsiElement closedElement = expr.getNextSibling();

        if (closedElement == null || closedElement != exprTag.getLastChild()) return null;

        argsText.append(expr.getText().trim());

        e = psiFile.getViewProvider().findElementAt(exprTagTextRange.getEndOffset(), HTMLLanguage.INSTANCE);
      }
      else {
        CharSequence chars = ((LeafPsiElement)e).getChars();
        int start = e.getTextOffset();

        propertyValueText.append(chars, Math.max(selectedRange.getStartOffset(), start) - start,
                                 Math.min(start + chars.length(), selectedRange.getEndOffset()) - start);
        e = PsiTreeUtil.nextLeaf(e);
      }
    }

    return Couple.of(propertyValueText.toString(), argsText.toString());
  }

  @Override
  public JavaI18nizeQuickFixDialog<UExpression> createDialog(final Project project, final Editor editor, final PsiFile psiFile) {
    final Couple<String> pair = calculatePropertyValue(editor, psiFile);
    if (pair == null) return null;

    TextRange selectedRange = JavaI18nUtil.getSelectedRange(editor, psiFile);
    assert selectedRange != null;
    PsiElement e = psiFile.getViewProvider().findElementAt(selectedRange.getStartOffset(), HTMLLanguage.INSTANCE);
    XmlAttributeValue attributeValue = PsiTreeUtil.getParentOfType(e, XmlAttributeValue.class);

    if (attributeValue != null) {
      return new GrailsI18nizeQuickFixDialog(project, psiFile, pair.first) {
        @Override
        protected String getArgs() {
          return pair.second;
        }

        @Override
        public @NotNull String getI18nizedText() {
          String nizedText = super.getI18nizedText();
          return "${" + nizedText + "}";
        }

        @Override
        protected @NotNull String getTemplateName() {
          return !pair.second.isEmpty() ? "I18nized Groovy string with injections.gsp" : "I18nized Groovy string.gsp";
        }
      };
    }

    return new GrailsI18nizeQuickFixDialog(project, psiFile, pair.first) {
      @Override
      protected String getArgs() {
        return pair.second;
      }

      @Override
      protected @NotNull String getTemplateName() {
        return !pair.second.isEmpty() ? "I18nized GSP Expression with injections.gsp" : "I18nized GSP Expression.gsp";
      }
    };
  }
}
