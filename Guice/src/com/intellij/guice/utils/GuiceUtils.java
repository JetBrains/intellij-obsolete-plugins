// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class GuiceUtils {
  private GuiceUtils() { }

  public static boolean isInstantiable(PsiClass referentClass) {
    if (referentClass.isInterface() ||
        referentClass.isEnum() ||
        referentClass.isAnnotationType() ||
        referentClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
      return false;
    }
    final PsiMethod[] constructors = referentClass.getConstructors();
    if (constructors.length == 0) {
      return true;
    }
    for (PsiMethod constructor : constructors) {
      if (AnnotationUtil.isAnnotated(constructor, GuiceAnnotations.INJECTS, CHECK_HIERARCHY)) {
        return true;
      }

      if (constructor.getParameterList().getParametersCount() == 0) {
        return true;
      }
    }
    return false;
  }

  public static boolean provides(PsiClass providerClass, PsiClass providedClass) {
    if (InheritanceUtil.isInheritor(providerClass, GuiceClasses.PROVIDER)) {
       return providedClass.equals(getProvidedMethodType(providerClass));
    }
    PsiMethod[] allMethods = providerClass.getAllMethods();
    for (PsiMethod method : allMethods) {
      if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.PROVIDES, CHECK_HIERARCHY)) {
        final PsiType returnType = method.getReturnType();
        if (returnType instanceof PsiClassType && providedClass.equals(((PsiClassType)returnType).resolve())) {
          return true;
        }
      }
    }
    return false;
  }

  public static @Nullable PsiClass getProvidedType(@Nullable PsiClass providerClass) {
    if (providerClass != null && InheritanceUtil.isInheritor(providerClass, GuiceClasses.PROVIDER)) {
      return getProvidedMethodType(providerClass);
    }
    return null;
  }

  private static PsiClass getProvidedMethodType(@NotNull PsiClass providerClass) {
    final PsiMethod[] methods = providerClass.findMethodsByName("get", true);
    for (PsiMethod method : methods) {
      if (method.getParameterList().getParametersCount() != 0) {
        continue;
      }
      final PsiType returnType = method.getReturnType();
      if (returnType instanceof PsiClassType) {
        return  ((PsiClassType)returnType).resolve();
      }
    }
    return null;
  }

  public static boolean isBinding(PsiElement element) {
    if (!(element instanceof PsiMethodCallExpression callExpression)) {
      return false;
    }
    final PsiMethodCallExpression containingCall = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class, true);
    if (containingCall != null) {
      return false;
    }

    final PsiMethod method = callExpression.resolveMethod();
    if (method == null) {
      return false;
    }
    final PsiClass containingClass = method.getContainingClass();
    return containingClass != null && InheritanceUtil.isInheritor(containingClass, GuiceClasses.SCOPED_BINDING_BUILDER);
  }

  public static @Nullable PsiClass findImplementedClassForBinding(PsiMethodCallExpression call) {
    return getClassArgumentOfCallInChain(call, "bind");
  }

  public static @Nullable PsiMethodCallExpression findCallInChain(PsiMethodCallExpression call, String name) {
    final PsiReferenceExpression methodExpression = call.getMethodExpression();
    final String methodName = methodExpression.getReferenceName();
    if (name.equals(methodName)) {
      final PsiExpression[] args = call.getArgumentList().getExpressions();
      if (args.length == 1) {
        return call;
      }
      return null;
    }
    final PsiElement qualifier = methodExpression.getQualifier();
    if (!(qualifier instanceof PsiMethodCallExpression)) {
      return null;
    }
    return findCallInChain((PsiMethodCallExpression)qualifier, name);
  }

  public static @Nullable PsiExpression getArgumentOfCallInChain(PsiMethodCallExpression call, final String name) {
    final PsiMethodCallExpression inCall = findCallInChain(call, name);
    return inCall != null ? inCall.getArgumentList().getExpressions()[0] : null;
  }

  private static @Nullable PsiClass getClassArgumentOfCallInChain(PsiMethodCallExpression call, final String name) {
    final PsiExpression expression = getArgumentOfCallInChain(call, name);
    if (!(expression instanceof PsiClassObjectAccessExpression)) {
      return null;
    }
    final PsiType classType = ((PsiClassObjectAccessExpression)expression).getOperand().getType();
    if (classType instanceof PsiClassType) {
      return ((PsiClassType)classType).resolve();
    }
    return null;
  }

  public static @Nullable PsiClass findImplementingClassForBinding(PsiMethodCallExpression call) {
    return getClassArgumentOfCallInChain(call, "to");
  }

  public static @Nullable PsiClass findProvidingClassForBinding(PsiMethodCallExpression call) {
    return getClassArgumentOfCallInChain(call, "toProvider");
  }

  public static @Nullable PsiExpression findScopeForBinding(PsiMethodCallExpression call) {
    return getArgumentOfCallInChain(call, "in");
  }

  public static @Nullable PsiMethodCallExpression findScopeCallForBinding(PsiMethodCallExpression call) {
    return findCallInChain(call, "in");
  }

  public static @Nullable PsiMethodCallExpression findProvidingCallForBinding(PsiMethodCallExpression call) {
    return findCallInChain(call, "toProvider");
  }

  public static @Nullable PsiMethodCallExpression findBindingCallForBinding(PsiMethodCallExpression call) {
    return findCallInChain(call, "to");
  }

  public static @Nullable PsiMethodCallExpression findAnnotatedWithCallForBinding(PsiMethodCallExpression call) {
    return findCallInChain(call, "annotatedWith");
  }

  public static @Nullable String getScopeAnnotationForScopeExpression(PsiExpression arg) {
    if (!(arg instanceof PsiReferenceExpression referenceExpression)) {
      return null;
    }
    final PsiElement referent = referenceExpression.resolve();
    if (!(referent instanceof PsiField field)) {
      return null;
    }
    final PsiClass aClass = field.getContainingClass();
    if (aClass == null) {
      return null;
    }
    final String className = aClass.getQualifiedName();
    final String fieldName = field.getName();
    if ("SINGLETON".equals(fieldName) && "com.google.inject.Scopes".equals(className)) {
      return "com.google.inject.Singleton";
    }
    if ("REQUEST".equals(fieldName) && "com.google.inject.servlet.ServletScopes".equals(className)) {
      return "com.google.inject.servlet.RequestScoped";
    }
    if ("SESSION".equals(fieldName) && "com.google.inject.servlet.ServletScopes".equals(className)) {
      return "com.google.inject.servlet.SessionScoped";
    }
    return null;
  }
}