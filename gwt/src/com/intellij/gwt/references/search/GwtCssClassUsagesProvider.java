package com.intellij.gwt.references.search;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.css.usages.CssClassOrIdReferenceBasedUsagesProvider;
import org.jetbrains.annotations.NotNull;

public final class GwtCssClassUsagesProvider extends CssClassOrIdReferenceBasedUsagesProvider {
  @Override
  protected boolean acceptElement(@NotNull PsiElement candidate) {
    if (candidate instanceof PsiLiteralExpression) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(candidate);
      return module != null && GwtFacet.getInstance(module) != null;
    }
    return false;
  }
}
