// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

import java.util.Set;

/**
 * Extension point for contributing Guice binding and injection rules to the model.
 *
 * <p>Implementations register declarative call, method, field, and class patterns via
 * {@link #register(GuiceExtensionRegistrar)}. The plugin compiles all registered rules
 * into a {@link GuiceExtensionIndex} that filters candidates together in multiple stages.
 *
 * <h3>Thread safety</h3>
 * <p>Implementations must be stateless and thread-safe.
 */
@ApiStatus.Experimental
@ApiStatus.OverrideOnly
public interface GuiceBindingContributor {

  ExtensionPointName<GuiceBindingContributor> EP_NAME =
      ExtensionPointName.create("com.intellij.guice.bindingContributor");

  /**
   * Registers declarative call and annotation patterns for this contributor.
   */
  default void register(@NotNull GuiceExtensionRegistrar registrar) {
    Set<String> words = getBindingWords();
    if (!words.isEmpty()) {
      registrar.registerLegacyContributor(words, this);
    }
  }

  /**
   * Legacy hook for returning method-name tokens handled by {@link #processCall}.
   * New contributors should override {@link #register(GuiceExtensionRegistrar)} instead.
   */
  default @NotNull Set<String> getBindingWords() {
    return Set.of();
  }

  /**
   * Legacy hook for creating binding descriptors from a resolved UAST call expression.
   * New contributors should override {@link #register(GuiceExtensionRegistrar)} instead.
   */
  default boolean processCall(@NotNull UCallExpression call,
                              @NotNull String methodName,
                              @NotNull String resolvedQName,
                              @NotNull PsiClass containingClass,
                              @NotNull Set<BindDescriptor> descriptors) {
    return false;
  }

  /**
   * Legacy fallback for unresolved calls.
   * New contributors should override {@link #register(GuiceExtensionRegistrar)} instead.
   */
  default boolean processUnresolvedCall(@NotNull UCallExpression call,
                                        @NotNull String methodName,
                                        @NotNull Set<BindDescriptor> descriptors) {
    return false;
  }
}
