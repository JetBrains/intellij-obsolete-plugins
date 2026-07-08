package com.intellij.lang.puppet.ide.usages;

import com.intellij.lang.puppet.psi.PuppetDataTypeLightElement;
import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

public class PuppetReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  public PuppetReferencesSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = queryParameters.getElementToSearch();

    final String name;

    if (element instanceof PuppetVariable) {
      name = ((PuppetVariable)element).getName();
    }
    else if (element instanceof PuppetLazyProxyLightElement) {
      name = ((PuppetLazyProxyLightElement)element).getName();
    }
    else if (element instanceof PuppetDataTypeLightElement) {
      name = ((PuppetDataTypeLightElement)element).getName();
    }
    else {
      return;
    }

    if (StringUtil.isEmpty(name)) {
      return;
    }

    final SearchScope searchScope = queryParameters.getEffectiveSearchScope();
    queryParameters.getOptimizer().searchWord(name, searchScope, true, element);
  }
}
