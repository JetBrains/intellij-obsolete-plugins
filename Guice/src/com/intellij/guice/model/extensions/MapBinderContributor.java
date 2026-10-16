// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Contributor for Guice {@code MapBinder} calls and {@code @ProvidesIntoMap} methods.
 */
final class MapBinderContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerDualTypeBinder(
        GuiceCallPattern.named("newMapBinder", "mapBinder")
            .forBinder("com.google.inject.multibindings.MapBinder"),
        MapMultibindDescriptor::new
    );
    registrar.registerProvidesAnnotation(
        List.of(GuiceAnnotations.PROVIDES_INTO_MAP, GuiceAnnotations.CHECKED_PROVIDES_INTO_MAP),
        method -> {
          PsiType valueType = method.getReturnType();
          PsiType keyType = resolveMapKeyType(method);
          return valueType != null && keyType != null
                 ? GuiceKeyForms.mapForms(method, keyType, valueType)
                 : List.of();
        }
    );
  }

  /**
   * Resolves the key type from a {@code @ProvidesIntoMap} method's {@code @MapKey} annotation.
   */
  static @Nullable PsiType resolveMapKeyType(@NotNull PsiMethod method) {
    for (PsiAnnotation annotation : method.getAnnotations()) {
      PsiClass annotationClass = annotation.resolveAnnotationType();
      if (annotationClass == null) continue;

      PsiAnnotation mapKeyAnno = annotationClass.getAnnotation("com.google.inject.multibindings.MapKey");
      if (mapKeyAnno == null) continue;

      for (PsiMethod valueMethod : annotationClass.findMethodsByName("value", false)) {
        PsiType returnType = valueMethod.getReturnType();
        if (returnType instanceof PsiClassType) {
          return returnType;
        }
        if (returnType instanceof PsiPrimitiveType pt) {
          return pt.getBoxedType(method);
        }
      }

      for (PsiMethod getter : annotationClass.findMethodsByName("getValue", false)) {
        PsiType returnType = getter.getReturnType();
        if (returnType instanceof PsiClassType) {
          return returnType;
        }
        if (returnType instanceof PsiPrimitiveType pt) {
          return pt.getBoxedType(method);
        }
      }
    }
    return null;
  }
}
