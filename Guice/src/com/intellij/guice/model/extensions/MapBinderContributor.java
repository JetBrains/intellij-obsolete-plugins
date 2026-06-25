// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

import java.util.Set;

/**
 * Contributor for Guice {@code MapBinder} bindings:
 * {@code MapBinder.newMapBinder()} and {@code mapBinder()}.
 */
public final class MapBinderContributor implements GuiceBindingContributor {

  private static final Set<String> BINDING_WORDS = Set.of("newMapBinder", "mapBinder");

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
    if (!ContributorUtil.isBinderMethod(resolvedQName, call, "com.google.inject.multibindings.MapBinder")) {
      return false;
    }

    PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
    if (outermostSource != null) {
      PsiClass[] kv = ContributorUtil.extractDualTypeArgs(call);
      descriptors.add(new MapMultibindDescriptor(outermostSource, kv[0], kv[1]));
      return true;
    }
    return false;
  }
}
