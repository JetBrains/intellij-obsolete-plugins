package com.intellij.lang.puppet.ide.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.PuppetDefaultWrapper;
import com.intellij.lang.puppet.psi.mixins.PuppetResourceInstanceDeclarationMixin;
import com.intellij.lang.puppet.util.PuppetConfigurationUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PuppetAnnotator implements Annotator, PuppetTokenTypes {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (PuppetConfigurationUtil.getPuppetVersion(element) == PuppetLanguage.Version.PUPPET_3) {
      annotatePuppet3(element, holder);
    }
    else {
      annotatePuppet4(element, holder);
    }
  }

  private static void annotatePuppet3(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (PsiUtilCore.getElementType(element) == SINGLE_QUOTED_STRING) {
      assert element instanceof LeafPsiElement;
      CharSequence text = ((LeafPsiElement)element).getChars();
      if (!StringUtil.endsWithChar(text, '\'')) {
        errorClosingQuoteExpected(holder);
      }
      else if (StringUtil.endsWith(text, "\\'")) {
        TextRange range = element.getTextRange();
        holder.newAnnotation(HighlightSeverity.ERROR, PuppetBundle.message("puppet.v3.closing.quote.escaped")).range(TextRange.from(range.getEndOffset() - 2, 1)).create();
      }
    }
    else if (element instanceof PuppetResourceInstanceDeclarationMixin) {
      List<PsiElement> list = ((PuppetResourceInstanceDeclarationMixin)element).getNameIdentifiersList();
      for (PsiElement psiElement : list) {
        if (psiElement instanceof PuppetDefaultWrapper) {
          holder.newAnnotation(HighlightSeverity.ERROR, PuppetBundle.message("puppet.v3.per.expression.default")).create();
        }
      }
    }
  }

  private static void annotatePuppet4(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (PsiUtilCore.getElementType(element) == SINGLE_QUOTED_STRING) {
      assert element instanceof LeafPsiElement;
      if (isPuppet4SingleQuotedStringUnclosed(((LeafPsiElement)element).getChars())) {
        errorClosingQuoteExpected(holder);
      }
    }
  }

  private static boolean isPuppet4SingleQuotedStringUnclosed(CharSequence text) {
    if (!StringUtil.endsWithChar(text, '\'')) {
      return true;
    }
    int backSlashes = 0;
    for (int currentIndex = text.length() - 2; currentIndex >= 0; currentIndex--) {
      if (text.charAt(currentIndex) == '\\') {
        backSlashes++;
      }
      else {
        break;
      }
    }
    return backSlashes % 2 != 0;
  }

  private static void errorClosingQuoteExpected(@NotNull AnnotationHolder holder) {
    holder.newAnnotation(HighlightSeverity.ERROR, PuppetBundle.message("puppet.unclosed.string")).create();
  }
}

