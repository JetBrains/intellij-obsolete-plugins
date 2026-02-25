// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

public final class ArtifactFieldReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public ArtifactFieldReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    PsiElement elementToSearch = queryParameters.getElementToSearch();

    if (!(elementToSearch instanceof GrField field)) return;

    if (!field.isProperty()) return;

    PsiMethod getter;

    PsiMethod[] getters = field.getGetters();
    if (getters.length != 1) return;

    getter = getters[0];

    ArtifactPropertyGetterReferenceSearcher
      .searchReferences(getter, elementToSearch, queryParameters.getEffectiveSearchScope(), queryParameters.getOptimizer());
  }
}
