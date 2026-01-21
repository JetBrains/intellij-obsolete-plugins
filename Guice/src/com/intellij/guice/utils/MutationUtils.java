// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NonNls;

public final class MutationUtils {
  private MutationUtils() {
    super();
  }

  public static void negateExpression(PsiExpression exp) throws IncorrectOperationException {
    final PsiJavaParserFacade facade = JavaPsiFacade.getInstance(exp.getProject()).getParserFacade();

    PsiExpression expressionToReplace = exp;
    final @NonNls String expString;
    final @NonNls String newExpressionText = exp.getText();
    if ("true".equals(newExpressionText)) {
      expressionToReplace = exp;
      expString = "false";
    }
    else if ("false".equals(newExpressionText)) {
      expressionToReplace = exp;
      expString = "true";
    }
    else if (BoolUtils.isNegated(exp)) {
      expressionToReplace = BoolUtils.findNegation(exp);
      expString = newExpressionText;
    }
    else if (BoolUtils.isNegation(exp)) {
      expressionToReplace = exp;
      expString = BoolUtils.getNegated(exp).getText();
    }
    else if (ComparisonUtils.isComparison(exp)) {
      final PsiBinaryExpression binaryExpression =
        (PsiBinaryExpression)exp;
      final String negatedComparison =
        ComparisonUtils.getNegatedComparison(binaryExpression.getOperationTokenType());
      final PsiExpression lhs = binaryExpression.getLOperand();
      final PsiExpression rhs = binaryExpression.getROperand();
      assert rhs != null;
      expString = lhs.getText() + negatedComparison + rhs.getText();
    }
    else {
      if (ParenthesesUtils.getPrecedence(exp) > ParenthesesUtils.PREFIX_PRECEDENCE) {
        expString = "!(" + newExpressionText + ')';
      }
      else {
        expString = '!' + newExpressionText;
      }
    }
    final PsiExpression newCall =
      facade.createExpressionFromText(expString, null);
    final PsiElement insertedElement = expressionToReplace.replace(newCall);
    final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(exp.getProject());
    codeStyleManager.reformat(insertedElement);
  }

  public static PsiExpression replaceExpression(String newExpression,
                                                PsiExpression exp) throws IncorrectOperationException {
    final Project project = exp.getProject();
    final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
    final PsiExpression newCall =
      factory.createExpressionFromText(newExpression, null);
    final PsiElement insertedElement = exp.replace(newCall);
    final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
    final PsiElement shortenedElement = JavaCodeStyleManager.getInstance(project).shortenClassReferences(insertedElement);
    return (PsiExpression)codeStyleManager.reformat(shortenedElement);
  }

  public static void addAnnotation(PsiModifierListOwner owner, String annotation) throws IncorrectOperationException {
    final PsiModifierList modifiers = owner.getModifierList();
    final Project project = owner.getProject();
    final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    final PsiAnnotation newAnnotation = elementFactory.createAnnotationFromText(annotation, owner);
    assert modifiers != null;
    final PsiElement replacedAnnotation = modifiers.add(newAnnotation);
    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
    codeStyleManager.shortenClassReferences(replacedAnnotation);
  }

  public static void replaceAnnotation(PsiAnnotation originalAnnotation, String replacementString) throws IncorrectOperationException {
    final Project project = originalAnnotation.getProject();
    final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    final PsiAnnotation newAnnotation = elementFactory.createAnnotationFromText(replacementString, originalAnnotation);
    final PsiElement replacedAnnotation = originalAnnotation.replace(newAnnotation);
    JavaCodeStyleManager.getInstance(project).shortenClassReferences(replacedAnnotation);
  }
}
