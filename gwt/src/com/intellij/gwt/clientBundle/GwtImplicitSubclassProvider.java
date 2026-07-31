package com.intellij.gwt.clientBundle;

import com.intellij.codeInspection.inheritance.ImplicitSubclassProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtImplicitSubclassProvider extends ImplicitSubclassProvider {

  @Override
  public boolean isApplicableTo(@NotNull PsiClass psiClass)  {
    return ClientBundleUtil.isImplementationProvidedByGwt(psiClass)
           || UiBinderUtil.isImplementationProvidedByGwt(psiClass);
  }

  @Override
  public @Nullable SubclassingInfo getSubclassingInfo(@NotNull PsiClass psiClass) {
    return new SubclassingInfo(GwtBundle.message("implementation.provided"));
  }
}
