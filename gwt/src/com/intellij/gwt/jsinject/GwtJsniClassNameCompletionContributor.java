package com.intellij.gwt.jsinject;

import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.JavaClassReferenceCompletionContributor;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public final class GwtJsniClassNameCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, final @NotNull CompletionResultSet result) {
    PsiElement position = parameters.getPosition();
    if (parameters.getCompletionType() != CompletionType.BASIC ||
        !position.getContainingFile().getLanguage().equals(GwtLanguageDialect.GWT_DIALECT) ||
        !(position.getParent() instanceof JSGwtReferenceExpressionImpl)) {
      return;
    }

    boolean hasClassRef = JavaClassReferenceCompletionContributor.findJavaClassReference(position.getContainingFile(), parameters.getOffset()) != null;
    boolean runOtherContributors = !hasClassRef || PsiJavaPatterns.psiElement().afterLeaf(".").accepts(position);
    final boolean empty = !runOtherContributors || result.runRemainingContributors(parameters, true).isEmpty();

    if (!empty && parameters.getInvocationCount() == 0) {
      result.restartCompletionWhenNothingMatches();
    }

    GwtFacet facet = GwtFacet.findFacetByPsiElement(parameters.getOriginalFile());
    final boolean shortClassReferencesSupported = facet == null || facet.getSdkVersion().isShortClassReferencesInJavaScriptSupported();
    if (empty || parameters.isExtendedCompletion()) {
      AllClassesGetter.processJavaClasses(parameters, result.getPrefixMatcher(), parameters.getInvocationCount() <= 1,
                                          psiClass -> result.addElement(AllClassesGetter.createLookupItem(psiClass, shortClassReferencesSupported ? AllClassesGetter.TRY_SHORTENING : AllClassesGetter.INSERT_FQN)));
    }
  }
}
