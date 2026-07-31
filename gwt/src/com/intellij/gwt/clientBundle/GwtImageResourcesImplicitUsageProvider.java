package com.intellij.gwt.clientBundle;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.gwt.clientBundle.jam.ClientBundleMethodJamElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

public final class GwtImageResourcesImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    //todo remove when references from 'gwt-image' attributes will be supported (IDEA-61021)
    if (element instanceof PsiMethod method) {
      final PsiType type = method.getReturnType();
      if (type != null && ClientBundleUtil.IMAGE_RESOURCE_INTERFACE.equals(type.getCanonicalText())) {
        return ClientBundleMethodJamElement.getElement(method) != null;
      }
    }
    return false;
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
