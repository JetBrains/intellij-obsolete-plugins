// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Matches {@link SetMultibindDescriptor}s against injection points of type
 * {@code Set<T>} and finds multibinder targets for {@code @ProvidesIntoSet} methods.
 */
public final class SetMultibindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return SetMultibindDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_SET, GuiceAnnotations.CHECKED_PROVIDES_INTO_SET);
  }

  /**
   * Extracts the element type {@code T} from Guice's Set multibinder injection point types:
   * <ul>
   *   <li>{@code Set<T>} / {@code Set<? extends T>} (Kotlin variance)</li>
   *   <li>{@code Collection<Provider<T>>} (all Provider variants: Guice, javax, jakarta)</li>
   * </ul>
   */
  @Override
  public @Nullable PsiType unwrapType(@NotNull PsiType type) {
    // Set<T> or Set<? extends T> (wildcard stripped by getTypeParameter).
    PsiType element = GuiceUtils.getMultibinderElementType(type);
    if (element != null) return element;

    // Collection<Provider<T>> — Guice also binds this for Multibinder<T>.
    PsiType collectionParam = GuiceUtils.getTypeParameter(type, "java.util.Collection", 0);
    if (collectionParam != null) {
      PsiType provided = GuiceUtils.getProviderType(collectionParam);
      if (provided != null) return provided;
    }

    return null;
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof SetMultibindDescriptor smb)) return null;
    PsiElement bindExpr = descriptor.getBindExpression();
    if (bindExpr == null) return null;
    PsiClass elementClass = smb.getElementType();
    return createSetType(bindExpr, elementClass);
  }

  @Override
  public @Nullable PsiType wrapProvidesType(@NotNull PsiMethod providesMethod) {
    if (!isProvidesIntoMethod(providesMethod)) return null;
    PsiType returnType = providesMethod.getReturnType();
    if (!(returnType instanceof PsiClassType ct)) return null;
    return createSetType(providesMethod, ct.resolve());
  }

  private static @Nullable PsiType createSetType(@NotNull PsiElement context,
                                                 @Nullable PsiClass elementClass) {
    if (elementClass == null) return null;
    PsiClass setClass = JavaPsiFacade.getInstance(context.getProject())
        .findClass("java.util.Set", context.getResolveScope());
    if (setClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.getProject());
    return factory.createType(setClass, factory.createType(elementClass));
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
      if (!(descriptor instanceof SetMultibindDescriptor smb)) continue;
      if (GuiceUtils.areClassesEquivalent(smb.getElementType(), returnClass)) {
        PsiElement bindExpr = smb.getBindExpression();
        if (bindExpr != null) targets.add(bindExpr);
      }
    }
    return targets;
  }
}
