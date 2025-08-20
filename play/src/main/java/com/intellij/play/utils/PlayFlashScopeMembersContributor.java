package com.intellij.play.utils;

import com.intellij.play.constants.PlayConstants;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;

import java.util.HashSet;
import java.util.Set;

public class PlayFlashScopeMembersContributor extends NonCodeMembersContributor {

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;
    final PsiFile containingFile = place.getContainingFile().getOriginalFile();
    final PsiClass controller = PlayPathUtils.getCorrespondingController(containingFile);
    if (controller != null) {
      for (PsiMethod psiMethod : getFlashScopePutMethods(aClass)) {
        for (PlayImplicitVariable implicitVariable : PlayUtils.getPutMethodInitVariables(psiMethod, PlayUtils.getPsiClassLocalScope(controller))
          .values()) {
          if (!processor.execute(implicitVariable, state)) return;
        }
      }
    }
  }

  @NotNull
  private static Set<PsiMethod> getFlashScopePutMethods(PsiClass flashScopeClass) {
    Set<PsiMethod> putMethods = new HashSet<>();
    for (PsiMethod psiMethod : flashScopeClass.getMethods()) {
      if ("put".equals(psiMethod.getName())) {
        putMethods.add(psiMethod);
      }
    }
    return putMethods;
  }


  @Override
  protected String getParentClassName() {
    return PlayConstants.SCOPE_FLASH;
  }
}
