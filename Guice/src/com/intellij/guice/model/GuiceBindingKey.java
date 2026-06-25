// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A Guice binding key: the combination of a type and an optional qualifier annotation
 * that uniquely identifies a binding in the Guice dependency graph.
 *
 * <p>Two entries participate in the same binding when their keys {@link #matches match}.
 * This is the <b>single matching predicate</b> used for all navigation — forward and reverse
 * use the same check, which guarantees symmetry by construction.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code @Inject Foo foo}                → key {@code (Foo, null)}</li>
 *   <li>{@code @Inject @Named("x") Foo foo}    → key {@code (Foo, @Named("x"))}</li>
 *   <li>{@code bind(Foo.class).to(FooImpl.class)} → key {@code (Foo, null)}</li>
 *   <li>{@code @Inject Provider<Foo> p}        → key {@code (Foo, null)} (unwrapped at creation)</li>
 *   <li>{@code @Inject Map<K,V> m}             → key {@code (Map<K,V>, null)}</li>
 * </ul>
 */
public final class GuiceBindingKey {
  private final @NotNull PsiType myType;
  private final @Nullable PsiAnnotation myQualifier;

  public GuiceBindingKey(@NotNull PsiType type, @Nullable PsiAnnotation qualifier) {
    myType = type;
    myQualifier = qualifier;
  }

  public GuiceBindingKey(@NotNull PsiType type) {
    this(type, null);
  }

  public @NotNull PsiType getType() {
    return myType;
  }

  public @Nullable PsiAnnotation getQualifier() {
    return myQualifier;
  }

  /**
   * Returns the FQN of the raw type, suitable for fast index lookups.
   * Primitive types are boxed (e.g., {@code boolean} → {@code java.lang.Boolean}).
   * Returns {@code null} for types without a resolvable class.
   */
  public @Nullable String getTypeFqn() {
    return GuiceNavigationIndex.getTypeFqn(myType);
  }

  /**
   * Checks whether this key matches another key.
   *
   * <p>Type matching uses {@link TypeConversionUtil#isAssignable} for generic-aware
   * comparison. Qualifier matching uses {@link com.intellij.codeInsight.AnnotationUtil#equal}.
   *
   * <p>This predicate is <b>symmetric</b>: {@code a.matches(b) == b.matches(a)}.
   */
  public boolean matches(@NotNull GuiceBindingKey other) {
    // Type: assignable in either direction (symmetric).
    if (!TypeConversionUtil.isAssignable(myType, other.myType)
        && !TypeConversionUtil.isAssignable(other.myType, myType)) {
      return false;
    }

    // Qualifier: both null, or structurally equal.
    if (myQualifier == null && other.myQualifier == null) return true;
    if (myQualifier == null || other.myQualifier == null) return false;
    return com.intellij.codeInsight.AnnotationUtil.equal(myQualifier, other.myQualifier);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GuiceBindingKey that)) return false;
    return myType.equals(that.myType) && Objects.equals(myQualifier, that.myQualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myType, myQualifier != null ? myQualifier.getQualifiedName() : null);
  }

  @Override
  public String toString() {
    String typeStr = myType.getCanonicalText();
    return myQualifier != null
        ? "@" + myQualifier.getQualifiedName() + " " + typeStr
        : typeStr;
  }
}
