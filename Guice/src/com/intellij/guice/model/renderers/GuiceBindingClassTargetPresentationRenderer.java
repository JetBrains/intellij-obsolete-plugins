// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.renderers;

import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.guice.GuiceIcons;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import javax.swing.*;
import java.util.List;

import com.intellij.platform.backend.presentation.TargetPresentation;

public final class GuiceBindingClassTargetPresentationRenderer extends PsiTargetPresentationRenderer<PsiElement> {
  @Override
  public @NotNull String getElementText(@NotNull PsiElement element) {
    UElement uElement = UastContextKt.toUElement(element);
    uElement = GuiceUtils.getSelectorIfQualified(uElement);
    if (uElement instanceof UCallExpression callExpression) {
      StringBuilder sb = new StringBuilder();
      if (addExpressionText(callExpression, sb, "bind", true)) {
        addToBinding(callExpression, sb, "annotatedWith");

        if (addToBinding(callExpression, sb, "to")) return sb.toString();
        if (addToBinding(callExpression, sb, "toInstance")) return sb.toString();
        if (addToBinding(callExpression, sb, "toProvider")) return sb.toString();
        if (addToBinding(callExpression, sb, "toConstructor")) return sb.toString();
      }
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  private static boolean addToBinding(@NotNull UCallExpression element, @NotNull StringBuilder sb, @NotNull String methodName) {
    return addExpressionText(element, sb, methodName, false);
  }

  private static boolean addExpressionText(@NotNull UCallExpression element,
                                           @NotNull StringBuilder sb,
                                           @NotNull String methodName,
                                           boolean firstElementInChain) {
    final UCallExpression to = GuiceUtils.findCallInChain(element, methodName);
    if (to != null) {
      final List<UExpression> valueArgs = to.getValueArguments();
      final List<PsiType> typeArgs = to.getTypeArguments();

      if (!firstElementInChain) sb.append(".");
      sb.append(methodName);

      if (!typeArgs.isEmpty()) {
        sb.append("<");
        sb.append(typeArgs.getFirst().getPresentableText());
        sb.append(">");
      }

      sb.append("(");
      if (!valueArgs.isEmpty()) {
        sb.append(getExpressionText(valueArgs.getFirst()));
      }
      sb.append(")");
      return true;
    }
    return false;
  }

  private static String getExpressionText(@NotNull UExpression expression) {
    if (expression instanceof UClassLiteralExpression classLiteral) {
      final PsiElement sourcePsi = classLiteral.getSourcePsi();
      if (sourcePsi != null) {
        return sourcePsi.getText();
      }
    }
    final PsiElement sourcePsi = expression.getSourcePsi();
    if (sourcePsi != null) {
      return sourcePsi.getText();
    }
    return expression.toString();
  }

  @Override
  public @Nullable String getContainerText(@NotNull PsiElement element) {
    final PsiFile file = element.getContainingFile();
    return file != null ? file.getName() : null;
  }

  @Override
  public Icon getIcon(@NotNull PsiElement element) {
    return GuiceIcons.GoogleSmall;
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
}
