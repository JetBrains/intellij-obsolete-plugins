package com.intellij.gwt.rpc;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.Processor;

public final class GwtOverridingServiceMethodsSearcher extends GwtSearcherBase<PsiMethod, OverridingMethodsSearch.SearchParameters> {
  @Override
  protected PsiFile getContainingFile(final OverridingMethodsSearch.SearchParameters parameters) {
    return parameters.getMethod().getContainingFile();
  }

  @Override
  public boolean doExecute(final OverridingMethodsSearch.SearchParameters queryParameters, final Processor<? super PsiMethod> consumer) {
    PsiMethod method = queryParameters.getMethod();
    PsiClass async = method.getContainingClass();
    PsiClass sync = RemoteServiceUtil.findSynchronousInterface(async);
    if (sync != null) {
      PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
      if (syncMethod != null) {
        if (!consumer.process(syncMethod)) {
          return false;
        }
        if (!OverridingMethodsSearch.search(syncMethod, queryParameters.getScope(), queryParameters.isCheckDeep()).forEach(consumer)) {
          return false;
        }
      }
    }
    return true;
  }
}
