package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;

public final class GwtUiImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    return UiBinderUtil.isUiHandlerMethod(element) || UiBinderUtil.isUiFactoryMethod(element);
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    if (!(element instanceof PsiField)) {
      return false;
    }
    return UiBinderUtil.isUiField(((PsiField)element));
  }
}
