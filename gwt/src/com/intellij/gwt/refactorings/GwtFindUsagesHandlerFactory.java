package com.intellij.gwt.refactorings;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.find.findUsages.JavaFindUsagesHandler;
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.ide.util.SuperMethodWarningUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.DeepestSuperMethodsSearch;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class GwtFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
  @Override
  public boolean canFindUsages(@NotNull PsiElement element) {
    if (DumbService.isDumb(element.getProject())) return false;
    if (!(element instanceof PsiMethod method)) {
      return false;
    }

    if (RemoteServiceUtil.findSynchronousMethod(method) != null) {
      return true;
    }

    final Collection<PsiMethod> supers = DeepestSuperMethodsSearch.search(method).findAll();
    for (PsiMethod superMethod : supers) {
      if (RemoteServiceUtil.findSynchronousMethod(superMethod) != null) {
        return true;
      }
    }

    return false;
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
    final JavaFindUsagesHandlerFactory factory = JavaFindUsagesHandlerFactory.getInstance(element.getProject());
    return new JavaFindUsagesHandler(element, factory) {
      @Override
      public PsiElement @NotNull [] getSecondaryElements() {
        PsiElement element = getPsiElement();
        if (element instanceof PsiMethod) {
          PsiMethod[] methods = SuperMethodWarningUtil.checkSuperMethods((PsiMethod)element, getActionString());
          for (PsiMethod method : methods) {
            final PsiMethod syncMethod = RemoteServiceUtil.findSynchronousMethod(method);
            if (syncMethod != null) {
              methods = ArrayUtil.append(methods, syncMethod);
              break;
            }
          }
          if (methods.length > 0) {
            return methods;
          }
        }
        return super.getSecondaryElements();
      }
    };
  }

}
