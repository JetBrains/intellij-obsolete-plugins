// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Matches {@link SetMultibindDescriptor}s against injection points of type
 * {@code Set<T>} and finds multibinder targets for {@code @ProvidesIntoSet} methods.
 */
final class SetMultibindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return SetMultibindDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_SET, GuiceAnnotations.CHECKED_PROVIDES_INTO_SET);
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof SetMultibindDescriptor smb)) return null;
    PsiElement bindExpr = descriptor.getBindExpression();
    if (bindExpr == null) return null;
    return createSetType(bindExpr, smb.getElementPsiType());
  }

  @Override
  public @NotNull List<PsiType> wrapTypes(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof SetMultibindDescriptor smb)) return List.of();
    PsiElement bindExpr = descriptor.getBindExpression();
    PsiType elementType = smb.getElementPsiType();
    if (bindExpr == null || elementType == null) return List.of();
    return GuiceKeyForms.setForms(bindExpr, elementType);
  }

  @Override
  public @NotNull List<PsiType> wrapProvidesTypes(@NotNull PsiMethod providesMethod) {
    if (!isProvidesIntoMethod(providesMethod)) return List.of();
    PsiType returnType = providesMethod.getReturnType();
    if (returnType == null) return List.of();
    return GuiceKeyForms.setForms(providesMethod, returnType);
  }

  private static @Nullable PsiType createSetType(@NotNull PsiElement context,
                                                 @Nullable PsiType elementType) {
    if (elementType == null) return null;
    if (elementType instanceof PsiPrimitiveType pt) {
      elementType = pt.getBoxedType(context);
      if (elementType == null) return null;
    }
    PsiClass setClass = JavaPsiFacade.getInstance(context.getProject())
        .findClass("java.util.Set", context.getResolveScope());
    if (setClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.getProject());
    return factory.createType(setClass, elementType);
  }
}
