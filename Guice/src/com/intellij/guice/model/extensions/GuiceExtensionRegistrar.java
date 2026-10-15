// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Declarative registration sink for {@link GuiceBindingContributor} extensions.
 *
 * <p>All registered rules are compiled into a {@link GuiceExtensionIndex} that filters
 * candidate rules together in multiple stages.
 */
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface GuiceExtensionRegistrar {
  /**
   * Registers a single-type binder factory call such as {@code OptionalBinder.newOptionalBinder}
   * or {@code Multibinder.newSetBinder}.
   */
  void registerSingleTypeBinder(@NotNull GuiceCallPattern pattern,
                                @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory);

  /**
   * Registers a dual-type binder factory call such as {@code MapBinder.newMapBinder}.
   */
  void registerDualTypeBinder(@NotNull GuiceCallPattern pattern,
                              @NotNull ContributorUtil.DualTypeDescriptorFactory factory);

  /**
   * Registers a call pattern that produces {@link BindDescriptor} instances inside Guice module classes.
   */
  void registerCallDescriptor(@NotNull GuiceCallPattern pattern,
                              @NotNull BiFunction<? super UCallExpression, ? super Set<BindDescriptor>, Boolean> handler);

  /**
   * Registers a call pattern inside Guice module classes that directly produces {@link GuiceEntry} items.
   */
  void registerModuleCallEntry(@NotNull GuiceCallPattern pattern,
                               @NotNull BiConsumer<? super UCallExpression, ? super Set<GuiceEntry>> producer);

  /**
   * Registers a call pattern in any class that produces {@link GuiceEntry} items
   * or reports cross-class bindings via {@link GuiceCallContext}.
   */
  void registerCallWithContext(@NotNull GuiceCallPattern pattern,
                               @NotNull BiConsumer<? super UCallExpression, ? super GuiceCallContext> producer);

  /**
   * Registers {@code @Provides}-style method annotations that wrap the method return type into bound key types.
   */
  void registerProvidesAnnotation(@NotNull Collection<String> annotationFqns,
                                  @NotNull Function<? super PsiMethod, ? extends List<PsiType>> wrappedTypesProvider);

  /**
   * Registers field annotations that allow {@code @BindingAnnotation} qualifiers and mark fields as
   * implicitly read when bound via {@link GuiceCallContext}, without scanning all annotated fields globally.
   */
  void registerBindingFieldAnnotations(@NotNull Collection<String> annotationFqns);
}
