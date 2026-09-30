// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Matches {@link MapMultibindDescriptor}s against injection points of type
 * {@code Map<K, V>} and finds multibinder targets for {@code @ProvidesIntoMap} methods.
 */
final class MapMultibindMatchStrategy implements GuiceBindingMatchStrategy {

  @Override
  public @NotNull Class<? extends BindDescriptor> getDescriptorClass() {
    return MapMultibindDescriptor.class;
  }

  @Override
  public @NotNull Collection<String> getProvidesAnnotations() {
    return List.of(GuiceAnnotations.PROVIDES_INTO_MAP, GuiceAnnotations.CHECKED_PROVIDES_INTO_MAP);
  }

  @Override
  public @Nullable PsiType wrapType(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof MapMultibindDescriptor mmb)) return null;
    PsiElement bindExpr = descriptor.getBindExpression();
    if (bindExpr == null) return null;
    return createMapType(bindExpr, mmb.getKeyPsiType(), mmb.getValuePsiType());
  }

  @Override
  public @NotNull List<PsiType> wrapTypes(@NotNull BindDescriptor descriptor) {
    if (!(descriptor instanceof MapMultibindDescriptor mmb)) return List.of();
    PsiElement bindExpr = descriptor.getBindExpression();
    PsiType keyType = mmb.getKeyPsiType();
    PsiType valueType = mmb.getValuePsiType();
    if (bindExpr == null || keyType == null || valueType == null) return List.of();
    boolean permitsDuplicates = ContributorUtil.hasBinderCall(descriptor, "permitDuplicates");
    return GuiceKeyForms.mapForms(bindExpr, keyType, valueType, permitsDuplicates);
  }

  @Override
  public @NotNull List<PsiType> wrapProvidesTypes(@NotNull PsiMethod providesMethod) {
    if (!isProvidesIntoMethod(providesMethod)) return List.of();
    PsiType valueType = providesMethod.getReturnType();
    PsiType keyType = resolveMapKeyType(providesMethod);
    if (valueType == null || keyType == null) return List.of();
    return GuiceKeyForms.mapForms(providesMethod, keyType, valueType);
  }

  private static @Nullable PsiType createMapType(@NotNull PsiElement context,
                                                 @Nullable PsiType keyType,
                                                 @Nullable PsiType valueType) {
    if (keyType == null || valueType == null) return null;
    if (keyType instanceof PsiPrimitiveType kpt) {
      keyType = kpt.getBoxedType(context);
      if (keyType == null) return null;
    }
    if (valueType instanceof PsiPrimitiveType vpt) {
      valueType = vpt.getBoxedType(context);
      if (valueType == null) return null;
    }
    PsiClass mapClass = JavaPsiFacade.getInstance(context.getProject())
        .findClass("java.util.Map", context.getResolveScope());
    if (mapClass == null) return null;
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(context.getProject());
    return factory.createType(mapClass, keyType, valueType);
  }

  /**
   * Resolves the key type from a {@code @ProvidesIntoMap} method's {@code @MapKey}
   * annotation. Returns {@code null} if no {@code @MapKey} is found.
   *
   * <p>Preserves generic type arguments on the annotation's {@code value()} return type
   * (for example, {@code Class<?>} in {@code @ClassMapKey}).
   */
  static @Nullable PsiType resolveMapKeyType(@NotNull PsiMethod method) {
    for (PsiAnnotation annotation : method.getAnnotations()) {
      PsiClass annotationClass = annotation.resolveAnnotationType();
      if (annotationClass == null) continue;

      PsiAnnotation mapKeyAnno = annotationClass.getAnnotation("com.google.inject.multibindings.MapKey");
      if (mapKeyAnno == null) continue;

      // Look for the value() method — in Kotlin annotation classes this is
      // the light method for the constructor property (e.g., val value: ChannelType).
      for (PsiMethod valueMethod : annotationClass.findMethodsByName("value", false)) {
        PsiType returnType = valueMethod.getReturnType();
        if (returnType instanceof PsiClassType) {
          return returnType;
        }
        if (returnType instanceof PsiPrimitiveType pt) {
          return pt.getBoxedType(method);
        }
      }

      // Kotlin annotation: try property accessors if value() wasn't found as a method.
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
