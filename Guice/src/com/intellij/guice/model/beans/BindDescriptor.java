// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class BindDescriptor {
   private final PsiMethodCallExpression myCallExpression;
   private final NullableLazyValue<PsiClass> myBoundClass = new NullableLazyValue<>() {
     @Override
     protected @Nullable PsiClass compute() {
       return GuiceUtils.findImplementedClassForBinding(getBindExpression());
     }
   };

  private final NullableLazyValue<PsiClass> myBindingClass = new NullableLazyValue<>() {
    @Override
    protected @Nullable PsiClass compute() {
      return calculateBindingClass();
    }
  };

  public BindDescriptor(@NotNull PsiMethodCallExpression callExpression) {
    myCallExpression = callExpression;

  }

  public @Nullable PsiClass getBoundClass() {
    return myBoundClass.getValue();
  }

  public @Nullable PsiClass getBindingClass() {
    return myBindingClass.getValue();
  }

  public abstract @Nullable PsiClass calculateBindingClass();

  public @NotNull PsiMethodCallExpression getBindExpression() {
    return myCallExpression;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BindDescriptor that)) return false;
    return Objects.equals(myCallExpression, that.myCallExpression);
  }

  @Override
  public int hashCode() {
    return myCallExpression.hashCode();
  }
}
