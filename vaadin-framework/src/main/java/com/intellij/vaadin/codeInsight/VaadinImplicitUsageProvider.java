package com.intellij.vaadin.codeInsight;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;

public class VaadinImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    if (!(element instanceof PsiClass)) return false;

    PsiModifierList modifierList = ((PsiClass)element).getModifierList();
    return modifierList != null && modifierList.hasAnnotation("com.vaadin.shared.ui.Connect");
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return false;
  }
}
