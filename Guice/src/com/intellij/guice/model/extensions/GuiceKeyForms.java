// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds all the keys that a Guice multibinder or optional binder binds.
 *
 * <p>A key form is used only if its classes resolve from the binding site.
 * For example, the Guava {@code Optional} forms appear only when Guava is on the classpath.
 */
final class GuiceKeyForms {
  private static final List<String> PROVIDERS = List.of(
    "com.google.inject.Provider",
    "javax.inject.Provider",
    "jakarta.inject.Provider");

  private static final List<String> OPTIONALS = List.of(
    "java.util.Optional",
    "com.google.common.base.Optional");

  private GuiceKeyForms() {
  }

  /** {@code Multibinder<T>} binds {@code Set<T>} and {@code Collection<Provider<T>>}. */
  static @NotNull List<PsiType> setForms(@NotNull PsiElement context, @NotNull PsiType element) {
    List<PsiType> result = new ArrayList<>();
    addIfResolved(result, parameterized(context, "java.util.Set", element));
    for (PsiType provider : providers(context, element)) {
      addIfResolved(result, parameterized(context, "java.util.Collection", provider));
    }
    return result;
  }

  /**
   * {@code MapBinder<K, V>} binds {@code Map<K, V>} and {@code Map<K, Provider<V>>}.
   * After {@code permitDuplicates()}, it also binds {@code Map<K, Set<V>>},
   * {@code Map<K, Set<Provider<V>>>}, and {@code Map<K, Collection<Provider<V>>>}.
   */
  static @NotNull List<PsiType> mapForms(@NotNull PsiElement context,
                                         @NotNull PsiType key,
                                         @NotNull PsiType value,
                                         boolean permitsDuplicates) {
    List<PsiType> result = new ArrayList<>();
    addIfResolved(result, parameterized(context, "java.util.Map", key, value));
    List<PsiType> providerTypes = providers(context, value);
    for (PsiType provider : providerTypes) {
      addIfResolved(result, parameterized(context, "java.util.Map", key, provider));
    }
    if (permitsDuplicates) {
      PsiClassType setOfValue = parameterized(context, "java.util.Set", value);
      if (setOfValue != null) {
        addIfResolved(result, parameterized(context, "java.util.Map", key, setOfValue));
      }
      for (PsiType provider : providerTypes) {
        PsiClassType setOfProvider = parameterized(context, "java.util.Set", provider);
        if (setOfProvider != null) {
          addIfResolved(result, parameterized(context, "java.util.Map", key, setOfProvider));
        }
        PsiClassType collectionOfProvider = parameterized(context, "java.util.Collection", provider);
        if (collectionOfProvider != null) {
          addIfResolved(result, parameterized(context, "java.util.Map", key, collectionOfProvider));
        }
      }
    }
    return result;
  }

  static @NotNull List<PsiType> mapForms(@NotNull PsiElement context, @NotNull PsiType key, @NotNull PsiType value) {
    return mapForms(context, key, value, false);
  }

  /**
   * {@code OptionalBinder<T>} binds {@code Optional<T>} and {@code Optional<Provider<T>>}.
   * It binds {@code T} too when a default value or a binding is set.
   */
  static @NotNull List<PsiType> optionalForms(@NotNull PsiElement context, @NotNull PsiType element, boolean bindsElement) {
    List<PsiType> result = new ArrayList<>();
    for (String optional : OPTIONALS) {
      addIfResolved(result, parameterized(context, optional, element));
      for (PsiType provider : providers(context, element)) {
        addIfResolved(result, parameterized(context, optional, provider));
      }
    }
    if (bindsElement) {
      result.add(element);
    }
    return result;
  }

  private static @NotNull List<PsiType> providers(@NotNull PsiElement context, @NotNull PsiType element) {
    List<PsiType> result = new ArrayList<>();
    for (String provider : PROVIDERS) {
      addIfResolved(result, parameterized(context, provider, element));
    }
    return result;
  }

  private static void addIfResolved(@NotNull List<PsiType> result, @Nullable PsiType type) {
    if (type != null) result.add(type);
  }

  private static @Nullable PsiClassType parameterized(@NotNull PsiElement context,
                                                      @NotNull String classFqn,
                                                      PsiType @NotNull ... arguments) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(context.getProject());
    PsiClass psiClass = facade.findClass(classFqn, context.getResolveScope());
    if (psiClass == null || psiClass.getTypeParameters().length != arguments.length) return null;
    return facade.getElementFactory().createType(psiClass, arguments);
  }
}
