// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Objects;

public abstract class BindDescriptor {
   private final SmartPsiElementPointer<PsiElement> myCallExpressionPointer;
   private final NullableLazyValue<PsiClass> myBoundClass = new NullableLazyValue<>() {
     @Override
     protected @Nullable PsiClass compute() {
       final UCallExpression uCall = GuiceUtils.getCallExpression(getBindExpression());
       return uCall != null ? GuiceUtils.findImplementedClassForBinding(uCall) : null;
     }
   };

  private final NullableLazyValue<PsiClass> myBindingClass = new NullableLazyValue<>() {
    @Override
    protected @Nullable PsiClass compute() {
      return calculateBindingClass();
    }
  };

  public BindDescriptor(@NotNull PsiElement callExpression) {
    myCallExpressionPointer = SmartPointerManager.createPointer(callExpression);
  }

  public @Nullable PsiClass getBoundClass() {
    return myBoundClass.getValue();
  }

  public @Nullable PsiClass getBindingClass() {
    return myBindingClass.getValue();
  }

  public abstract @Nullable PsiClass calculateBindingClass();

  public boolean matchesType(@NotNull PsiType type) {
    if (type instanceof PsiClassType) {
      return GuiceUtils.areClassesEquivalent(getBoundClass(), ((PsiClassType)type).resolve());
    }
    return false;
  }

  /**
   * Returns the bind call expression, or {@code null} if the element has been deleted.
   */
  public @Nullable PsiElement getBindExpression() {
    return myCallExpressionPointer.getElement();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BindDescriptor that)) return false;
    return Objects.equals(myCallExpressionPointer, that.myCallExpressionPointer);
  }

  @Override
  public int hashCode() {
    return myCallExpressionPointer.hashCode();
  }
}
