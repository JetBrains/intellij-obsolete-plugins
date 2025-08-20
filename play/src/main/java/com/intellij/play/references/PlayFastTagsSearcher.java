package com.intellij.play.references;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.play.constants.PlayConstants;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

public class PlayFastTagsSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public PlayFastTagsSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters parameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement search = parameters.getElementToSearch();
    if (search instanceof PsiMethod method) {
      if (method.getName().startsWith("_")){
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null && InheritanceUtil.isInheritor(containingClass, PlayConstants.FAST_TAGS)) {
            parameters.getOptimizer().searchWord(method.getName().substring(1), parameters.getEffectiveSearchScope(), UsageSearchContext.ANY, false, method);
        }
      }
    }
  }
}
