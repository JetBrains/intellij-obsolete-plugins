// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

import java.util.Set;

/**
 * Contributor for Guice {@code OptionalBinder} bindings:
 * {@code OptionalBinder.newOptionalBinder()} and {@code optionalBinder()}.
 */
public final class OptionalBinderContributor implements GuiceBindingContributor {

  private static final Set<String> BINDING_WORDS = Set.of("newOptionalBinder", "optionalBinder");

  @Override
  public @NotNull Set<String> getBindingWords() {
    return BINDING_WORDS;
  }

  @Override
  public boolean processCall(@NotNull UCallExpression call,
                             @NotNull String methodName,
                             @NotNull String resolvedQName,
                             @NotNull PsiClass containingClass,
                             @NotNull Set<BindDescriptor> descriptors) {
    if (!ContributorUtil.isBinderMethod(resolvedQName, call, "com.google.inject.multibindings.OptionalBinder")) {
      return false;
    }

    PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
    if (outermostSource != null) {
      descriptors.add(new OptionalBindDescriptor(outermostSource, ContributorUtil.extractSingleTypeArg(call)));
      return true;
    }
    return false;
  }
}
