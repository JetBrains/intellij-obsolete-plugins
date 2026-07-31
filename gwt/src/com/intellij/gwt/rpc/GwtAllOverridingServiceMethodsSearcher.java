package com.intellij.gwt.rpc;

import com.intellij.openapi.util.Couple;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.AllOverridingMethodsSearch;
import com.intellij.util.Processor;

public final class GwtAllOverridingServiceMethodsSearcher extends GwtSearcherBase<Couple<PsiMethod>, AllOverridingMethodsSearch.SearchParameters> {
  @Override
  protected PsiFile getContainingFile(final AllOverridingMethodsSearch.SearchParameters parameters) {
    return parameters.getPsiClass().getContainingFile();
  }

  @Override
  public boolean doExecute(final AllOverridingMethodsSearch.SearchParameters queryParameters, final Processor<? super Couple<PsiMethod>> consumer) {
    PsiClass async = queryParameters.getPsiClass();
    PsiClass sync = RemoteServiceUtil.findSynchronousInterface(async);
    if (sync != null) {
      for (PsiMethod method : async.getMethods()) {
        PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
        if (syncMethod != null) {
          if (!consumer.process(Couple.of(method, syncMethod))) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
