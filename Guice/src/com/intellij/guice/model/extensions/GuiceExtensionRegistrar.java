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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
   * or {@code Multibinder.newSetBinder}, with the Guice argument shape {@code (binder, type[, qualifier])}:
   * the element type is the explicit type argument, else the second value argument, else the only argument.
   * The exact owner classes of the pattern are the binder classes.
   *
   * @see #registerSingleTypeBinder(GuiceCallPattern, Collection, Function, BiFunction)
   */
  default void registerSingleTypeBinder(@NotNull GuiceCallPattern pattern,
                                        @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory) {
    registerSingleTypeBinder(pattern, pattern.getExactOwnerClasses(), ContributorUtil::extractSinglePsiType, factory);
  }

  /**
   * Registers a single-type binder factory call.
   *
   * @param binderClassFqns the generic binder classes that the factory returns, such as {@code Multibinder};
   *                        a variable of such a type gives the element type when a chain starts at the variable
   * @param typeExtractor   returns the element type of a factory call, or {@code null} when it has none
   * @param factory         creates the descriptor from the outermost chain expression and the element type
   */
  default void registerSingleTypeBinder(@NotNull GuiceCallPattern pattern,
                                        @NotNull Collection<String> binderClassFqns,
                                        @NotNull Function<? super UCallExpression, ? extends PsiType> typeExtractor,
                                        @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory) {
    registerSingleTypeBinder(pattern, BinderShape.singleType(binderClassFqns, typeExtractor), factory);
  }

  /**
   * Registers a single-type binder factory call with its full {@link BinderShape}: bound type, qualifier and
   * element calls. Every consumer of the binder call follows the shape.
   */
  void registerSingleTypeBinder(@NotNull GuiceCallPattern pattern,
                                @NotNull BinderShape shape,
                                @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory);

  /**
   * Registers a dual-type binder factory call such as {@code MapBinder.newMapBinder}, with the Guice argument
   * shape {@code (binder, keyType, valueType[, qualifier])}. The exact owner classes of the pattern are the binder classes.
   *
   * @see #registerDualTypeBinder(GuiceCallPattern, Collection, Function, ContributorUtil.DualTypeDescriptorFactory)
   */
  default void registerDualTypeBinder(@NotNull GuiceCallPattern pattern,
                                      @NotNull ContributorUtil.DualTypeDescriptorFactory factory) {
    registerDualTypeBinder(pattern, pattern.getExactOwnerClasses(), ContributorUtil::extractDualPsiTypes, factory);
  }

  /**
   * Registers a dual-type binder factory call.
   *
   * @param binderClassFqns the generic binder classes that the factory returns, such as {@code MapBinder}
   * @param typesExtractor  returns the key and value types of a factory call as a two-element array; an element
   *                        is {@code null} when the call has no such type
   */
  default void registerDualTypeBinder(@NotNull GuiceCallPattern pattern,
                                      @NotNull Collection<String> binderClassFqns,
                                      @NotNull Function<? super UCallExpression, PsiType @NotNull []> typesExtractor,
                                      @NotNull ContributorUtil.DualTypeDescriptorFactory factory) {
    registerDualTypeBinder(pattern, BinderShape.dualType(binderClassFqns, typesExtractor), factory);
  }

  /**
   * Registers a dual-type binder factory call with its full {@link BinderShape}: key and value types,
   * qualifier and element calls. Every consumer of the binder call follows the shape.
   */
  void registerDualTypeBinder(@NotNull GuiceCallPattern pattern,
                              @NotNull BinderShape shape,
                              @NotNull ContributorUtil.DualTypeDescriptorFactory factory);

  /**
   * Handles a module call that matched a {@link #registerCallDescriptor} pattern.
   */
  @FunctionalInterface
  interface CallDescriptorHandler {
    /**
     * @param call the matched call
     * @param sink receives the descriptors of this call
     * @return {@code true} if the handler produced the descriptors of the call, so no other handler runs for it
     */
    boolean handle(@NotNull UCallExpression call, @NotNull Consumer<? super BindDescriptor> sink);
  }

  /**
   * Produces the {@link GuiceEntry} items of a module call that matched a {@link #registerModuleCallEntry} pattern.
   */
  @FunctionalInterface
  interface ModuleCallEntryProducer {
    /**
     * @param call the matched call
     * @param sink receives the entries of this call
     */
    void produce(@NotNull UCallExpression call, @NotNull Consumer<? super GuiceEntry> sink);
  }

  /**
   * Registers a call pattern that produces {@link BindDescriptor} instances inside Guice module classes.
   * The sink is add-only, and the index takes its content only when the handler returns normally:
   * a handler that throws contributes nothing.
   */
  void registerCallDescriptor(@NotNull GuiceCallPattern pattern, @NotNull CallDescriptorHandler handler);

  /**
   * Registers a call pattern inside Guice module classes that directly produces {@link GuiceEntry} items.
   * The sink is add-only, and the index takes its content only when the producer returns normally.
   */
  void registerModuleCallEntry(@NotNull GuiceCallPattern pattern, @NotNull ModuleCallEntryProducer producer);

  /**
   * Registers a call pattern in any class that produces {@link GuiceEntry} items
   * or reports cross-class bindings via {@link GuiceCallContext}.
   * The index takes the entries that the producer reports only when it returns normally.
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
