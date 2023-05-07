package com.intellij.play.utils.processors;

import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.HashSet;
import java.util.Set;

public class TopLevelControllersProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    for (PlayImplicitVariable playImplicitVariable : getTopLevelControllers(scope)) {
      if (!ResolveUtil.processElement(processor, playImplicitVariable, state)) return false;
    }
    return true;
  }

  public static Set<PlayImplicitVariable> getTopLevelControllers(final PsiElement scope) {
    Set<PlayImplicitVariable> implicitSet = new HashSet<>();

    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(scope.getProject());
    final PsiPackage psiPackage = psiFacade.findPackage(PlayUtils.CONTROLLERS_PKG);
    if (psiPackage != null) {
      for (PsiClass psiClass : psiPackage.getClasses(scope.getResolveScope())) {
        implicitSet.add(PlayImplicitVariablesFactory.createLightClassImplicitVariable(psiClass, psiClass.getName(), true));
      }
    }
    return implicitSet;
  }
}
