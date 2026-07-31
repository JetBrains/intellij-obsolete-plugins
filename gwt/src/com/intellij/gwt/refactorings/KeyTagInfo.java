package com.intellij.gwt.refactorings;

import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

final class KeyTagInfo extends GwtJavadocTagInfo {
  public KeyTagInfo() {
    super("gwt.key");
  }

  @Override
  protected boolean isValidFor(final @NotNull PsiMethod psiMethod) {
    PsiClass aClass = psiMethod.getContainingClass();
    return aClass != null && GwtI18nManager.getInstance(aClass.getProject()).isLocalizableInterface(aClass);
  }
}
