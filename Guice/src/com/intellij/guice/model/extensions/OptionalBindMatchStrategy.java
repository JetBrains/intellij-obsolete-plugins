// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches {@link OptionalBindDescriptor}s against injection points of type
 * {@code Optional<T>} (both {@code java.util.Optional} and
 * {@code com.google.common.base.Optional}).
 */
public final class OptionalBindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return OptionalBindDescriptor.class;
  }

  /**
   * Extracts the element type {@code T} from Guice's OptionalBinder injection point types:
   * <ul>
   *   <li>{@code Optional<T>} (java.util and Guava)</li>
   *   <li>{@code Optional<Provider<T>>} (all Provider variants: Guice, javax, jakarta)</li>
   * </ul>
   */
  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    PsiType inner = GuiceUtils.getOptionalType(type);
    if (inner == null) return null;

    // Unwrap Provider<T> → T if present (Guice also binds Optional<Provider<T>>).
    PsiType provided = GuiceUtils.getProviderType(inner);
    return provided != null ? provided : inner;
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof OptionalBindDescriptor obd)) return null;
    PsiClass optClass = obd.getOptionalBoundClass();
    if (optClass == null) return null;
    PsiElement context = descriptor.getBindExpression();
    if (context == null) return null;
    // Try java.util.Optional first, fall back to Guava.
    PsiType type = createOptionalType(context, "java.util.Optional", optClass);
    return type != null ? type : createOptionalType(context, "com.google.common.base.Optional", optClass);
  }

  private static @Nullable PsiType createOptionalType(@NotNull PsiElement context,
                                                      @NotNull String optionalFqn,
                                                      @NotNull PsiClass elementClass) {
    PsiClass optionalClass = JavaPsiFacade.getInstance(context.getProject())
        .findClass(optionalFqn, context.getResolveScope());
    if (optionalClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.getProject());
    return factory.createType(optionalClass, factory.createType(elementClass));
  }

  @Override
  public @NotNull List<PsiElement> findMultibinderTargets(@NotNull PsiMethod providesMethod,
                                                          @NotNull List<? extends BindDescriptor> descriptors) {
    List<PsiElement> targets = new ArrayList<>();
    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType returnClassType)) return targets;

    PsiClass returnClass = returnClassType.resolve();
    if (returnClass == null) return targets;

    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof OptionalBindDescriptor opt)) continue;
      if (GuiceUtils.areClassesEquivalent(opt.getOptionalBoundClass(), returnClass)) {
        PsiElement bindExpr = opt.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
