package com.intellij.play.utils.processors;

import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import java.util.HashSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Set;

public class RenderArgProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    for (PlayImplicitVariable playImplicitVariable : getRenderArgs(scope)) {
      if (!ResolveUtil.processElement(processor, playImplicitVariable, state)) return false;
    }
    return true;
  }

  public static Set<PlayImplicitVariable> getRenderArgs(PsiElement scope) {
    final Set<PlayImplicitVariable> set = new HashSet<>();

    final PsiFile psiFile = scope.getContainingFile();
    final PsiClass controller = PlayPathUtils.getCorrespondingController(psiFile);
    if (controller != null) {
      final PsiMethod psiMethod = getRenderArgPutMethod(psiFile);
      if (psiMethod != null) {
        set.addAll(PlayUtils.getPutMethodInitVariables(psiMethod, PlayUtils.getPsiClassLocalScope(controller)).values());
      }
    }

    return set;
  }

  @Nullable
  private static PsiMethod getRenderArgPutMethod(PsiFile file) {
    final PsiClass correspondingController = PlayPathUtils.getCorrespondingController(file);
    if (correspondingController != null) {
      for (PsiField psiField : correspondingController.getAllFields()) {
        if ("renderArgs".equals(psiField.getName())) {
          final PsiClass psiClass = ((PsiClassType)psiField.getType()).resolve();
          if (psiClass != null) {
            for (PsiMethod psiMethod : psiClass.getMethods()) {
              if ("put".equals(psiMethod.getName())) {
                return psiMethod;
              }
            }
          }
          return null;
        }
      }
    }
    return null;
  }
}
