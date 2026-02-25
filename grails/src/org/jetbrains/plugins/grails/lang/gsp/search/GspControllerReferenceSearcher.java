// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

public final class GspControllerReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public GspControllerReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement elementToSearch = queryParameters.getElementToSearch();
    if (!(elementToSearch instanceof GrClassDefinition controller)) return;

    if (!GrailsArtifact.CONTROLLER.isInstance(controller)) return;

    final String text = GrailsArtifact.CONTROLLER.getArtifactName(controller);
    SearchScope searchScope = queryParameters.getEffectiveSearchScope();

    if (searchScope instanceof GlobalSearchScope) {
      searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(((GlobalSearchScope)searchScope), GspFileType.GSP_FILE_TYPE,
                                                                    GroovyFileType.GROOVY_FILE_TYPE);
    }
    queryParameters.getOptimizer()
      .searchWord(text, searchScope, (short)(UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_STRINGS), true, controller);
  }
}
