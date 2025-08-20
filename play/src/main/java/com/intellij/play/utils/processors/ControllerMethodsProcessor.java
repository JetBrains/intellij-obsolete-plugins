package com.intellij.play.utils.processors;

import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.beans.PlayLightMethodBuilder;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.plugins.groovy.lang.resolve.ResolveUtilKt.shouldProcessMethods;

public class ControllerMethodsProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    if (!shouldProcessMethods(processor)) {
      return true;
    }
    for (LightMethodBuilder methodBuilder : getControllerMethodsVariables(scope)) {
      if (!ResolveUtil.processElement(processor, methodBuilder, state)) return false;
    }
    return true;
  }

  private static Set<LightMethodBuilder> getControllerMethodsVariables(final PsiElement scope) {
    Set<LightMethodBuilder> methodBuilders = new HashSet<>();
    final PsiClass controller = PlayPathUtils.getCorrespondingController(scope.getContainingFile());
    if (controller != null) {
      for (final PsiMethod psiMethod : controller.getAllMethods()) {
        if (psiMethod.hasModifierProperty(PsiModifier.STATIC) && psiMethod.hasModifierProperty(PsiModifier.PUBLIC)) {
          methodBuilders.add(new PlayLightMethodBuilder(scope, psiMethod));
        }
      }
    }
    return methodBuilders;
  }
}
