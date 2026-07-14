// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Objects;

public abstract class BindDescriptor {
   private final SmartPsiElementPointer<PsiElement> myCallExpressionPointer;

   /**
    * Cached UAST representation of the outermost call expression.
    * Computed once via {@link GuiceUtils#getCallExpression} to avoid repeated
    * {@code toUElement()} bridges, which are expensive.
    */
   private final NullableLazyValue<UCallExpression> myOutermostCall = new NullableLazyValue<>() {
     @Override
     protected @Nullable UCallExpression compute() {
       return GuiceUtils.getCallExpression(getBindExpression());
     }
   };

   /**
    * The full parameterized bound type (e.g., {@code Set<Foo>} not just {@code Set}).
    * Used by {@link #matchesType} for generic-aware matching to avoid false positives
    * between different parameterizations of the same raw type.
    */
   private final NullableLazyValue<PsiType> myBoundType = new NullableLazyValue<>() {
     @Override
     protected @Nullable PsiType compute() {
       final UCallExpression uCall = getOutermostCall();
       return uCall != null ? GuiceUtils.findImplementedTypeForBinding(uCall) : null;
     }
   };

   private final NullableLazyValue<PsiClass> myBoundClass = new NullableLazyValue<>() {
     @Override
     protected @Nullable PsiClass compute() {
       PsiType type = myBoundType.getValue();
       return type instanceof PsiClassType ct ? ct.resolve() : null;
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

  /**
   * Returns the full parameterized bound type, preserving generic type parameters.
   */
  public @Nullable PsiType getBoundType() {
    return myBoundType.getValue();
  }

  public @Nullable PsiClass getBindingClass() {
    return myBindingClass.getValue();
  }

  public abstract @Nullable PsiClass calculateBindingClass();

  /**
   * Checks whether this binding matches the given injection point type.
   *
   * <p>Uses {@link TypeConversionUtil#isAssignable} for full generic-aware
   * type comparison, which prevents false positives like {@code Set<Foo>}
   * matching {@code Set<Bar>}.
   */
  public boolean matchesType(@NotNull PsiType type) {
    PsiType boundType = getBoundType();
    if (boundType != null) {
      return TypeConversionUtil.isAssignable(type, boundType);
    }
    // Fallback: if no bound type available, use raw class comparison.
    if (type instanceof PsiClassType) {
      return GuiceUtils.areClassesEquivalent(getBoundClass(), ((PsiClassType)type).resolve());
    }
    return false;
  }

  /**
   * Returns the cached UAST call expression for querying binding structure.
   *
   * <p>This is the single PSI→UAST bridge point in the descriptor hierarchy.
   * Subclasses should use this instead of calling
   * {@code GuiceUtils.getCallExpression(getBindExpression())} directly.
   */
  public @Nullable UCallExpression getOutermostCall() {
    return myOutermostCall.getValue();
  }

  /**
   * Returns the source PSI element for identity and survival across edits.
   *
   * <p>For querying binding structure (bound type, binding class, etc.),
   * prefer {@link #getOutermostCall()} which stays in UAST.
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
