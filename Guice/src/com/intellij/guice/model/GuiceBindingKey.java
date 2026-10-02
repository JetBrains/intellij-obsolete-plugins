// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A Guice binding key: the combination of a type and an optional qualifier
 * that uniquely identifies a binding in the Guice dependency graph.
 *
 * <p>Two entries participate in the same binding when their keys {@link #matches match}.
 * This is the <b>single matching predicate</b> used for all navigation — forward and reverse
 * use the same check, which guarantees symmetry by construction.
 *
 * <p>The key holds no PSI. It keeps the canonical text of the type, as Guice compares keys by the exact type.
 * A primitive type becomes its wrapper type, as in Guice.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code @Inject Foo foo}                → key {@code (Foo, null)}</li>
 *   <li>{@code @Inject @Named("x") Foo foo}    → key {@code (Foo, Named("x"))}</li>
 *   <li>{@code bind(Foo.class).to(FooImpl.class)} → key {@code (Foo, null)}</li>
 *   <li>{@code @Inject Provider<Foo> p}        → key {@code (Foo, null)} (unwrapped at creation)</li>
 *   <li>{@code @Inject Map<K,V> m}             → key {@code (Map<K,V>, null)}</li>
 * </ul>
 */
@ApiStatus.Experimental
public final class GuiceBindingKey {
  private final @NotNull String myTypeText;
  private final @Nullable String myTypeFqn;
  private final @Nullable GuiceQualifier myQualifier;
  /**
   * Whether the key stands for every parameterization of a generic class.
   * The just-in-time binding of {@code class Foo<T>} serves {@code Foo<String>}, {@code Foo<Integer>} and so on.
   */
  private final boolean myAnyParameterization;

  public GuiceBindingKey(@NotNull PsiType type, @Nullable GuiceQualifier qualifier) {
    this(type, qualifier, false);
  }

  private GuiceBindingKey(@NotNull PsiType type, @Nullable GuiceQualifier qualifier, boolean anyParameterization) {
    if (type instanceof PsiPrimitiveType primitiveType && primitiveType.getBoxedTypeName() != null) {
      myTypeText = primitiveType.getBoxedTypeName();
      myTypeFqn = myTypeText;
    }
    else {
      myTypeText = type.getCanonicalText();
      PsiClass psiClass = type instanceof PsiClassType classType ? classType.resolve() : null;
      myTypeFqn = psiClass != null ? psiClass.getQualifiedName() : null;
    }
    myQualifier = qualifier;
    myAnyParameterization = anyParameterization;
  }

  /**
   * Returns the unqualified key of a class, for example the key of a just-in-time binding.
   * For a generic class, the key matches every parameterization of the class.
   */
  public static @NotNull GuiceBindingKey forClass(@NotNull PsiClass psiClass) {
    PsiType type = JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass);
    return new GuiceBindingKey(type, null, psiClass.hasTypeParameters());
  }

  public @Nullable GuiceQualifier getQualifier() {
    return myQualifier;
  }

  /**
   * Returns the FQN of the raw type, suitable for fast index lookups.
   * Primitive types are boxed (e.g., {@code boolean} → {@code java.lang.Boolean}).
   * Returns {@code null} for types without a resolvable class.
   */
  public @Nullable String getTypeFqn() {
    return myTypeFqn;
  }

  /**
   * Checks whether this key matches another key.
   *
   * <p>The types must be equal, as in Guice. A subtype or a supertype does not match.
   * A key from {@link #forClass} of a generic class matches every parameterization of the class.
   * Qualifier matching uses {@link GuiceQualifier#matches}.
   *
   * <p>This predicate is <b>symmetric</b>: {@code a.matches(b) == b.matches(a)}.
   */
  public boolean matches(@NotNull GuiceBindingKey other) {
    if (!myTypeText.equals(other.myTypeText)) {
      boolean sameClass = myTypeFqn != null && myTypeFqn.equals(other.myTypeFqn);
      if (!sameClass || !(myAnyParameterization || other.myAnyParameterization)) {
        return false;
      }
    }
    return GuiceQualifier.matches(myQualifier, other.myQualifier);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GuiceBindingKey that)) return false;
    return myAnyParameterization == that.myAnyParameterization
           && myTypeText.equals(that.myTypeText)
           && Objects.equals(myQualifier, that.myQualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myTypeText, myQualifier);
  }

  @Override
  public String toString() {
    return myQualifier != null ? myQualifier + " " + myTypeText : myTypeText;
  }
}
