// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.renderers;

import com.intellij.guice.GuiceIcons;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class GuiceBindingClassPsiElementListCellRenderer extends PsiElementListCellRenderer {
  @Override
  public String getElementText(PsiElement element) {
    if (element instanceof PsiMethodCallExpression methodCallExpression) {

      StringBuilder sb = new StringBuilder();
      if (addExpressionText(methodCallExpression, sb, "bind", true)) {
        addToBinding(methodCallExpression, sb, "annotatedWith");

        if (addToBinding(methodCallExpression, sb, "to")) return sb.toString();
        if (addToBinding(methodCallExpression, sb, "toInstance")) return sb.toString();
        if (addToBinding(methodCallExpression, sb, "toProvider")) return sb.toString();
        if (addToBinding(methodCallExpression, sb, "toConstructor")) return sb.toString();
      }
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  private static boolean addToBinding(@NotNull PsiMethodCallExpression element, @NotNull StringBuilder sb, @NotNull String methodName) {
      return addExpressionText(element, sb, methodName, false);
  }

  private static boolean addExpressionText(@NotNull PsiMethodCallExpression element,
                                           @NotNull StringBuilder sb,
                                           @NotNull String methodName,
                                           boolean firstElementInChain) {
    final PsiMethodCallExpression to = GuiceUtils.findCallInChain(element, methodName);
    if (to != null) {
      if (!firstElementInChain) sb.append(".");
      sb.append(methodName);
      sb.append("(");
      sb.append(getExpressionText(to.getArgumentList().getExpressions()[0]));
      sb.append(")") ;
      return true;
    }
    return false;
  }

  private static String getExpressionText(@NotNull PsiExpression expression) {
    if (expression instanceof PsiClassObjectAccessExpression) return expression.getText();
    if (expression instanceof PsiNewExpression) {
      final PsiType expressionType = expression.getType();
      if (expressionType != null) {
        StringBuilder sb = new StringBuilder("new ");

        sb.append(expressionType.getPresentableText());
        sb.append("(");

        final PsiExpressionList list = ((PsiNewExpression)expression).getArgumentList();
        final boolean hasArgs = list != null && list.getExpressions().length > 0;
        if (hasArgs) sb.append("...");
        sb.append(")");

        if (expressionType instanceof PsiClassType) {
          if (((PsiClassType)expressionType).resolve() instanceof PsiAnonymousClass) {
            sb.append("{...}");
          }
        }
        return sb.toString();
      }
      return expression.getText();
    }

    return expression.getText();
  }

  @Override
  protected @Nullable String getContainerText(PsiElement element, String name) {
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
    if (psiClass != null) {
      return SymbolPresentationUtil.getSymbolPresentableText(psiClass);
    }
    return SymbolPresentationUtil.getSymbolContainerText(element);
  }

  @Override
  protected Icon getIcon(PsiElement element) {
    return GuiceIcons.GoogleSmall;
  }
}
