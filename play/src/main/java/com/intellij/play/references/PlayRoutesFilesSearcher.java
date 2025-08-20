package com.intellij.play.references;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

public class PlayRoutesFilesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public PlayRoutesFilesSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters parameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement search = parameters.getElementToSearch();
    if (search instanceof PsiNamedElement method) {
      if (DumbService.isDumb(search.getProject())) return;

      if (!PlayUtils.isController(PsiTreeUtil.getContextOfType(method, PsiClass.class, false))) return;

      SearchScope searchScope = PlayPathUtils.getRoutsFilesScope(method);
      if (searchScope != null) {
        parameters.getOptimizer().searchWord(method.getName(), searchScope, UsageSearchContext.IN_PLAIN_TEXT, false, method);
      }
    }
  }
}
