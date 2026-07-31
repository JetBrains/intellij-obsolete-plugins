package com.intellij.gwt.rpc;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.DirectClassInheritorsSearch;
import com.intellij.util.Processor;

public class GwtAsyncServiceClassInheritorSearcher {
  public static final class Direct extends GwtSearcherBase<PsiClass, DirectClassInheritorsSearch.SearchParameters> {
    @Override
    protected PsiFile getContainingFile(DirectClassInheritorsSearch.SearchParameters parameters) {
      return parameters.getClassToProcess().getContainingFile();
    }

    @Override
    protected boolean doExecute(DirectClassInheritorsSearch.SearchParameters queryParameters, Processor<? super PsiClass> consumer) {
      PsiClass synchronousInterface = RemoteServiceUtil.findSynchronousInterface(queryParameters.getClassToProcess());
      if (synchronousInterface != null) {
        if (!consumer.process(synchronousInterface)) {
          return false;
        }
      }
      return true;
    }
  }

  public static final class Deep extends GwtSearcherBase<PsiClass, ClassInheritorsSearch.SearchParameters> {
      @Override
      protected PsiFile getContainingFile(ClassInheritorsSearch.SearchParameters parameters) {
        return parameters.getClassToProcess().getContainingFile();
      }

      @Override
      protected boolean doExecute(ClassInheritorsSearch.SearchParameters queryParameters, Processor<? super PsiClass> consumer) {
        PsiClass synchronousInterface = RemoteServiceUtil.findSynchronousInterface(queryParameters.getClassToProcess());
        if (synchronousInterface != null) {
          if (!consumer.process(synchronousInterface)) {
            return false;
          }
          return ClassInheritorsSearch.search(synchronousInterface, queryParameters.getScope(), queryParameters.isCheckDeep(),
                                              queryParameters.isCheckInheritance(), queryParameters.isIncludeAnonymous()).forEach(consumer);
        }
        return true;
      }
    }
}
