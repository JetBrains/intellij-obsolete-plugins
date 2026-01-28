// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers;

import com.intellij.helidon.constants.HelidonConstants;
import com.intellij.microservices.jvm.pathvars.usages.PathVariableUsageUastReferenceProvider;
import com.intellij.microservices.url.parameters.PathVariableDefinitionsSearcher;
import com.intellij.openapi.project.Project;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.microservices.jvm.pathvars.usages.AnnotationParamSearcherUtils.processPathVariables;

public final class HelidonHttpRequestPathParamReferenceProvider extends PathVariableUsageUastReferenceProvider {
  public static final HelidonHttpRequestPathParamReferenceProvider INSTANCE = new HelidonHttpRequestPathParamReferenceProvider();

  private HelidonHttpRequestPathParamReferenceProvider() {}

  @Override
  public PathVariableDefinitionsSearcher getSearcher() {
    return new MyPathVariableDefinitionsSearcher();
  }

  private static class MyPathVariableDefinitionsSearcher implements PathVariableDefinitionsSearcher {
    @Override
    public boolean processDefinitions(@NotNull PsiElement context,
                                      @NotNull Processor<? super PomTargetPsiElement> processor) {
      PsiMethod declaration = PsiTreeUtil.getParentOfType(context, PsiMethod.class);
      if (isHandlerMethodCandidate(declaration)) {
        MethodReferencesSearch.search(declaration, declaration.getResolveScope(), true).forEach(reference -> {
          PsiMethodCallExpression methodCallExpression =
            PsiTreeUtil.getParentOfType(reference.getElement(), PsiMethodCallExpression.class);
          if (methodCallExpression != null) {
            for (PsiExpression expression : methodCallExpression.getArgumentList().getExpressions()) {
              if (expression instanceof PsiLiteralExpression) {
                if (!processPathVariables(expression, processor)) return false;
              }
              else {
                for (PsiLiteralExpression literalExpression : PsiTreeUtil.findChildrenOfType(expression, PsiLiteralExpression.class)) {
                  if (!processPathVariables(literalExpression, processor)) return false;
                }
              }
            }
          }
          return true;
        });
      }

      return true;
    }
  }

  private static boolean isHandlerMethodCandidate(@Nullable PsiMethod declaration) {
    if (declaration == null) return false;
    PsiParameter[] parameters = declaration.getParameterList().getParameters();
    return parameters.length == 2
           && parameters[0].getType().isAssignableFrom(getTypeByName(HelidonConstants.HTTP_SERVER_REQUEST, declaration.getProject()))
           && parameters[1].getType().isAssignableFrom(getTypeByName(HelidonConstants.HTTP_SERVER_RESPONSE, declaration.getProject()));
  }

  private static @NotNull PsiClassType getTypeByName(@NotNull String request, @NotNull Project project) {
    return PsiType.getTypeByName(request, project, GlobalSearchScope.allScope(project));
  }
}
