// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;



public final class GuiceInjectionUtil {

  public static @Nullable PsiExpression findNamedExpression(final PsiExpression annotatedWithExpression) {
    if (annotatedWithExpression instanceof PsiMethodCallExpression expression) {
      final PsiMethod method = expression.resolveMethod();
      if (method != null) {
        if ("named".equals(method.getName())) {
          final PsiExpression[] expressions = expression.getArgumentList().getExpressions();
          return expressions.length > 0 ? expressions[0] : null;
        }
        else {
          PsiCodeBlock body = method.getBody();
          if (body != null) {
            final Set<PsiExpression> returns = new HashSet<>();

            body.accept(new JavaRecursiveElementVisitor() {
              @Override
              public void visitClass(@NotNull PsiClass aClass) {
              }

              @Override
              public void visitLambdaExpression(@NotNull PsiLambdaExpression expression) {
              }

              @Override
              public void visitReturnStatement(@NotNull PsiReturnStatement statement) {
                PsiExpression returnValue = statement.getReturnValue();
                if (returnValue != null) {
                  returns.add(returnValue);
                }
              }
            });

            for (PsiExpression psiExpression : returns) {
              final PsiExpression namedExpression = findNamedExpression(psiExpression);
              if (namedExpression != null) return namedExpression;
            }
          }
        }
      }
      final UCallExpression uCall = GuiceUtils.getCallExpression(expression);
      if (uCall != null) {
        final UExpression namedArg = GuiceUtils.getArgumentOfCallInChain(uCall, "named");
        if (namedArg != null && namedArg.getSourcePsi() instanceof PsiExpression named) {
          return named;
        }
      }
    }
    if (annotatedWithExpression instanceof PsiReferenceExpression) {
      final PsiElement resolve = ((PsiReferenceExpression)annotatedWithExpression).resolve();
      if (resolve instanceof PsiVariable) {
        PsiExpression initializer = ((PsiVariable)resolve).getInitializer();
        if (initializer != null) {
          return findNamedExpression(initializer);
        }
      }
    }
    return null;
  }

  public static @Nullable PsiClass getCallExpressionType(@NotNull UCallExpression expression, final String name) {
    PsiClass aClass = null;
    final UExpression uExpression = GuiceUtils.getArgumentOfCallInChain(expression, name);
    if (uExpression != null) {
      final PsiType type = GuiceUtils.getBindingTypeFromExpression(uExpression);
      if (type instanceof PsiClassType) {
        aClass = ((PsiClassType)type).resolve();
      }
    } else {
      final UCallExpression inCall = GuiceUtils.findCallInChain(expression, name);
      if (inCall != null) {
        final List<PsiType> typeArgs = inCall.getTypeArguments();
        if (!typeArgs.isEmpty()) {
          final PsiType type = typeArgs.getFirst();
          if (type instanceof PsiClassType) {
            aClass = ((PsiClassType)type).resolve();
          }
        }
      }
    }
    return aClass;
  }

}
