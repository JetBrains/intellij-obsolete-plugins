// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import static com.intellij.codeInsight.AnnotationUtil.CHECK_HIERARCHY;

public final class GuiceUtils {
  private GuiceUtils() { }

  public static @Nullable UCallExpression getCallExpression(@Nullable PsiElement element) {
    if (element == null) return null;
    final UElement uElement = UastContextKt.toUElement(element);
    final UExpression selector = getSelectorIfQualified(uElement);
    return selector instanceof UCallExpression ? (UCallExpression)selector : null;
  }

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
    for (String providerFqn : GuiceClasses.PROVIDERS) {
      if (InheritanceUtil.isInheritor(providerClass, providerFqn)) {
        return providedClass.equals(getProvidedMethodType(providerClass));
      }
    }
    PsiMethod[] allMethods = providerClass.getAllMethods();
    for (PsiMethod method : allMethods) {
      if (AnnotationUtil.isAnnotated(method, GuiceAnnotations.PROVIDES_ANNOTATIONS, CHECK_HIERARCHY)) {
        final PsiType returnType = method.getReturnType();
        if (returnType != null) {
          PsiClass resolved = null;
          if (returnType instanceof PsiClassType classType) {
            resolved = classType.resolve();
          } else if (returnType instanceof PsiPrimitiveType primitiveType) {
            resolved = JavaPsiFacade.getInstance(providerClass.getProject())
              .findClass(primitiveType.getBoxedTypeName(), providerClass.getResolveScope());
          }
          if (providedClass.equals(resolved)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public static @Nullable PsiClass getProvidedType(@Nullable PsiClass providerClass) {
    if (providerClass != null) {
      for (String providerFqn : GuiceClasses.PROVIDERS) {
        if (InheritanceUtil.isInheritor(providerClass, providerFqn)) {
          return getProvidedMethodType(providerClass);
        }
      }
    }
    return null;
  }

  public static @Nullable PsiType getProviderType(@Nullable PsiType type) {
    if (type instanceof PsiClassType classType) {
      final PsiClass psiClass = classType.resolve();
      if (psiClass == null) return null;

      // Only unwrap *direct* provider types: Provider<T>, CheckedProvider<T>, etc.
      // User-defined subtypes (e.g., SyncConfigBinder extends Provider<SyncConfig>)
      // are NOT unwrapped — Guice does not auto-create instances of arbitrary Provider
      // subtypes.  Injecting such a subtype requires its own explicit binding.
      final String fqn = psiClass.getQualifiedName();
      if (fqn != null && GuiceClasses.PROVIDERS.contains(fqn)) {
        final PsiTypeParameter[] typeParameters = psiClass.getTypeParameters();
        if (typeParameters.length > 0) {
          return classType.resolveGenerics().getSubstitutor().substitute(typeParameters[0]);
        }
      }
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
    return InheritanceUtil.isInheritor(containingClass, GuiceClasses.SCOPED_BINDING_BUILDER);
  }

  public static @Nullable PsiClass findImplementedClassForBinding(PsiMethodCallExpression call) {
    if (call == null) return null;
    final UCallExpression uCall = getCallExpression(call);
    return uCall != null ? findImplementedClassForBinding(uCall) : null;
  }

  public static @Nullable PsiType getBindingTypeFromExpression(PsiExpression expression) {
    if (expression == null) return null;
    final UExpression uExpr = org.jetbrains.uast.UastContextKt.toUElement(expression, UExpression.class);
    return uExpr != null ? getBindingTypeFromExpression(uExpr) : null;
  }

  public static @Nullable PsiMethodCallExpression findCallInChain(PsiMethodCallExpression call, String name) {
    if (call == null) return null;
    final UCallExpression uCall = getCallExpression(call);
    if (uCall != null) {
      final UCallExpression res = findCallInChain(uCall, name);
      if (res != null && res.getSourcePsi() instanceof PsiMethodCallExpression) {
        return (PsiMethodCallExpression)res.getSourcePsi();
      }
    }
    return null;
  }

  public static @Nullable PsiExpression getArgumentOfCallInChain(PsiMethodCallExpression call, final String name) {
    if (call == null) return null;
    final UCallExpression uCall = getCallExpression(call);
    if (uCall != null) {
      final UExpression res = getArgumentOfCallInChain(uCall, name);
      if (res != null && res.getSourcePsi() instanceof PsiExpression) {
        return (PsiExpression)res.getSourcePsi();
      }
    }
    return null;
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

  public static @Nullable PsiType getOptionalType(@Nullable PsiType type) {
    if (type instanceof PsiClassType classType) {
      final PsiClass psiClass = classType.resolve();
      if (psiClass != null && ("java.util.Optional".equals(psiClass.getQualifiedName()) ||
                               "com.google.common.base.Optional".equals(psiClass.getQualifiedName()))) {
        final PsiType[] parameters = classType.getParameters();
        if (parameters.length > 0) {
          return parameters[0];
        }
      }
    }
    return null;
  }

  public static @Nullable PsiType getMultibinderElementType(@Nullable PsiType type) {
    return getTypeParameter(type, "java.util.Set", 0);
  }

  public static @Nullable PsiType getMultibinderValueType(@Nullable PsiType type) {
    return getTypeParameter(type, "java.util.Map", 1);
  }

  /**
   * If {@code type} is {@code com.google.common.collect.Multimap<K, V>}, returns {@code K}.
   */
  public static @Nullable PsiType getMultimapKeyType(@Nullable PsiType type) {
    return getTypeParameter(type, "com.google.common.collect.Multimap", 0);
  }

  /**
   * If {@code type} is {@code com.google.common.collect.Multimap<K, V>}, returns {@code V}.
   */
  public static @Nullable PsiType getMultimapValueType(@Nullable PsiType type) {
    return getTypeParameter(type, "com.google.common.collect.Multimap", 1);
  }

  public static @Nullable UCallExpression findCallInChain(UCallExpression call, String name) {
    if (call == null) return null;
    if (name.equals(call.getMethodName())) {
      return call;
    }
    return findCallInChain(getReceiverCall(call), name);
  }

  public static @Nullable UExpression getArgumentOfCallInChain(UCallExpression call, final String name) {
    final UCallExpression inCall = findCallInChain(call, name);
    return (inCall != null && inCall.getValueArgumentCount() > 0) ? inCall.getValueArguments().get(0) : null;
  }

  public static @Nullable PsiClass findImplementedClassForBinding(UCallExpression call) {
    UCallExpression current = call;
    while (current != null) {
      final String name = current.getMethodName();
      if ("bind".equals(name) || "newOptionalBinder".equals(name) || "optionalBinder".equals(name) ||
          "newSetBinder".equals(name) || "setBinder".equals(name)) {
        final List<PsiType> typeArgs = current.getTypeArguments();
        if (!typeArgs.isEmpty()) {
          final PsiType type = typeArgs.get(0);
          if (type instanceof PsiClassType) {
            return ((PsiClassType)type).resolve();
          }
        } else {
          final List<UExpression> args = current.getValueArguments();
          if (args.size() > 1) {
            final PsiType type = getBindingTypeFromExpression(args.get(1));
            if (type instanceof PsiClassType) {
              return ((PsiClassType)type).resolve();
            }
          } else if (args.size() == 1) {
            final PsiType type = getBindingTypeFromExpression(args.get(0));
            if (type instanceof PsiClassType) {
              return ((PsiClassType)type).resolve();
            }
          }
        }
        return null;
      }
      if ("newMapBinder".equals(name) || "mapBinder".equals(name)) {
        final List<PsiType> typeArgs = current.getTypeArguments();
        if (typeArgs.size() > 1) {
          final PsiType type = typeArgs.get(1);
          if (type instanceof PsiClassType) {
            return ((PsiClassType)type).resolve();
          }
        } else {
          final List<UExpression> args = current.getValueArguments();
          if (args.size() > 2) {
            final UExpression valueExpression = args.get(2);
            final PsiType type = getBindingTypeFromExpression(valueExpression);
            if (type instanceof PsiClassType) {
              return ((PsiClassType)type).resolve();
            }
          }
        }
        return null;
      }
      current = getReceiverCall(current);
    }
    return null;
  }

  public static @Nullable UCallExpression getReceiverCall(@Nullable UCallExpression call) {
    if (call == null) return null;
    UExpression receiver = skipParenthesesAndCasts(getEffectiveReceiver(call));
    UExpression selector = skipParenthesesAndCasts(getSelectorIfQualified(receiver));
    return selector instanceof UCallExpression ? (UCallExpression)selector : null;
  }

  public static @Nullable PsiType getBindingTypeFromExpression(UExpression expression) {
    expression = skipParenthesesAndCasts(expression);
    if (expression instanceof UClassLiteralExpression classLiteral) {
      return classLiteral.getType();
    }
    final UExpression receiver = skipParenthesesAndCasts(getQualifierExpression(expression));
    if (receiver instanceof UClassLiteralExpression) {
      return getBindingTypeFromExpression(receiver);
    }
    if (expression instanceof UCallExpression callExpression) {
      if ("get".equals(callExpression.getMethodName())) {
        final PsiMethod method = callExpression.resolve();
        if (method != null) {
          final PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && "com.google.inject.Key".equals(containingClass.getQualifiedName())) {
            final List<UExpression> args = callExpression.getValueArguments();
            if (!args.isEmpty()) {
              return getBindingTypeFromExpression(args.get(0));
            }
          }
        }
      }
    }
    if (expression instanceof UBinaryExpressionWithType castExpression) {
      return getBindingTypeFromExpression(castExpression.getOperand());
    }
    if (expression instanceof UObjectLiteralExpression objectLiteral) {
      final PsiType type = objectLiteral.getExpressionType();
      if (type instanceof PsiClassType classType) {
        final PsiClass psiClass = classType.resolve();
        if (psiClass != null) {
          if ("com.google.inject.TypeLiteral".equals(psiClass.getQualifiedName())) {
            final PsiType[] parameters = classType.getParameters();
            if (parameters.length > 0) {
              return parameters[0];
            }
          } else if (psiClass instanceof PsiAnonymousClass anonymousClass) {
            final PsiClassType baseClassType = anonymousClass.getBaseClassType();
            final PsiClass baseClass = baseClassType.resolve();
            if (baseClass != null && "com.google.inject.TypeLiteral".equals(baseClass.getQualifiedName())) {
              final PsiType[] parameters = baseClassType.getParameters();
              if (parameters.length > 0) {
                return parameters[0];
              }
            }
          }
        }
      }
    }
    return null;
  }

  public static @Nullable PsiClass getQualifierFromExpression(UExpression expression) {
    if (expression instanceof UCallExpression callExpression) {
      if ("get".equals(callExpression.getMethodName())) {
        final PsiMethod method = callExpression.resolve();
        if (method != null) {
          final PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && "com.google.inject.Key".equals(containingClass.getQualifiedName())) {
            final List<UExpression> args = callExpression.getValueArguments();
            if (args.size() > 1) {
              final UExpression annoExpr = args.get(1);
              if (annoExpr instanceof UClassLiteralExpression classLiteral) {
                final PsiType type = classLiteral.getType();
                if (type instanceof PsiClassType) {
                  return ((PsiClassType)type).resolve();
                }
              }
              final PsiType type = annoExpr.getExpressionType();
              if (type instanceof PsiClassType) {
                return ((PsiClassType)type).resolve();
              }
            }
          }
        }
      }
    }
    return null;
  }

  public static @Nullable UExpression getNamedExpressionFromKeyGet(UExpression expression) {
    if (expression instanceof UCallExpression callExpression) {
      if ("get".equals(callExpression.getMethodName())) {
        final PsiMethod method = callExpression.resolve();
        if (method != null) {
          final PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && "com.google.inject.Key".equals(containingClass.getQualifiedName())) {
            final List<UExpression> args = callExpression.getValueArguments();
            if (args.size() > 1) {
              return findNamedExpression(args.get(1));
            }
          }
        }
      }
    }
    return null;
  }

  public static @Nullable UExpression findNamedExpression(final UExpression annotatedWithExpression) {
    if (annotatedWithExpression instanceof UCallExpression callExpression) {
      if ("named".equals(callExpression.getMethodName())) {
        final PsiMethod method = callExpression.resolve();
        if (method != null) {
          final PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && "com.google.inject.name.Names".equals(containingClass.getQualifiedName())) {
            final List<UExpression> args = callExpression.getValueArguments();
            if (!args.isEmpty()) {
              return args.get(0);
            }
          }
        }
      }
    }
    return null;
  }

  public static boolean isUntargetedBinding(@Nullable UCallExpression call) {
    UCallExpression current = call;
    while (current != null) {
      final String name = current.getMethodName();
      if ("to".equals(name) || "toInstance".equals(name) || "toProvider".equals(name) || "toConstructor".equals(name)) {
        return false;
      }
      current = getReceiverCall(current);
    }
    return true;
  }

  public static boolean areClassesEquivalent(@Nullable PsiClass c1, @Nullable PsiClass c2) {
    if (c1 == null || c2 == null) return false;
    if (c1 == c2) return true;
    return c1.getManager().areElementsEquivalent(c1, c2);
  }

  public static @Nullable PsiMethod getJitConstructor(@NotNull PsiClass psiClass) {
    for (PsiMethod constructor : psiClass.getConstructors()) {
      if (AnnotationUtil.isAnnotated(constructor, GuiceAnnotations.INJECTS, 0)) {
        return constructor;
      }
    }
    return null;
  }

  public static @Nullable UExpression skipParenthesesAndCasts(@Nullable UExpression expression) {
    UExpression current = expression;
    while (current != null) {
      if (current instanceof UParenthesizedExpression parenthesized) {
        current = parenthesized.getExpression();
      }
      else if (current instanceof UBinaryExpressionWithType cast) {
        current = cast.getOperand();
      }
      else {
        break;
      }
    }
    return current;
  }

  /**
   * Climbs up the UAST parent chain as long as the parent is a UQualifiedReferenceExpression,
   * returning the outermost qualified reference or the element itself if it is not qualified.
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code bind(Foo.class).to(FooImpl.class)}
   *               If element is the {@code to} call, returns the qualified reference representing the whole statement.</li>
   *   <li>Kotlin: {@code bind<Foo>().to<FooImpl>()}
   *               If element is the {@code to} call, returns the qualified reference representing the whole statement.</li>
   * </ul>
   */
  public static @NotNull UElement getOutermostQualifiedParent(@NotNull UElement element) {
    UElement current = element;
    while (current.getUastParent() instanceof UQualifiedReferenceExpression) {
      current = current.getUastParent();
    }
    return current;
  }

  /**
   * Returns the receiver of the method call. If the call has no direct receiver,
   * but is the selector of a parent UQualifiedReferenceExpression, returns the receiver
   * of that qualified reference.
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code bind(Foo.class).to(FooImpl.class)}
   *               If call is {@code to}, returns {@code bind(Foo.class)} call.</li>
   *   <li>Kotlin: {@code bind<Foo>().to<FooImpl>()}
   *               If call is {@code to}, returns {@code bind<Foo>()} call.</li>
   * </ul>
   */
  public static @Nullable UExpression getEffectiveReceiver(@NotNull UCallExpression call) {
    UExpression receiver = call.getReceiver();
    if (receiver == null) {
      final UElement parent = call.getUastParent();
      if (parent instanceof UQualifiedReferenceExpression qualified) {
        if (qualified.getSelector() == call) {
          receiver = qualified.getReceiver();
        }
      }
    }
    return receiver;
  }

  /**
   * Unwraps a UElement if it is a UQualifiedReferenceExpression, returning its selector.
   * Otherwise, returns the element itself if it is a UExpression, or null.
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code bind(Foo.class).to(FooImpl.class)}
   *               If element is the qualified expression, returns the {@code to} call.</li>
   *   <li>Kotlin: {@code bind<Foo>().to<FooImpl>()}
   *               If element is the qualified expression, returns the {@code to} call.</li>
   * </ul>
   */
  public static @Nullable UExpression getSelectorIfQualified(@Nullable UElement element) {
    if (element instanceof UQualifiedReferenceExpression qualified) {
      return qualified.getSelector();
    }
    return element instanceof UExpression ? (UExpression)element : null;
  }

  /**
   * Returns the qualifier expression (receiver) for either UQualifiedReferenceExpression or UCallExpression.
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code bind(Foo.class).to(FooImpl.class)}
   *               If expression is {@code to} call, returns {@code bind(Foo.class)}.</li>
   *   <li>Kotlin: {@code Foo::class.java}
   *               If expression is the {@code .java} call, returns {@code Foo::class}.</li>
   * </ul>
   */
  public static @Nullable UExpression getQualifierExpression(@NotNull UExpression expression) {
    if (expression instanceof UQualifiedReferenceExpression qualified) {
      return qualified.getReceiver();
    }
    if (expression instanceof UCallExpression call) {
      return getEffectiveReceiver(call);
    }
    return null;
  }

  /**
   * Resolves a PsiClass from a PsiType if the type is a PsiClassType.
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code PsiType type = ...; // PsiClassType for String.class}
   *               Returns {@code PsiClass} for {@code java.lang.String}.</li>
   *   <li>Kotlin: {@code val type: PsiType = ...}
   *               Returns {@code PsiClass} for the resolved type.</li>
   * </ul>
   */
  public static @Nullable PsiClass resolveClass(@Nullable PsiType type) {
    return type instanceof PsiClassType classType ? classType.resolve() : null;
  }

  /**
   * Checks if the type resolves to a class with the specified FQN, and if so,
   * returns the type parameter at the specified index.
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code PsiType type = ...; // Provider<BaseSessionService>}
   *               {@code getTypeParameter(type, "com.google.inject.Provider", 0)} returns type of {@code BaseSessionService}.</li>
   *   <li>Kotlin: {@code val type: PsiType = ... // Map<String, Int>}
   *               {@code getTypeParameter(type, "java.util.Map", 1)} returns type of {@code Int}.</li>
   * </ul>
   */
  public static @Nullable PsiType getTypeParameter(@Nullable PsiType type, @NotNull String classFqn, int index) {
    if (type instanceof PsiClassType classType) {
      final PsiClass psiClass = classType.resolve();
      if (psiClass != null && classFqn.equals(psiClass.getQualifiedName())) {
        final PsiType[] parameters = classType.getParameters();
        if (index >= 0 && index < parameters.length) {
          return parameters[index];
        }
      }
    }
    return null;
  }
}