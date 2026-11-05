// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.GuiceQualifiers;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The shape of a binder factory call, such as {@code Multibinder.newSetBinder(binder, Foo.class, qualifier)}:
 * how to read the bound types and the qualifier from the call, and which calls on the binder start an
 * element chain whose {@code .to(Impl)} tail is an implementation reference and not a binding of the element
 * type. Every consumer of a binder call in the plugin follows the shape, so a binder with its own argument
 * order needs no change in the plugin.
 *
 * @param binderClassFqns    the generic binder classes that the factory returns, such as {@code MapBinder}
 * @param typesExtractor     returns the bound types of a factory call: one type for a single-type binder,
 *                           key and value for a dual-type binder; an element is {@code null} when unknown
 * @param qualifierExtractor returns the expression of the qualifier of a factory call, or {@code null} when
 *                           the call has none
 * @param elementCallNames   the names of the binder methods that start an element chain
 */
@ApiStatus.Experimental
public record BinderShape(@NotNull Collection<String> binderClassFqns,
                          @NotNull Function<? super UCallExpression, PsiType @NotNull []> typesExtractor,
                          @NotNull Function<? super UCallExpression, ? extends @Nullable UExpression> qualifierExtractor,
                          @NotNull Collection<String> elementCallNames) {

  /** The element calls of the Guice binders: {@code addBinding}, {@code setDefault} and {@code setBinding}. */
  public static final Set<String> GUICE_ELEMENT_CALLS = Set.of("addBinding", "setDefault", "setBinding");

  public BinderShape {
    binderClassFqns = List.copyOf(binderClassFqns);
    elementCallNames = Set.copyOf(elementCallNames);
  }

  /**
   * The shape of a Guice-style single-type binder: {@code newXBinder(binder, Type.class[, qualifier])},
   * {@code newXBinder(binder, Key.get(Type.class, qualifier))} or {@code newXBinder<Type>(binder)}.
   */
  public static @NotNull BinderShape singleType(@NotNull Collection<String> binderClassFqns,
                                                @NotNull Function<? super UCallExpression, ? extends PsiType> typeExtractor) {
    return new BinderShape(binderClassFqns,
                           call -> new PsiType[]{typeExtractor.apply(call)},
                           BinderShape::positionalSingleTypeQualifier,
                           GUICE_ELEMENT_CALLS);
  }

  /**
   * The shape of a Guice-style dual-type binder: {@code newMapBinder(binder, K.class, V.class[, qualifier])}.
   */
  public static @NotNull BinderShape dualType(@NotNull Collection<String> binderClassFqns,
                                              @NotNull Function<? super UCallExpression, PsiType @NotNull []> typesExtractor) {
    return new BinderShape(binderClassFqns, typesExtractor, BinderShape::positionalDualTypeQualifier, GUICE_ELEMENT_CALLS);
  }

  /** The last bound type of a call: the element type of a single-type binder, the value type of a dual-type one. */
  public @Nullable PsiType boundType(@NotNull UCallExpression call) {
    PsiType[] types = typesExtractor.apply(call);
    return types.length == 0 ? null : types[types.length - 1];
  }

  private static @Nullable UExpression positionalSingleTypeQualifier(@NotNull UCallExpression call) {
    List<UExpression> args = call.getValueArguments();
    if (args.size() >= 3) return args.get(2);
    if (args.size() == 2) {
      UCallExpression keyGet = GuiceQualifiers.asKeyGet(args.get(1));
      if (keyGet != null && keyGet.getValueArgumentCount() > 1) return keyGet.getValueArguments().get(1);
    }
    return null;
  }

  private static @Nullable UExpression positionalDualTypeQualifier(@NotNull UCallExpression call) {
    List<UExpression> args = call.getValueArguments();
    return args.size() >= 4 ? args.get(3) : null;
  }
}
