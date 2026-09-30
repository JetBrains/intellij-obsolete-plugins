// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;


/**
 * Matches {@link OptionalBindDescriptor}s against injection points of type
 * {@code Optional<T>} (both {@code java.util.Optional} and
 * {@code com.google.common.base.Optional}).
 */
final class OptionalBindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return OptionalBindDescriptor.class;
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof OptionalBindDescriptor obd)) return null;
    PsiType optType = obd.getOptionalBoundType();
    if (optType == null) return null;
    PsiElement context = descriptor.getBindExpression();
    if (context == null) return null;
    // Try java.util.Optional first, fall back to Guava.
    PsiType type = createOptionalType(context, "java.util.Optional", optType);
    return type != null ? type : createOptionalType(context, "com.google.common.base.Optional", optType);
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_OPTIONAL);
  }

  @Override
  public @NotNull List<PsiType> wrapTypes(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof OptionalBindDescriptor obd)) return List.of();
    PsiElement bindExpr = descriptor.getBindExpression();
    PsiType element = obd.getOptionalBoundType();
    if (bindExpr == null || element == null) return List.of();
    return GuiceKeyForms.optionalForms(bindExpr, element, setsValue(descriptor));
  }

  @Override
  public @NotNull List<PsiType> wrapProvidesTypes(@NotNull PsiMethod providesMethod) {
    if (!isProvidesIntoMethod(providesMethod)) return List.of();
    PsiType returnType = providesMethod.getReturnType();
    if (returnType == null) return List.of();
    // A @ProvidesIntoOptional method sets the default value or the binding, so it binds T too.
    return GuiceKeyForms.optionalForms(providesMethod, returnType, true);
  }

  /** Tells if the chain or local variable sets a value, for example {@code newOptionalBinder(...).setDefault().to(Impl.class)}. */
  private static boolean setsValue(@NotNull BindDescriptor descriptor) {
    return ContributorUtil.hasBinderCall(descriptor, "setDefault", "setBinding");
  }

  private static @Nullable PsiType createOptionalType(@NotNull PsiElement context,
                                                      @NotNull String optionalFqn,
                                                      @NotNull PsiType elementType) {
    if (elementType instanceof PsiPrimitiveType pt) {
      elementType = pt.getBoxedType(context);
      if (elementType == null) return null;
    }
    PsiClass optionalClass = JavaPsiFacade.getInstance(context.getProject())
        .findClass(optionalFqn, context.getResolveScope());
    if (optionalClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.getProject());
    return factory.createType(optionalClass, elementType);
  }
}
