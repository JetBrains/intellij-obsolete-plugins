package com.intellij.gwt.refactorings;

import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

final class TypeArgsTagInfo extends GwtJavadocTagInfo {
  public TypeArgsTagInfo() {
    super("gwt.typeArgs");
  }

  @Override
  protected boolean isValidFor(final @NotNull PsiMethod psiMethod) {
    return RemoteServiceUtil.isRemoteServiceInterface(psiMethod.getContainingClass());
  }
}
