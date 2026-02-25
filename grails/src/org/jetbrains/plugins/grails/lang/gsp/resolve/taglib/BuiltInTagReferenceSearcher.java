// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.resolve.taglib;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;

public final class BuiltInTagReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public BuiltInTagReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement elementToSearch = queryParameters.getElementToSearch();
    if (!(elementToSearch instanceof PsiClass)) return;

    String name = ((PsiClass)elementToSearch).getQualifiedName();
    if (name == null) return;

    String tagName = GspTagLibUtil.getTagNameByClass(name);
    if (tagName == null) return;

    SearchScope searchScope = queryParameters.getEffectiveSearchScope();
    if (searchScope instanceof GlobalSearchScope) {
      searchScope = GlobalSearchScope
        .getScopeRestrictedByFileTypes(((GlobalSearchScope)searchScope), GspFileType.GSP_FILE_TYPE);
    }

    queryParameters.getOptimizer().searchWord(tagName, searchScope, UsageSearchContext.IN_FOREIGN_LANGUAGES, true, elementToSearch);
  }
}
