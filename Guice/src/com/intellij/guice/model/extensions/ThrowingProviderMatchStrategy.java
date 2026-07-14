// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Handles {@code @CheckedProvides(ProviderType.class)} methods from Guice's
 * {@code throwingproviders} extension.
 *
 * <p>{@code @CheckedProvides(BackendProvider.class) Foo get()} binds
 * {@code BackendProvider<Foo>} — <em>not</em> {@code Foo} directly.  Each
 * concrete {@link com.google.inject.throwingproviders.CheckedProvider} subtype
 * is a distinct binding ({@code BackendProviderA<T> ≠ BackendProviderB<T>}).
 *
 * <p>On the injection side, {@code @Inject BackendProvider<Foo>} keeps its
 * declared type (no unwrapping by {@code getProviderType}), matching the
 * binding directly.
 */
public final class ThrowingProviderMatchStrategy implements GuiceBindingMatchStrategy {

  /** Sentinel class that no real descriptor will ever be an instance of. */
  private static abstract class NoDescriptor extends BindDescriptor {
    private NoDescriptor(@NotNull PsiElement callExpression) {
      super(callExpression);
    }
  }

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    // This strategy is purely annotation-driven — no programmatic binder call
    // produces descriptors for it. Return a sentinel so the instanceof routing
    // in extractBindingCallEntries never matches.
    return NoDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.CHECKED_PROVIDES);
  }

  /**
   * Reads the provider class from {@code @CheckedProvides(value)} and wraps
   * the method's return type: {@code BackendProvider.class + Foo → BackendProvider<Foo>}.
   */
  @Override
  public @Nullable PsiType wrapProvidesType(@NotNull PsiMethod providesMethod) {
    if (!isProvidesIntoMethod(providesMethod)) return null;

    PsiAnnotation annotation = AnnotationUtil.findAnnotation(
        providesMethod, GuiceAnnotations.CHECKED_PROVIDES);
    if (annotation == null) return null;

    PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
    if (!(value instanceof PsiClassObjectAccessExpression classExpr)) return null;

    PsiType providerTypeRef = classExpr.getOperand().getType();
    if (!(providerTypeRef instanceof PsiClassType providerClassType)) return null;

    PsiClass providerClass = providerClassType.resolve();
    if (providerClass == null) return null;

    // Verify it's actually a CheckedProvider subtype.
    if (!InheritanceUtil.isInheritor(providerClass, GuiceClasses.CHECKED_PROVIDER)
        && !GuiceClasses.CHECKED_PROVIDER.equals(providerClass.getQualifiedName())) {
      return null;
    }

    PsiType returnType = providesMethod.getReturnType();
    if (returnType == null) return null;

    PsiElementFactory factory = JavaPsiFacade.getElementFactory(providesMethod.getProject());
    return factory.createType(providerClass, returnType);
  }

  /**
   * No injection-point unwrapping — the concrete provider type matters.
   * {@code BackendProvider<Foo>} stays as-is; it must not be confused with
   * {@code CheckedProvider<Foo>} or plain {@code Foo}.
   */
  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    return null;
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    return null;  // No binder descriptors for throwing providers.
  }
}
