// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.model.extensions.ContributorUtil;
import com.intellij.guice.model.extensions.GuiceKeyForms;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.SmartTypePointer;
import com.intellij.psi.SmartTypePointerManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@ApiStatus.Experimental
public class OptionalBindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myOptionalBoundClass;
  private final @Nullable SmartTypePointer myOptionalBoundType;

  public OptionalBindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiType optionalBoundType) {
    super(callExpression);
    PsiClass boundClass = optionalBoundType instanceof PsiClassType ct ? ct.resolve() : null;
    myOptionalBoundClass = boundClass != null ? SmartPointerManager.createPointer(boundClass) : null;
    myOptionalBoundType = optionalBoundType != null
                          ? SmartTypePointerManager.getInstance(callExpression.getProject()).createSmartTypePointer(optionalBoundType)
                          : null;
  }

  public @Nullable PsiClass getOptionalBoundClass() {
    return myOptionalBoundClass != null ? myOptionalBoundClass.getElement() : null;
  }

  public @Nullable PsiType getOptionalBoundType() {
    if (myOptionalBoundType != null) {
      return myOptionalBoundType.getType();
    }
    PsiClass cls = getOptionalBoundClass();
    return cls != null ? JavaPsiFacade.getElementFactory(cls.getProject()).createType(cls) : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return null;
  }

  @Override
  public boolean isSpecialBinder() {
    return true;
  }

  @Override
  public @Nullable PsiType getPrimaryWrappedType() {
    PsiType optType = getOptionalBoundType();
    PsiElement context = getBindExpression();
    if (optType == null || context == null) return null;
    PsiType type = GuiceKeyForms.createParameterizedType(context, "java.util.Optional", optType);
    return type != null ? type : GuiceKeyForms.createParameterizedType(context, "com.google.common.base.Optional", optType);
  }

  @Override
  public @NotNull List<PsiType> getWrappedBoundTypes() {
    PsiElement bindExpr = getBindExpression();
    PsiType element = getOptionalBoundType();
    if (bindExpr == null || element == null) return List.of();
    boolean setsValue = ContributorUtil.hasBinderCall(this, "setDefault", "setBinding");
    return GuiceKeyForms.optionalForms(bindExpr, element, setsValue);
  }
}
