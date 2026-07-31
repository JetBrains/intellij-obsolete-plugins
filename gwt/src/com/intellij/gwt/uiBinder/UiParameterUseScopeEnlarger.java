package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_XML_FILES_SCOPE;

public final class UiParameterUseScopeEnlarger extends UseScopeEnlarger {

  @Override
  public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    if (!(element instanceof PsiParameter parameter)) return null;

    final PsiMethod method = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
    if (method == null) return null;

    final PsiClass psiClass = method.getContainingClass();
    if (psiClass == null) return null;

    final PsiModifierList modifierList = method.getModifierList();
    if (method.isConstructor() && modifierList.hasAnnotation(UiBinderUtil.UI_CONSTRUCTOR_ANNOTATION)
        || modifierList.hasAnnotation(UiBinderUtil.UI_FACTORY_ANNOTATION)) {
      return psiClass.getUseScope().intersectWith(UI_XML_FILES_SCOPE);
    }

    return null;
  }
}
