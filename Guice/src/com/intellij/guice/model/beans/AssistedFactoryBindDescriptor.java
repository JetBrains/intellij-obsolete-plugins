// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import com.intellij.guice.utils.GuiceUtils;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.uast.UExpression;

public class AssistedFactoryBindDescriptor extends BindDescriptor {
  private final @Nullable PsiClass myFactoryClass;

  public AssistedFactoryBindDescriptor(@NotNull PsiElement callExpression, @Nullable PsiClass factoryClass) {
    super(callExpression);
    myFactoryClass = factoryClass;
  }

  @Override
  public @Nullable PsiClass getBoundClass() {
    return myFactoryClass;
  }

  @Override
  public @Nullable PsiClass calculateBindingClass() {
    final UCallExpression uCall = GuiceUtils.getCallExpression(getBindExpression());
    if (uCall != null) {
      UCallExpression current = uCall;
      while (current != null) {
        final String name = current.getMethodName();
        if ("implement".equals(name)) {
          final java.util.List<UExpression> args = current.getValueArguments();
          if (args.size() > 1) {
            final PsiType implType = GuiceUtils.getBindingTypeFromExpression(args.get(1));
            if (implType instanceof PsiClassType) {
              return ((PsiClassType)implType).resolve();
            }
          }
        }
        current = GuiceUtils.getReceiverCall(current);
      }
    }
    return myFactoryClass != null ? findFactoryProductType(myFactoryClass) : null;
  }

  private static @Nullable PsiClass findFactoryProductType(@NotNull PsiClass factoryClass) {
    for (PsiMethod method : factoryClass.getMethods()) {
      if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
        final PsiType returnType = method.getReturnType();
        if (returnType instanceof PsiClassType) {
          return ((PsiClassType)returnType).resolve();
        }
      }
    }
    return null;
  }
}
