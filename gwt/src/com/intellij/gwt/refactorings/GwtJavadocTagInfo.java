package com.intellij.gwt.refactorings;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

abstract class GwtJavadocTagInfo implements JavadocTagInfo {
  private final String myName;

  GwtJavadocTagInfo(@NonNls String name) {
    myName = name;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public boolean isInline() {
    return false;
  }

  @Override
  public boolean isValidInContext(PsiElement element) {
    return element instanceof PsiMethod && isValidFor((PsiMethod)element);
  }

  protected abstract boolean isValidFor(@NotNull PsiMethod psiMethod);

  @Override
  public String checkTagValue(PsiDocTagValue value) {
    return null;
  }

  @Override
  public PsiReference getReference(PsiDocTagValue value) {
    return null;
  }
}