// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contributor for {@code @CheckedProvides(ProviderType.class)} methods from the
 * Guice {@code throwingproviders} extension.
 */
final class ThrowingProviderContributor implements GuiceBindingContributor {
  @Override
  public void register(@NotNull GuiceExtensionRegistrar registrar) {
    registrar.registerProvidesAnnotation(
        List.of(GuiceAnnotations.CHECKED_PROVIDES),
        ThrowingProviderContributor::wrapCheckedProvidesTypes
    );
  }

  private static @NotNull List<PsiType> wrapCheckedProvidesTypes(@NotNull PsiMethod providesMethod) {
    PsiAnnotation annotation = AnnotationUtil.findAnnotation(
        providesMethod, GuiceAnnotations.CHECKED_PROVIDES);
    if (annotation == null) return List.of();

    PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
    if (!(value instanceof PsiClassObjectAccessExpression classExpr)) return List.of();

    PsiType providerTypeRef = classExpr.getOperand().getType();
    if (!(providerTypeRef instanceof PsiClassType providerClassType)) return List.of();

    PsiClass providerClass = providerClassType.resolve();
    if (providerClass == null) return List.of();

    if (!InheritanceUtil.isInheritor(providerClass, GuiceClasses.CHECKED_PROVIDER)
        && !GuiceClasses.CHECKED_PROVIDER.equals(providerClass.getQualifiedName())) {
      return List.of();
    }

    PsiType returnType = providesMethod.getReturnType();
    if (returnType == null) return List.of();

    PsiElementFactory factory = JavaPsiFacade.getElementFactory(providesMethod.getProject());
    PsiType wrapped = providerClass.hasTypeParameters()
                      ? factory.createType(providerClass, returnType)
                      : factory.createType(providerClass);
    return List.of(wrapped);
  }
}
