// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.MultimapBindDescriptor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Matches {@link MultimapBindDescriptor}s against injection points of type
 * {@code Multimap<K, V>} and finds multibinder targets for {@code @ProvidesIntoMap} methods.
 */
final class MultimapBindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return MultimapBindDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_MAP, GuiceAnnotations.CHECKED_PROVIDES_INTO_MAP);
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof MultimapBindDescriptor mbd)) return null;
    PsiType keyType = mbd.getKeyPsiType();
    PsiType valueType = mbd.getValuePsiType();
    PsiElement bindExpr = descriptor.getBindExpression();
    if (keyType == null || valueType == null || bindExpr == null) return null;
    PsiClass multimapClass = JavaPsiFacade.getInstance(bindExpr.getProject())
        .findClass("com.google.common.collect.Multimap", bindExpr.getResolveScope());
    if (multimapClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(bindExpr.getProject());
    return factory.createType(multimapClass, keyType, valueType);
  }
}
