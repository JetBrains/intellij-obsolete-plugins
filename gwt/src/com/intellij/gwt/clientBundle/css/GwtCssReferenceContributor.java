package com.intellij.gwt.clientBundle.css;

import com.intellij.gwt.clientBundle.css.language.GwtCssLanguage;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.css.impl.CssTokenImpl;
import com.intellij.psi.css.impl.util.CssReferenceProvider;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public final class GwtCssReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    final FilterPattern referencePlacePattern = new FilterPattern(new CssReferenceProvider.CssReferenceFilter());
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(CssTokenImpl.class)
        .inFile(PlatformPatterns.psiFile(StylesheetFile.class).withLanguage(GwtCssLanguage.GWT_CSS_LANGUAGE))
        .and(referencePlacePattern), new PsiReferenceProvider() {
      @Override
      public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (element instanceof CssTokenImpl) {
          return new PsiReference[]{new GwtCssVariableReference((CssTokenImpl)element)};
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });
  }
}
