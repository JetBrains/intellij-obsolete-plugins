package com.intellij.gwt.rpc;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Processor;

public final class GwtDeepestAsyncMethodSearcher extends GwtSearcherBase<PsiMethod, PsiMethod> {
  @Override
  protected PsiFile getContainingFile(PsiMethod parameters) {
    return parameters.getContainingFile();
  }

  @Override
  protected boolean doExecute(PsiMethod method, Processor<? super PsiMethod> consumer) {
    PsiClass async = RemoteServiceUtil.findAsynchronousInterface(method.getContainingClass());
    if (async != null) {
      PsiMethod asyncMethod = RemoteServiceUtil.findMethodInAsync(method, async);
      if (asyncMethod != null) {
        if (!consumer.process(asyncMethod)) {
          return false;
        }
      }
    }
    return false;
  }
}
