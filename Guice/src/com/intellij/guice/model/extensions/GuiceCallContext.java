// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.GuiceEntry;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UExpression;

import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Callback context passed to {@link GuiceExtensionRegistrar} call-entry handlers.
 *
 * <p>Allows a call handler to emit {@link GuiceEntry} items directly and to extract bindings
 * from external classes referenced by the call (such as {@code FlagBinder.createModule(SomeFlags.class)})
 * while recording cross-file dependencies in {@link com.intellij.guice.model.GuiceNavigationIndex}.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface GuiceCallContext {
  /**
   * Returns the mutable set of entries collected for the current file.
   */
  @NotNull Set<GuiceEntry> getEntries();

  /**
   * Extracts {@link GuiceEntry} items from {@code targetClass} and records a dependency from
   * {@code targetClass}'s containing file to the file that contains the matched call.
   *
   * <p>When {@code targetClass}'s file is edited, moved, renamed, or deleted, the registering
   * file is automatically re-indexed so its cross-class bindings stay up to date.
   */
  void reportClassBindings(
      @NotNull PsiClass targetClass,
      @NotNull BiConsumer<? super PsiClass, ? super Set<GuiceEntry>> classExtractor
  );

  /**
   * Resolves a class-literal argument expression (such as {@code SomeFlags.class}), extracts
   * bindings when resolved, and records a class-name dependency even when the class is unresolved
   * so that creating or moving the target class file triggers re-indexing of the call site.
   *
   * @return the resolved {@link PsiClass}, or {@code null} if unresolved
   */
  @Nullable PsiClass reportClassArgument(
      @Nullable UExpression classLiteralArgument,
      @NotNull BiConsumer<? super PsiClass, ? super Set<GuiceEntry>> classExtractor
  );
}
