// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionalBindDescriptor extends BindDescriptor {
  private final @Nullable SmartPsiElementPointer<PsiClass> myOptionalBoundClass;

  public OptionalBindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiClass optionalBoundClass) {
    super(callExpression);
    myOptionalBoundClass = optionalBoundClass != null ? SmartPointerManager.createPointer(optionalBoundClass) : null;
  }

  public @Nullable PsiClass getOptionalBoundClass() {
    return myOptionalBoundClass != null ? myOptionalBoundClass.getElement() : null;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    return null;
  }

  @Override
  public boolean matchesType(@NotNull PsiType type) {
    PsiType paramType = GuiceUtils.getTypeParameter(type, "java.util.Optional", 0);
    if (paramType == null) {
      paramType = GuiceUtils.getTypeParameter(type, "com.google.common.base.Optional", 0);
    }
    final PsiClass paramClass = GuiceUtils.resolveClass(paramType);
    return GuiceUtils.areClassesEquivalent(paramClass, getOptionalBoundClass());
  }
}
