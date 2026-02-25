// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchRequestCollector;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;

public final class ArtifactPropertyGetterReferenceSearcher extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {

  public ArtifactPropertyGetterReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull MethodReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    PsiElement elementToSearch = queryParameters.getMethod();

    if (!(elementToSearch instanceof GrMethod getter)) return;

    if (!PropertyUtilBase.isSimplePropertyGetter(getter)) return;

    searchReferences(getter, getter, queryParameters.getEffectiveSearchScope(), queryParameters.getOptimizer());
  }

  static void searchReferences(@NotNull PsiMethod getter,
                               PsiElement elementToSearch,
                               @NotNull SearchScope scope,
                               @NotNull SearchRequestCollector optimizer) {

    PsiClass psiClass = getter.getContainingClass();

    GrailsArtifact artifact = GrailsArtifact.getType(psiClass);
    if (artifact == null && GormUtils.isStandaloneGormBean(psiClass)) {
      artifact = GrailsArtifact.DOMAIN;
    }

    if (artifact != GrailsArtifact.CONTROLLER && artifact != GrailsArtifact.TAGLIB && artifact != GrailsArtifact.DOMAIN) return;

    String propertyName = GroovyPropertyUtils.getPropertyNameByGetterName(getter.getName(), true);
    assert propertyName != null;

    short searchContext = 0;

    if (artifact == GrailsArtifact.CONTROLLER || artifact == GrailsArtifact.DOMAIN) {
      searchContext |= UsageSearchContext.IN_STRINGS;
    }

    if (elementToSearch instanceof GrField) {
      String fieldName = ((GrField)elementToSearch).getName();
      if (!fieldName.equals(propertyName)) {
        // Rename processor finds field name in GSP file. If field name is not equals to property name references in GSP file will not be found.
        searchContext |= UsageSearchContext.IN_FOREIGN_LANGUAGES;
      }
    }

    if (searchContext == 0) return;

    if (scope instanceof GlobalSearchScope) {
      scope = GlobalSearchScope
        .getScopeRestrictedByFileTypes((GlobalSearchScope)scope, GroovyFileType.GROOVY_FILE_TYPE, GspFileType.GSP_FILE_TYPE);
    }

    optimizer.searchWord(propertyName, scope, searchContext, true, elementToSearch);
  }

}
