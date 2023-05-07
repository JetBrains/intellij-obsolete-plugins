package com.intellij.play.utils.beans;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.light.LightMethodBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyLanguage;

public class PlayLightMethodBuilder extends LightMethodBuilder {
  private final PsiMethod myPsiMethod;

  public PlayLightMethodBuilder(PsiElement scope, PsiMethod psiMethod) {
    super(scope.getManager(), GroovyLanguage.INSTANCE, psiMethod.getName(), psiMethod.getParameterList(),
          psiMethod.getModifierList());
    myPsiMethod = psiMethod;
  }

  @NotNull
  @Override
  public PsiElement getNavigationElement() {
    return myPsiMethod;
  }
}
