// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public final class GuiceInjectionUtil {

  public static @NotNull Set<InjectionPointDescriptor> getInjectionPoints(@NotNull PsiClass psiClass, boolean checkDeep) {
    Set<InjectionPointDescriptor> ips = new HashSet<>();
    if (psiClass.isValid()) {
      collectInjectionPoints(psiClass, ips, checkDeep);

      if (com.intellij.psi.util.InheritanceUtil.isInheritor(psiClass, "com.google.inject.Module")) {
        ips.addAll(getGetProviderInjectionPoints(psiClass));
      }
    }
    return ips;
  }

  private static void collectInjectionPoints(@NotNull PsiClass psiClass, @NotNull Set<InjectionPointDescriptor> ips, boolean checkDeep) {
    for (PsiField field : psiClass.getFields()) {
      if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
        ips.add(new InjectionPointDescriptor(field));
      }
    }
    for (PsiMethod method : psiClass.getMethods()) {
      if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0) ||
          AnnotationUtil.isAnnotated(method, GuiceBindingMatchStrategy.getAllProvidesAnnotations(), 0)) {
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
          ips.add(new InjectionPointDescriptor(parameter));
        }
      }
    }
    for (PsiMethod constructor : psiClass.getConstructors()) {
      if (AnnotationUtil.isAnnotated(constructor, GuiceAnnotations.INJECTS, 0)) {
        for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
          ips.add(new InjectionPointDescriptor(parameter));
        }
      }
    }
    if (checkDeep) {
      PsiClass superClass = psiClass.getSuperClass();
      if (superClass != null && !"java.lang.Object".equals(superClass.getQualifiedName())) {
        collectInjectionPoints(superClass, ips, true);
      }
    }
  }

  private static @NotNull Collection<InjectionPointDescriptor> getGetProviderInjectionPoints(@NotNull PsiClass psiClass) {
    return getGetProviderInjectionPoints(psiClass.getProject(), new com.intellij.psi.search.LocalSearchScope(psiClass.getNavigationElement()));
  }

  private static @NotNull Collection<InjectionPointDescriptor> getGetProviderInjectionPoints(@NotNull Project project, @NotNull com.intellij.psi.search.SearchScope scope) {
    final List<InjectionPointDescriptor> ips = new ArrayList<>();
    final Set<PsiElement> expressions = new HashSet<>();
    expressions.addAll(getUastCallExpressions(project, scope, "com.google.inject.Binder", "getProvider"));
    expressions.addAll(getUastCallExpressions(project, scope, "com.google.inject.AbstractModule", "getProvider"));
    expressions.addAll(getUastCallExpressions(project, scope, "com.google.inject.PrivateModule", "getProvider"));

    for (PsiElement expr : expressions) {
      ips.add(new InjectionPointDescriptor(expr));
    }
    return ips;
  }

  private static @NotNull Set<PsiElement> getUastCallExpressions(@NotNull Project project,
                                                                 @NotNull com.intellij.psi.search.SearchScope scope,
                                                                 @NotNull String fqn,
                                                                 @NotNull String methodName) {
    Set<PsiElement> expressions = new HashSet<>();
    final PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
    if (aClass != null) {
      final PsiMethod[] methods = aClass.findMethodsByName(methodName, false);
      for (PsiMethod method : methods) {
        for (PsiReference reference : com.intellij.psi.search.searches.ReferencesSearch.search(method, scope).findAll()) {
          final PsiElement element = reference.getElement();
          final UCallExpression uCall = org.jetbrains.uast.UastUtils.findContaining(element, UCallExpression.class);
          if (uCall != null) {
            final PsiElement sourcePsi = uCall.getSourcePsi();
            if (sourcePsi != null) {
              expressions.add(sourcePsi);
            }
          }
        }
      }
    }
    return expressions;
  }

  public static boolean checkBindingAnnotations(@NotNull Set<? extends PsiAnnotation> annotations, @NotNull Set<? extends PsiAnnotation> candidateAnnotations) {
    if (annotations.size() != candidateAnnotations.size()) return false;

    for (PsiAnnotation psiAnnotation : annotations) {
      boolean hasAnno = false;
      for (PsiAnnotation annotation : candidateAnnotations) {
        if (compareAnnotations(psiAnnotation, annotation)) {
          hasAnno = true; break;
        }
      }
      if (!hasAnno) return false;
    }
    return true;
  }

  private static boolean compareAnnotations(@NotNull PsiAnnotation anno, @NotNull PsiAnnotation candidateAnno) {
    return AnnotationUtil.equal(anno, candidateAnno);
  }


  public static boolean checkBindingAnnotations(InjectionPointDescriptor ip, @NotNull BindDescriptor descriptor) {
    final Set<PsiAnnotation> bindingAnnotations = ip.getBindingAnnotations();
    if (bindingAnnotations.size() > 1) return false;

    if (descriptor instanceof com.intellij.guice.model.beans.JitBindDescriptor) {
      return bindingAnnotations.isEmpty();
    }

    final UCallExpression expression = GuiceUtils.getCallExpression(descriptor.getBindExpression());
    if (expression == null) return false;

    PsiClass annotatedWith = getCallExpressionType(expression, "annotatedWith");
    if (annotatedWith == null) {
      final UExpression bindArg = GuiceUtils.getArgumentOfCallInChain(expression, "bind");
      if (bindArg != null) {
        annotatedWith = GuiceUtils.getQualifierFromExpression(bindArg);
      }
    }
    if (annotatedWith == null) {
      // Check whether .annotatedWith() is textually present in the chain even though
      // we couldn't resolve its argument (e.g., the annotation class doesn't exist yet).
      // In that case the binding explicitly has a qualifier — it must not match
      // unqualified injection points.
      if (GuiceUtils.findCallInChain(expression, "annotatedWith") != null) {
        return false;
      }
      return bindingAnnotations.isEmpty();
    }

    if (bindingAnnotations.size() == 1) {
      final PsiAnnotation bindingAnno = bindingAnnotations.iterator().next();
      final String annotatedWithName = annotatedWith.getQualifiedName();
      final String bindingAnnoName = bindingAnno.getQualifiedName();
      if (annotatedWithName != null && GuiceAnnotations.NAMEDS.contains(annotatedWithName)) {
        if (bindingAnnoName != null && GuiceAnnotations.NAMEDS.contains(bindingAnnoName)) {
          final UExpression annotatedWithExpression = GuiceUtils.getArgumentOfCallInChain(expression, "annotatedWith");
          if (annotatedWithExpression != null) {
            final PsiElement sourcePsi = annotatedWithExpression.getSourcePsi();
            if (sourcePsi instanceof PsiExpression) {
              final String nameValue = getNameValue((PsiExpression)sourcePsi);
              if (nameValue == null) return true; // impossible to define name
              return nameValue.equals(AnnotationUtil.getStringAttributeValue(bindingAnno, "value"));
            }
            return true;
          }
          final UExpression bindArg = GuiceUtils.getArgumentOfCallInChain(expression, "bind");
          if (bindArg != null) {
            final UExpression namedExpr = GuiceUtils.getNamedExpressionFromKeyGet(bindArg);
            if (namedExpr != null) {
              final PsiElement sourcePsi = namedExpr.getSourcePsi();
              if (sourcePsi instanceof PsiExpression) {
                final String nameValue = getNameValue((PsiExpression)sourcePsi);
                if (nameValue == null) return true;
                return nameValue.equals(AnnotationUtil.getStringAttributeValue(bindingAnno, "value"));
              }
              return true;
            }
          }
        }
        return false;
      }
      final String bindingAnnoQualifiedName = bindingAnno.getQualifiedName();
      if (bindingAnnoQualifiedName != null) {
        return bindingAnnoQualifiedName.equals(annotatedWith.getQualifiedName());
      }
    }

    return false;
  }

  private static @Nullable String getNameValue(@NotNull PsiExpression annotatedWithExpression) {
    final PsiExpression namedExpression = findNamedExpression(annotatedWithExpression);
    if (namedExpression != null) {
      final Pair<PsiElement, String> pair = StringExpressionHelper.evaluateExpression(namedExpression);
      if (pair != null) {
        return pair.getSecond();
      }
    }
    return null;
  }

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
