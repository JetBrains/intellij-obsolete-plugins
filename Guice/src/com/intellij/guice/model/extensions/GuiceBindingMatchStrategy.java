// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.model.InjectionPointDescriptor;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point for matching "special" binding descriptors against injection points.
 *
 * <p>Standard bindings ({@code bind(Foo.class).to(Bar.class)}) are matched via the
 * type-indexed maps in {@code GuiceLiveIndex}.  Special bindings (OptionalBinder,
 * Multibinder, MapBinder, etc.) require custom matching logic — for example,
 * an {@code OptionalBinder<Foo>} should match an injection point of type
 * {@code Optional<Foo>}.
 *
 * <p>Each implementation handles one category of special bindings and is identified
 * by the {@link BindDescriptor} subclass it operates on.
 *
 * <h3>How the index uses strategies</h3>
 * <ol>
 *   <li><b>Storage</b>: Descriptors whose class matches {@link #getDescriptorClass()}
 *       are stored in a separate list for linear scanning.</li>
 *   <li><b>Binding lookup</b>: When an injection point's type suggests it might be a
 *       special type (e.g., {@code Optional<T>}), the strategy's
 *       {@link #findMatchingBindings} is called to check all tracked descriptors.</li>
 *   <li><b>Provides targets</b>: For {@code @Provides} methods, the strategy's
 *       {@link #findMultibinderTargets} is called to find corresponding binder
 *       declarations for navigation.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * <p>Implementations must be stateless and thread-safe.
 */
public interface GuiceBindingMatchStrategy {

  ExtensionPointName<GuiceBindingMatchStrategy> EP_NAME =
      ExtensionPointName.create("com.intellij.guice.bindingMatchStrategy");

  /**
   * Returns the {@link BindDescriptor} subclass this strategy handles.
   *
   * <p>The index uses this to route descriptors into a separate tracked list
   * and to dispatch matching calls to the correct strategy.
   *
   * @return the descriptor class, e.g., {@code OptionalBindDescriptor.class}
   */
  @NotNull Class<? extends BindDescriptor> getDescriptorClass();

  /**
   * Checks whether an injection point's type is relevant for this strategy's
   * special matching.
   *
   * <p>This is a cheap pre-filter called before {@link #findMatchingBindings}
   * to avoid unnecessary iteration over the strategy's descriptor list.
   *
   * <p>For example, the OptionalBinder strategy returns {@code true} only when
   * the target type is {@code Optional<T>}.
   *
   * @param targetType the injection point's effective type (after Provider unwrapping)
   * @return {@code true} if this strategy should be consulted for matching
   */
  boolean isRelevantType(@NotNull PsiType targetType);

  /**
   * Extracts the inner type from a wrapper type matching this strategy's pattern.
   *
   * <p>For example, the OptionalBinder strategy extracts {@code T} from
   * {@code Optional<T>}.  Returns {@code null} if the type doesn't match
   * this strategy's pattern.
   *
   * <p>Used by the reverse lookup ({@code findMatchingInjectionPoints}) to
   * determine whether a standard binding could match an injection point
   * whose type is a wrapper like {@code Optional<T>} or {@code Set<T>}.
   *
   * @param type the injection point's effective type
   * @return the unwrapped inner type, or {@code null} if not applicable
   */
  @Nullable PsiType unwrapType(@NotNull PsiType type);

  /**
   * Finds matching bindings from this strategy's tracked descriptor list.
   *
   * <p>Called during {@code findMatchingBindings()} when {@link #isRelevantType}
   * returns {@code true}.  The strategy iterates its descriptors and checks
   * each against the injection point.
   *
   * @param descriptors the tracked descriptors of type {@link #getDescriptorClass()}
   * @param targetType  the injection point's effective type
   * @param ip          the injection point descriptor (for annotation matching)
   * @param result      output set — add matching descriptors here
   */
  void findMatchingBindings(@NotNull List<? extends BindDescriptor> descriptors,
                            @NotNull PsiType targetType,
                            @NotNull InjectionPointDescriptor ip,
                            @NotNull Set<BindDescriptor> result);

  /**
   * Finds {@code @Provides} methods that match an injection point via this
   * strategy's "provides-into" annotation pattern.
   *
   * <p>For example, {@code SetMultibindMatchStrategy} checks that a provides
   * method is annotated with {@code @ProvidesIntoSet} and its return type is
   * assignable to the Set element type.
   *
   * <p>The default implementation does nothing — strategies that don't handle
   * provides-into matching (e.g., OptionalBinder) can skip this.
   *
   * @param candidates     provides methods indexed by the unwrapped type FQN
   * @param unwrappedType  the inner type (e.g., {@code T} from {@code Set<T>})
   * @param ipAnnotations  the injection point's binding annotations
   * @param result         output set — add matching provides here
   */
  default void findMatchingProvides(@NotNull Set<GuiceProvides> candidates,
                                    @NotNull PsiType unwrappedType,
                                    @NotNull Set<PsiAnnotation> ipAnnotations,
                                    @NotNull Set<GuiceProvides> result) {
  }

  /**
   * Finds multibinder target elements for a {@code @Provides} method.
   *
   * <p>Called during {@code findMultibinderTargets()} to locate binder
   * declarations that correspond to a {@code @ProvidesIntoSet},
   * {@code @ProvidesIntoMap}, or similar annotated method.
   *
   * <p>The default implementation returns an empty list.
   *
   * @param providesMethod the {@code @Provides} method to find targets for
   * @param descriptors    the tracked descriptors of type {@link #getDescriptorClass()}
   * @return a list of PSI elements (bind expressions) for navigation
   */
  default @NotNull List<PsiElement> findMultibinderTargets(@NotNull PsiMethod providesMethod,
                                                           @NotNull List<? extends BindDescriptor> descriptors) {
    return List.of();
  }

  /**
   * Checks whether a standard binding should be excluded from matching when
   * the injection point's type matches this strategy's special type.
   *
   * <p>For example, when an injection point is {@code Optional<Foo>}, a standard
   * binding for {@code Foo} should NOT be treated as an {@code OptionalBinder}
   * match — only actual {@code OptionalBindDescriptor}s should match.
   *
   * <p>This prevents false-positive matches where a regular {@code bind(Foo.class)}
   * would incorrectly appear as a match for an {@code Optional<Foo>} injection point.
   *
   * @param descriptor the standard binding descriptor being checked
   * @return {@code true} if this standard binding should be excluded from matching
   *         when the injection point's type is this strategy's special type
   */
  default boolean shouldExcludeStandardBinding(@NotNull BindDescriptor descriptor) {
    return getDescriptorClass().isInstance(descriptor);
  }
}
