// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.renderers;

import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import javax.swing.*;

import com.intellij.platform.backend.presentation.TargetPresentation;

public final class GuiceInjectionPointTargetPresentationRenderer extends PsiTargetPresentationRenderer<PsiElement> {
  private final GuiceBindingClassTargetPresentationRenderer myBindingRenderer = new GuiceBindingClassTargetPresentationRenderer();

  @Override
  public @NotNull String getElementText(@NotNull PsiElement element) {
    final UElement uElement = UastContextKt.toUElement(element);
    if (uElement instanceof UCallExpression) {
      return myBindingRenderer.getElementText(element);
    }
    if (uElement instanceof UParameter parameter) {
      final UMethod method = UastUtils.getParentOfType(parameter, UMethod.class);
      if (method != null) {
        final UClass uClass = UastUtils.getParentOfType(method, UClass.class);
        final String className = uClass != null ? getCleanName(uClass, uClass.getName()) : "";
        final String methodName = getCleanName(method, method.getName());
        final String parameterName = getCleanName(parameter, parameter.getName());
        if (method.isConstructor()) {
          return className + "(" + parameterName + ")";
        }
        return className + "." + methodName + "(" + parameterName + ")";
      }
    }
    if (uElement instanceof UField field) {
      final UClass uClass = UastUtils.getParentOfType(field, UClass.class);
      if (uClass != null) {
        final String className = getCleanName(uClass, uClass.getName());
        final String fieldName = getCleanName(field, field.getName());
        return className + "." + fieldName;
      }
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  @Override
  public @Nullable String getContainerText(@NotNull PsiElement element) {
    final PsiFile file = element.getContainingFile();
    return file != null ? file.getName() : null;
  }

  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element) {
    return element.getIcon(0);
  }

  @Override
  public @NotNull TargetPresentation getPresentation(@NotNull PsiElement element) {
    var builder = TargetPresentation.builder(getElementText(element))
      .icon(getIcon(element));
    
    final String containerText = getContainerText(element);
    if (containerText != null) {
      builder = builder.containerText(containerText);
    }

    final com.intellij.util.TextWithIcon location = com.intellij.ide.util.PsiElementListCellRenderer.getModuleTextWithIcon(element);
    if (location != null) {
      builder = builder.locationText(location.getText(), location.getIcon());
    }

    return builder.presentation();
  }

  private static @NotNull String getCleanName(@NotNull UElement uElement, @NotNull String fallbackName) {
    final PsiElement sourcePsi = uElement.getSourcePsi();
    if (sourcePsi instanceof PsiNamedElement) {
      final String name = ((PsiNamedElement)sourcePsi).getName();
      if (name != null) return name;
    }
    return fallbackName;
  }
}
