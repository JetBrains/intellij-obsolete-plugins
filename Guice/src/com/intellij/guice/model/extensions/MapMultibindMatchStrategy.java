// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Matches {@link MapMultibindDescriptor}s against injection points of type
 * {@code Map<K, V>} and finds multibinder targets for {@code @ProvidesIntoMap} methods.
 */
public final class MapMultibindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return MapMultibindDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_MAP, GuiceAnnotations.CHECKED_PROVIDES_INTO_MAP);
  }

  /**
   * Extracts the value type {@code V} from Guice's Map binder injection point types:
   * <ul>
   *   <li>{@code Map<K, V>} / {@code Map<K, ? extends V>} (Kotlin variance)</li>
   *   <li>{@code Map<K, Provider<V>>} (all Provider variants: Guice, javax, jakarta)</li>
   *   <li>{@code Map<K, Set<V>>} (permitDuplicates)</li>
   *   <li>{@code Map<K, Set<Provider<V>>>} (permitDuplicates + Provider)</li>
   *   <li>{@code Map<K, Collection<Provider<V>>>} (permitDuplicates + Collection)</li>
   * </ul>
   */
  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    // Map<K, V> or Map<K, ? extends V> (wildcard stripped by getTypeParameter).
    PsiType valueType = GuiceUtils.getMultibinderValueType(type);
    if (valueType == null) return null;

    // Direct: Map<K, V> → V.
    // Also try unwrapping Provider: Map<K, Provider<V>> → V.
    PsiType provided = GuiceUtils.getProviderType(valueType);
    if (provided != null) return provided;

    // permitDuplicates: Map<K, Set<V>> → V, Map<K, Set<Provider<V>>> → V.
    PsiType setElement = GuiceUtils.getTypeParameter(valueType, "java.util.Set", 0);
    if (setElement != null) {
      PsiType setProvided = GuiceUtils.getProviderType(setElement);
      return setProvided != null ? setProvided : setElement;
    }

    // permitDuplicates: Map<K, Collection<Provider<V>>> → V.
    PsiType collParam = GuiceUtils.getTypeParameter(valueType, "java.util.Collection", 0);
    if (collParam != null) {
      PsiType collProvided = GuiceUtils.getProviderType(collParam);
      if (collProvided != null) return collProvided;
    }

    return valueType;
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof MapMultibindDescriptor mmb)) return null;
    PsiElement bindExpr = descriptor.getBindExpression();
    if (bindExpr == null) return null;
    PsiClass keyClass = mmb.getKeyType();
    PsiClass valueClass = mmb.getValueType();
    return createMapType(bindExpr, keyClass, valueClass);
  }

  @Override
  public @Nullable PsiType wrapProvidesType(@NotNull PsiMethod providesMethod) {
    if (!isProvidesIntoMethod(providesMethod)) return null;
    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType ct)) return null;
    PsiClass valueClass = ct.resolve();
    PsiClass keyClass = resolveMapKeyType(providesMethod);
    return createMapType(providesMethod, keyClass, valueClass);
  }

  private static @Nullable PsiType createMapType(@NotNull PsiElement context,
                                                 @Nullable PsiClass keyClass,
                                                 @Nullable PsiClass valueClass) {
    if (keyClass == null || valueClass == null) return null;
    PsiClass mapClass = JavaPsiFacade.getInstance(context.getProject())
        .findClass("java.util.Map", context.getResolveScope());
    if (mapClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.getProject());
    return factory.createType(mapClass, factory.createType(keyClass), factory.createType(valueClass));
  }

  /**
   * Extends the default matching predicate with Map key type verification.
   *
   * <p>In addition to the base checks (value type assignability, annotation,
   * binding annotation matching), this verifies that the {@code @MapKey}
   * annotation on the provides method produces a key type compatible with
   * the injection point's map key parameter.
   */
  @Override
  public boolean matchesProvides(@NotNull GuiceProvides provides,
                                 @NotNull PsiType ipType,
                                 @NotNull Set<PsiAnnotation> ipAnnotations) {
    if (!GuiceBindingMatchStrategy.super.matchesProvides(provides, ipType, ipAnnotations)) {
      return false;
    }

    // Extract the key type K from the injection point's Map<K, V>.
    PsiType keyParam = GuiceUtils.getTypeParameter(ipType, "java.util.Map", 0);
    PsiClass keyClass = keyParam instanceof PsiClassType ct ? ct.resolve() : null;

    PsiMethod method = provides.getPsiElement();
    return method != null && matchesMapKeyType(method, keyClass);
  }

  /**
   * Checks whether a {@code @ProvidesIntoMap} method's {@code @MapKey} annotation
   * produces a key whose type matches the expected key class.
   *
   * <p>If the expected key class is {@code null} (unresolvable), or if no
   * {@code @MapKey} meta-annotation is found on the method, returns {@code true}
   * to avoid false negatives — the value type match is still valid.
   */
  private static boolean matchesMapKeyType(@NotNull PsiMethod method, @Nullable PsiClass expectedKeyClass) {
    if (expectedKeyClass == null) return true;
    PsiClass actualKeyClass = resolveMapKeyType(method);
    return actualKeyClass == null || GuiceUtils.areClassesEquivalent(actualKeyClass, expectedKeyClass);
  }

  /**
   * Resolves the key type from a {@code @ProvidesIntoMap} method's {@code @MapKey}
   * annotation. Returns {@code null} if no {@code @MapKey} is found.
   *
   * <p>Works for both Java and Kotlin annotation classes by using
   * {@link PsiAnnotation#resolveAnnotationType()} instead of Java-specific reference resolution.
   */
  static @Nullable PsiClass resolveMapKeyType(@NotNull PsiMethod method) {
    for (PsiAnnotation annotation : method.getAnnotations()) {
      PsiClass annotationClass = annotation.resolveAnnotationType();
      if (annotationClass == null) continue;

      PsiAnnotation mapKeyAnno = annotationClass.getAnnotation("com.google.inject.multibindings.MapKey");
      if (mapKeyAnno == null) continue;

      // Look for the value() method — in Kotlin annotation classes this is
      // the light method for the constructor property (e.g., val value: ChannelType).
      for (PsiMethod valueMethod : annotationClass.findMethodsByName("value", false)) {
        PsiType returnType = valueMethod.getReturnType();
        if (returnType instanceof PsiClassType ct) {
          PsiClass resolved = ct.resolve();
          if (resolved != null) return resolved;
          // If resolve fails, try the raw type name as fallback.
        }
        if (returnType instanceof PsiPrimitiveType pt) {
          PsiClassType boxed = pt.getBoxedType(method);
          return boxed != null ? boxed.resolve() : null;
        }
      }

      // Kotlin annotation: try property accessors if value() wasn't found as a method.
      for (PsiMethod getter : annotationClass.findMethodsByName("getValue", false)) {
        PsiType returnType = getter.getReturnType();
        if (returnType instanceof PsiClassType ct) {
          return ct.resolve();
        }
      }
    }
    return null;
  }

  @Override
  public @NotNull List<PsiElement> findMultibinderTargets(@NotNull PsiMethod providesMethod,
                                                          @NotNull List<? extends BindDescriptor> descriptors) {
    List<PsiElement> targets = new ArrayList<>();

    if (!isProvidesIntoMethod(providesMethod)) return targets;

    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType returnClassType)) return targets;

    PsiClass returnClass = returnClassType.resolve();
    if (returnClass == null) return targets;


    for (BindDescriptor descriptor : descriptors) {
      if (!(descriptor instanceof MapMultibindDescriptor mmb)) continue;
      if (GuiceUtils.areClassesEquivalent(mmb.getValueType(), returnClass) &&
          matchesMapKeyType(providesMethod, mmb.getKeyType())) {
        PsiElement bindExpr = mmb.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
