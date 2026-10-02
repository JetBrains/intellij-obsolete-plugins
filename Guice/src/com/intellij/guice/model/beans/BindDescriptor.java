// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.beans;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@ApiStatus.Experimental
public abstract class BindDescriptor {
  private final SmartPsiElementPointer<PsiElement> myCallExpressionPointer;

  public BindDescriptor(@NotNull PsiElement callExpression) {
    myCallExpressionPointer = SmartPointerManager.createPointer(callExpression);
  }

  public @Nullable PsiClass getBoundClass() {
    PsiType type = getBoundType();
    return type instanceof PsiClassType ct ? ct.resolve() : null;
  }

  /**
   * Returns the full parameterized bound type, preserving generic type parameters.
   */
  public @Nullable PsiType getBoundType() {
    final UCallExpression uCall = getOutermostCall();
    return uCall != null ? GuiceUtils.findImplementedTypeForBinding(uCall) : null;
  }

  public @Nullable PsiClass getBindingClass() {
    return calculateBindingClass();
  }

  public abstract @Nullable PsiClass calculateBindingClass();

  /**
   * Whether this descriptor represents a special collection/optional binder (such as {@code Multibinder},
   * {@code MapBinder}, or {@code OptionalBinder}) that produces wrapped bound types via {@link #getWrappedBoundTypes()}.
   */
  public boolean isSpecialBinder() {
    return false;
  }

  /**
   * Returns the primary wrapped type bound by this special binder descriptor, or {@code null} if not applicable.
   */
  public @Nullable PsiType getPrimaryWrappedType() {
    return null;
  }

  /**
   * Returns all wrapped key types bound by this special binder descriptor.
   */
  public @NotNull List<PsiType> getWrappedBoundTypes() {
    PsiType primary = getPrimaryWrappedType();
    return primary != null ? List.of(primary) : List.of();
  }

  /**
   * Returns the presentation text provider for navigation popups produced by this special binder descriptor.
   */
  public @Nullable Function<PsiElement, String> getTextProvider() {
    PsiType wrappedType = getPrimaryWrappedType();
    if (wrappedType != null) {
      String text = wrappedType.getPresentableText();
      return _element -> text;
    }
    return null;
  }

  /**
   * Returns the UAST call expression for querying binding structure.
   */
  public @Nullable UCallExpression getOutermostCall() {
    return GuiceUtils.getCallExpression(getBindExpression());
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
    // A multibinder descriptor and the descriptor of its .to() tail share the call expression. Keep both.
    if (o == null || getClass() != o.getClass()) return false;
    BindDescriptor that = (BindDescriptor)o;
    return Objects.equals(myCallExpressionPointer, that.myCallExpressionPointer);
  }

  @Override
  public int hashCode() {
    return myCallExpressionPointer.hashCode();
  }
}
