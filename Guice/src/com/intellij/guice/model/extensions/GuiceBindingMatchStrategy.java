// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Extension point for "special" bindings such as OptionalBinder, Multibinder and MapBinder.
 *
 * <p>Each implementation handles one category of special bindings. It is identified
 * by the {@link BindDescriptor} subclass that it operates on.
 *
 * <h3>How the navigation index uses strategies</h3>
 * <ol>
 *   <li><b>Binder calls</b>: {@link com.intellij.guice.model.GuiceEntryProducer} gives a descriptor of type
 *       {@link #getDescriptorClass()} the key from {@link #wrapType}. For example,
 *       an {@code OptionalBinder<Foo>} gives the key {@code Optional<Foo>}.</li>
 *   <li><b>Provides methods</b>: a method with one of the {@link #getProvidesAnnotations()}
 *       gets the key from {@link #wrapProvidesType}. For example,
 *       {@code @ProvidesIntoSet Foo provide()} gives the key {@code Set<Foo>}.</li>
 *   <li><b>Presentation</b>: {@link #getTextProvider} gives the text in the navigation popup.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * <p>Implementations must be stateless and thread-safe.
 */
@ApiStatus.Internal
@ApiStatus.OverrideOnly
public interface GuiceBindingMatchStrategy {

  ExtensionPointName<GuiceBindingMatchStrategy> EP_NAME =
      ExtensionPointName.create("com.intellij.guice.bindingMatchStrategy");

  // ---- Identity ----

  /**
   * Returns the {@link BindDescriptor} subclass this strategy handles,
   * or {@code null} when the strategy handles only annotations via {@link #getProvidesAnnotations()}.
   *
   * @return the descriptor class (e.g., {@code OptionalBindDescriptor.class}), or {@code null}
   */
  default @Nullable Class<? extends BindDescriptor> getDescriptorClass() {
    return null;
  }

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
   * Cached provides annotations — invalidated automatically when {@link #EP_NAME} changes.
   */
  record ProvidesAnnotationsSnapshot(@NotNull Set<String> all, @NotNull Set<String> into) {
    static @NotNull ProvidesAnnotationsSnapshot get() {
      return EP_NAME.computeIfAbsent(ProvidesAnnotationsSnapshot.class, () -> {
        Set<String> into = new HashSet<>();
        for (GuiceBindingMatchStrategy strategy : EP_NAME.getExtensionList()) {
          into.addAll(strategy.getProvidesAnnotations());
        }
        Set<String> all = new HashSet<>(GuiceAnnotations.PROVIDES_ANNOTATIONS);
        all.addAll(into);
        return new ProvidesAnnotationsSnapshot(Set.copyOf(all), Set.copyOf(into));
      });
    }
  }

  /**
   * Returns all provides-style annotation FQNs: the base {@code @Provides} /
   * {@code @CheckedProvides} plus any contributed by match strategies.
   */
  static @NotNull Set<String> getAllProvidesAnnotations() {
    return ProvidesAnnotationsSnapshot.get().all();
  }

  /**
   * Returns only "provides-into" annotation FQNs contributed by strategies
   * (e.g., {@code @ProvidesIntoSet}, {@code @ProvidesIntoMap}).
   *
   * <p>Used to exclude {@code @ProvidesInto*} methods from direct type matching,
   * since they should only be matched through their strategy's dispatch.
   */
  static @NotNull Set<String> getProvidesIntoAnnotations() {
    return ProvidesAnnotationsSnapshot.get().into();
  }

  // ---- Type handling ----

  /**
   * Constructs the full collection type for a descriptor handled by this strategy.
   *
   * <p>Given a {@link BindDescriptor}
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

  /**
   * Returns all the keys that a descriptor handled by this strategy binds.
   * For example, a {@code Multibinder<Foo>} binds {@code Set<Foo>} and {@code Collection<Provider<Foo>>}.
   *
   * <p>The default implementation returns the result of {@link #wrapType}.
   *
   * @param descriptor a descriptor matching {@link #getDescriptorClass()}
   * @return the bound types, may be empty
   */
  default @NotNull List<PsiType> wrapTypes(@NotNull BindDescriptor descriptor) {
    PsiType type = wrapType(descriptor);
    return type != null ? List.of(type) : List.of();
  }

  /**
   * Returns all the keys that a {@code @ProvidesInto*} method of this strategy contributes to.
   *
   * <p>The default implementation returns the result of {@link #wrapProvidesType}.
   *
   * @param providesMethod the provides method
   * @return the bound types, may be empty
   */
  default @NotNull List<PsiType> wrapProvidesTypes(@NotNull PsiMethod providesMethod) {
    PsiType type = wrapProvidesType(providesMethod);
    return type != null ? List.of(type) : List.of();
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

  // ---- Helpers ----

  /**
   * Checks whether a {@code @Provides} method is annotated with this strategy's
   * "provides-into" annotation (e.g., {@code @ProvidesIntoSet} for the Set strategy).
   *
   * <p>Used by {@link #wrapProvidesType} implementations to confirm that the method belongs to this strategy.
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
}
