// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.GuiceInjectionUtil;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
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
 *   <li><b>Binding lookup</b>: When an injection point's type can be unwrapped by
 *       {@link #unwrapType}, the strategy's {@link #findMatchingBindings} is called
 *       to check all tracked descriptors.</li>
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

  // ---- Identity ----

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
   * Returns the FQNs of {@code @Provides}-style annotations this strategy handles.
   *
   * <p>For example, the SetMultibinder strategy returns
   * {@code @ProvidesIntoSet} and {@code @CheckedProvidesIntoSet}.
   *
   * <p>These annotations are used for:
   * <ul>
   *   <li>Recognizing methods as provides methods during indexing</li>
   *   <li>Adding gutter icons to annotated methods in the annotator</li>
   *   <li>Determining {@link #isProvidesIntoMethod} (default impl)</li>
   * </ul>
   *
   * <p>The default implementation returns an empty collection (e.g., OptionalBinder
   * has no provides-into annotation).
   *
   * @return annotation FQNs, or empty if this strategy has no provides annotations
   */
  default @NotNull Collection<String> getProvidesAnnotations() {
    return List.of();
  }

  // ---- Annotation caches ----

  /**
   * Cached provides annotations — invalidated when the EP changes.
   */
  final class ProvidesAnnotationsCache {
    private static volatile @Nullable Set<String> cached;
    private static volatile @Nullable Set<String> cachedInto;

    static {
      EP_NAME.addChangeListener(() -> { cached = null; cachedInto = null; }, ApplicationManager.getApplication());
    }

    static @NotNull Set<String> get() {
      Set<String> result = cached;
      if (result == null) {
        Set<String> all = new HashSet<>(GuiceAnnotations.PROVIDES_ANNOTATIONS);
        for (GuiceBindingMatchStrategy strategy : EP_NAME.getExtensionList()) {
          all.addAll(strategy.getProvidesAnnotations());
        }
        result = Set.copyOf(all);
        cached = result;
      }
      return result;
    }

    static @NotNull Set<String> getInto() {
      Set<String> result = cachedInto;
      if (result == null) {
        Set<String> into = new HashSet<>();
        for (GuiceBindingMatchStrategy strategy : EP_NAME.getExtensionList()) {
          into.addAll(strategy.getProvidesAnnotations());
        }
        result = Set.copyOf(into);
        cachedInto = result;
      }
      return result;
    }
  }

  /**
   * Returns all provides-style annotation FQNs: the base {@code @Provides} /
   * {@code @CheckedProvides} plus any contributed by match strategies.
   */
  static @NotNull Set<String> getAllProvidesAnnotations() {
    return ProvidesAnnotationsCache.get();
  }

  /**
   * Returns only "provides-into" annotation FQNs contributed by strategies
   * (e.g., {@code @ProvidesIntoSet}, {@code @ProvidesIntoMap}).
   *
   * <p>Used to exclude {@code @ProvidesInto*} methods from direct type matching,
   * since they should only be matched through their strategy's dispatch.
   */
  static @NotNull Set<String> getProvidesIntoAnnotations() {
    return ProvidesAnnotationsCache.getInto();
  }

  // ---- Type handling ----

  /**
   * Extracts the inner type from a wrapper type matching this strategy's pattern.
   *
   * <p>For example, the OptionalBinder strategy extracts {@code T} from
   * {@code Optional<T>}.  Returns {@code null} if the type doesn't match
   * this strategy's pattern.
   *
   * <p>This method also serves as the relevance pre-filter: a non-null return
   * indicates this strategy should be consulted for matching.
   *
   * @param type the injection point's effective type
   * @return the unwrapped inner type, or {@code null} if not applicable
   */
  @Nullable PsiType unwrapType(@NotNull PsiType type);

  /**
   * Constructs the full collection type for a descriptor handled by this strategy.
   *
   * <p>This is the reverse of {@link #unwrapType}: given a {@link BindDescriptor}
   * known to be an instance of {@link #getDescriptorClass()}, it constructs the
   * parameterized type that injection points will use.
   *
   * <p>For example, the MapBinder strategy constructs {@code Map<K, V>} from
   * a {@code MapMultibindDescriptor}'s key and value types.
   *
   * @param descriptor a descriptor matching {@link #getDescriptorClass()}
   * @return the full parameterized type, or {@code null} if types are unresolvable
   */
  default @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    return null;
  }

  /**
   * Constructs the full collection type for a {@code @ProvidesInto*} method.
   *
   * <p>Given a provides method annotated with one of this strategy's
   * {@link #getProvidesAnnotations()}, constructs the injection point type
   * that this method contributes to.
   *
   * <p>For example, for a {@code @ProvidesIntoSet Foo provideFoo()}, the
   * Set strategy constructs {@code Set<Foo>}.
   *
   * @param providesMethod the provides method
   * @return the collection type the method contributes to, or {@code null}
   */
  default @Nullable PsiType wrapProvidesType(@NotNull PsiMethod providesMethod) {
    return null;
  }

  // ---- Presentation ----

  /**
   * Returns a text provider for entries created from a descriptor handled by this strategy.
   *
   * <p>The returned function receives the binding expression's PSI element and
   * produces the human-readable text shown in navigation popups.
   *
   * <p>The default implementation shows the wrapped type (e.g., {@code Map<String, Foo>},
   * {@code Set<Bar>}). Strategies may override to produce more descriptive text
   * (e.g., including the call name: {@code newMapBinder(String.class, Foo.class)}).
   *
   * @param descriptor a descriptor matching {@link #getDescriptorClass()}
   * @return a text provider function, or {@code null} to use the platform default
   */
  default @Nullable Function<PsiElement, String> getTextProvider(@NotNull BindDescriptor descriptor) {
    PsiType wrappedType = wrapType(descriptor);
    if (wrappedType != null) {
      String text = wrappedType.getPresentableText();
      return _element -> text;
    }
    return null;
  }

  // ---- Matching (with sensible defaults) ----

  /**
   * Tests whether a {@code @ProvidesInto*} method matches an injection point type.
   *
   * <p>This is the <b>single matching predicate</b> used by both forward
   * (provides → IPs) and reverse (IP → provides) lookups, ensuring navigation
   * consistency: if A links to B, B always links back to A.
   *
   * <p>The default implementation checks:
   * <ol>
   *   <li>The IP type can be unwrapped by this strategy</li>
   *   <li>The provides method's return type is assignable to the unwrapped type</li>
   *   <li>The provides method has this strategy's annotation</li>
   *   <li>Binding annotations match</li>
   * </ol>
   *
   * <p>Override to add additional checks (e.g., Map key type matching).
   *
   * @param provides      the {@code @ProvidesInto*} method to test
   * @param ipType        the injection point's effective type (e.g., {@code Map<K, V>})
   * @param ipAnnotations the injection point's binding annotations
   * @return {@code true} if the provides method matches the injection point
   */
  default boolean matchesProvides(@NotNull GuiceProvides provides,
                                  @NotNull PsiType ipType,
                                  @NotNull Set<PsiAnnotation> ipAnnotations) {
    PsiType unwrappedType = unwrapType(ipType);
    if (unwrappedType == null) return false;

    PsiType productType = provides.getProductType();
    PsiMethod method = provides.getPsiElement();
    if (productType == null || method == null) return false;

    return TypeConversionUtil.isAssignable(unwrappedType, productType) &&
           isProvidesIntoMethod(method) &&
           GuiceInjectionUtil.checkBindingAnnotations(ipAnnotations, provides.getBindingAnnotations());
  }

  /**
   * Finds {@code @Provides} methods that match an injection point via this
   * strategy's "provides-into" annotation pattern.
   *
   * <p>The default implementation iterates candidates and delegates to
   * {@link #matchesProvides} for each, ensuring forward/reverse consistency.
   *
   * @param candidates     provides methods indexed by the unwrapped type FQN
   * @param targetType     the injection point's full effective type (e.g., {@code Map<K, V>})
   * @param ipAnnotations  the injection point's binding annotations
   * @param result         output set — add matching provides here
   */
  default void findMatchingProvides(@NotNull Set<GuiceProvides> candidates,
                                    @NotNull PsiType targetType,
                                    @NotNull Set<PsiAnnotation> ipAnnotations,
                                    @NotNull Set<GuiceProvides> result) {
    for (GuiceProvides provides : candidates) {
      try {
        if (matchesProvides(provides, targetType, ipAnnotations)) {
          result.add(provides);
        }
      }
      catch (PsiInvalidElementAccessException e) {
        // Stale PSI — skip.
      }
    }
  }

  /**
   * Checks whether a {@code @Provides} method is annotated with this strategy's
   * "provides-into" annotation (e.g., {@code @ProvidesIntoSet} for the Set strategy).
   *
   * <p>Used during reverse lookup (provides → injection points) to confirm that
   * a match through {@link #unwrapType} is valid for this strategy.
   *
   * <p>The default implementation checks against {@link #getProvidesAnnotations()}.
   * Strategies that return a non-empty collection from that method get this for free.
   *
   * @param providesMethod the provides method to check
   * @return {@code true} if this method has the relevant annotation
   */
  default boolean isProvidesIntoMethod(@NotNull PsiMethod providesMethod) {
    Collection<String> annotations = getProvidesAnnotations();
    return !annotations.isEmpty() && AnnotationUtil.isAnnotated(providesMethod, annotations, 0);
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
   * @param descriptor the standard binding descriptor being checked
   * @return {@code true} if this standard binding should be excluded
   */
  default boolean shouldExcludeStandardBinding(@NotNull BindDescriptor descriptor) {
    return getDescriptorClass().isInstance(descriptor);
  }
}
