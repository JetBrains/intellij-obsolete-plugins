package com.intellij.gwt.rpc;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.daemon.impl.analysis.JavaHighlightUtil.isSerializable;
import static com.intellij.gwt.rpc.GwtSerializableUtil.IS_SERIALIZABLE_INTERFACE_NAME;

public final class GwtSerializableImplicitUsageProvider implements ImplicitUsageProvider {

  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    if (element instanceof PsiMethod method) {
      if (method.isConstructor()) {
        PsiClass psiClass = method.getContainingClass();
        if (psiClass != null) {
          if (method.getParameterList().getParametersCount() == 0 && isSerializable(psiClass, IS_SERIALIZABLE_INTERFACE_NAME)) {
            if (method.hasModifierProperty(PsiModifier.PUBLIC)) {
              return true;
            }

            GwtVersion gwtVersion = GwtFacet.getGwtVersion(ModuleUtilCore.findModuleForPsiElement(method));
            return gwtVersion.isPrivateNoArgConstructorInSerializableClassAllowed();
          }
        }
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
