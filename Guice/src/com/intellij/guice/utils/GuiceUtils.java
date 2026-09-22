// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import java.util.Collection;
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
      if (AnnotationUtil.isAnnotated(method, GuiceBindingMatchStrategy.getAllProvidesAnnotations(), CHECK_HIERARCHY)) {
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

      // Only unwrap *direct* provider types: Provider<T>, CheckedProvider<T>.
      // Subtypes like BackendProvider<T> extends CheckedProvider<T> are NOT unwrapped —
      // the concrete provider class matters (BackendProviderA<T> ≠ BackendProviderB<T>).
      // Those are handled on the producer side via @CheckedProvides(ProviderType.class).
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



  /**
   * Resolves a scope expression (the argument to {@code .in()}) to the group of
   * equivalent scope annotation FQNs.
   *
   * <p>Handles two forms:
   * <ul>
   *   <li>Field reference: {@code Scopes.SINGLETON}, {@code ServletScopes.REQUEST}</li>
   *   <li>Class literal: {@code Singleton.class} (Java), {@code Singleton::class.java} (Kotlin)</li>
   * </ul>
   *
   * @return the equivalent annotation FQNs (e.g., all three Singleton variants), or {@code null}
   */
  public static @Nullable Collection<String> getScopeAnnotationsForScopeExpression(UExpression arg) {
    // 1. Class literal: .in(Singleton.class) (Java) / .in(Singleton::class.java) (Kotlin)
    //    Both forms evaluate to type Class<X> — extract X from the type parameter.
    PsiType exprType = arg.getExpressionType();
    if (exprType instanceof PsiClassType ct) {
      PsiClass rawClass = ct.resolve();
      if (rawClass != null && "java.lang.Class".equals(rawClass.getQualifiedName())) {
        PsiType[] typeArgs = ct.getParameters();
        if (typeArgs.length == 1 && typeArgs[0] instanceof PsiClassType argType) {
          PsiClass scopeClass = argType.resolve();
          if (scopeClass != null) {
            return findScopeGroup(scopeClass.getQualifiedName());
          }
        }
      }
    }

    // 2. Field reference: .in(Scopes.SINGLETON)
    if (arg instanceof UReferenceExpression referenceExpression) {
      PsiElement referent = referenceExpression.resolve();
      if (referent instanceof PsiField field) {
        String annotation = getScopeAnnotationForField(field);
        return annotation != null ? findScopeGroup(annotation) : null;
      }
    }

    return null;
  }

  public static @Nullable String getScopeAnnotationForScopeExpression(PsiExpression arg) {
    if (!(arg instanceof PsiReferenceExpression referenceExpression)) {
      // Try class literal: .in(Singleton.class)
      if (arg instanceof PsiClassObjectAccessExpression classAccess) {
        PsiType type = classAccess.getOperand().getType();
        if (type instanceof PsiClassType ct) {
          PsiClass cls = ct.resolve();
          if (cls != null) {
            Collection<String> group = findScopeGroup(cls.getQualifiedName());
            return group != null && !group.isEmpty() ? group.iterator().next() : null;
          }
        }
      }
      return null;
    }
    final PsiElement referent = referenceExpression.resolve();
    if (!(referent instanceof PsiField field)) {
      return null;
    }
    return getScopeAnnotationForField(field);
  }

  /**
   * Finds the scope group containing the given annotation FQN.
   */
  private static @Nullable Collection<String> findScopeGroup(@Nullable String fqn) {
    if (fqn == null) return null;
    for (Collection<String> group : GuiceAnnotations.SCOPE_GROUPS) {
      if (group.contains(fqn)) return group;
    }
    return null;
  }

  private static @Nullable String getScopeAnnotationForField(@NotNull PsiField field) {
    final PsiClass aClass = field.getContainingClass();
    if (aClass == null) {
      return null;
    }
    final String className = aClass.getQualifiedName();
    final String fieldName = field.getName();
    if ("SINGLETON".equals(fieldName) && "com.google.inject.Scopes".equals(className)) {
      return GuiceAnnotations.GUICE_SINGLETON;
    }
    if ("REQUEST".equals(fieldName) && "com.google.inject.servlet.ServletScopes".equals(className)) {
      return GuiceAnnotations.REQUEST_SCOPED;
    }
    if ("SESSION".equals(fieldName) && "com.google.inject.servlet.ServletScopes".equals(className)) {
      return GuiceAnnotations.SESSION_SCOPED;
    }
    return null;
  }

  /**
   * If the type is an Optional ({@code java.util.Optional<T>} or {@code com.google.common.base.Optional<T>}),
   * extracts the element type {@code T}.
   */
  public static @Nullable PsiType getOptionalType(@Nullable PsiType type) {
    PsiType inner = getTypeParameter(type, "java.util.Optional", 0);
    if (inner == null) {
      inner = getTypeParameter(type, "com.google.common.base.Optional", 0);
    }
    return inner;
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
    PsiType type = findImplementedTypeForBinding(call);
    return type instanceof PsiClassType ct ? ct.resolve() : null;
  }

  /**
   * Like {@link #findImplementedClassForBinding}, but returns the full parameterized
   * {@link PsiType} instead of just the raw {@link PsiClass}.
   *
   * <p>This preserves generic type parameters (e.g., {@code Set<Foo>} instead of
   * just {@code Set}), which is critical for accurate type matching that avoids
   * false positives between different parameterizations of the same raw type.
   */
  public static @Nullable PsiType findImplementedTypeForBinding(UCallExpression call) {
    UCallExpression current = call;
    while (current != null) {
      final String name = current.getMethodName();
      if ("bind".equals(name) || "newOptionalBinder".equals(name) || "optionalBinder".equals(name) ||
          "newSetBinder".equals(name) || "setBinder".equals(name)) {
        final List<PsiType> typeArgs = current.getTypeArguments();
        if (!typeArgs.isEmpty()) {
          return typeArgs.get(0);
        } else {
          final List<UExpression> args = current.getValueArguments();
          if (args.size() > 1) {
            return getBindingTypeFromExpression(args.get(1));
          } else if (args.size() == 1) {
            return getBindingTypeFromExpression(args.get(0));
          }
        }
        return null;
      }
      if ("newMapBinder".equals(name) || "mapBinder".equals(name)) {
        final List<PsiType> typeArgs = current.getTypeArguments();
        if (typeArgs.size() > 1) {
          return typeArgs.get(1);
        } else {
          final List<UExpression> args = current.getValueArguments();
          if (args.size() > 2) {
            return getBindingTypeFromExpression(args.get(2));
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

  /**
   * Resolves the {@link PsiClass} from a binding call's first class argument.
   *
   * <p>Handles all calling conventions:
   * <ul>
   *   <li>Java: {@code call(Foo.class)} — direct {@code UClassLiteralExpression}</li>
   *   <li>Kotlin: {@code call(Foo::class.java)} — {@code .java} accessor wrapping
   *       {@code UClassLiteralExpression}</li>
   *   <li>Kotlin reified: {@code call<Foo>()} — type argument, no value arguments</li>
   * </ul>
   *
   * @param call the call expression to extract the class from
   * @return the resolved class, or {@code null} if unresolvable
   */
  public static @Nullable PsiClass resolveClassArgument(@NotNull UCallExpression call) {
    // 1. Try value arguments: call(Foo.class) or call(Foo::class.java)
    List<UExpression> args = call.getValueArguments();
    if (args.size() == 1) {
      PsiType type = getBindingTypeFromExpression(args.get(0));
      return type instanceof PsiClassType ct ? ct.resolve() : null;
    }

    // 2. Try type arguments: call<Foo>()  (Kotlin reified generics)
    if (args.isEmpty()) {
      List<PsiType> typeArgs = call.getTypeArguments();
      if (!typeArgs.isEmpty()) {
        PsiType type = typeArgs.get(0);
        return type instanceof PsiClassType ct ? ct.resolve() : null;
      }
    }

    return null;
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
   * Resolves a PSI element to the outermost {@link UCallExpression} in a Guice binding chain,
   * verifying that the resolved method belongs to a {@code ScopedBindingBuilder}.
   *
   * <p>Returns {@code null} if the element is not a binding call, is not on a
   * {@code ScopedBindingBuilder}, or is an inner call in a chain (not the outermost).
   *
   * <p>This is the common preamble shared by all {@code Move*Predicate.satisfiedBy()} methods.
   */
  public static @Nullable UCallExpression resolveOutermostBindingCall(@NotNull PsiElement element) {
    UCallExpression uCall = UastContextKt.toUElement(element, UCallExpression.class);
    if (uCall == null) return null;
    PsiMethod method = uCall.resolve();
    if (method == null) return null;
    PsiClass containingClass = method.getContainingClass();
    if (!InheritanceUtil.isInheritor(containingClass, GuiceClasses.SCOPED_BINDING_BUILDER)) {
      return null;
    }
    if (isInnerCallInChain(uCall)) return null;
    return uCall;
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
   * Returns {@code true} if this call is an inner (receiver) part of a method chain,
   * i.e. there is another call chained after it.
   *
   * <p>For {@code bind(Foo).to(Bar).in(Singleton)}, both {@code bind} and {@code to}
   * return {@code true}, while {@code in} returns {@code false}.
   */
  public static boolean isInnerCallInChain(@NotNull UCallExpression call) {
    UElement current = call;
    while (true) {
      UElement parent = current.getUastParent();
      if (!(parent instanceof UQualifiedReferenceExpression qre)) break;
      if (current.equals(qre.getReceiver())) {
        return true;
      }
      current = parent;
    }
    return false;
  }

  /**
   * Walks a UAST expression tree to find the innermost (leftmost) call expression.
   * For {@code bind(Foo).to(Bar).in(Singleton)}, returns the {@code bind(Foo)} call.
   */
  public static @Nullable UCallExpression findInnermostCall(@NotNull UElement element) {
    if (element instanceof UQualifiedReferenceExpression qre) {
      UCallExpression fromReceiver = findInnermostCall(qre.getReceiver());
      if (fromReceiver != null) return fromReceiver;
      UExpression selector = qre.getSelector();
      if (selector instanceof UCallExpression call) return call;
    }
    if (element instanceof UCallExpression call) {
      return call;
    }
    return null;
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
        if (call.equals(qualified.getSelector())) {
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
   * Extracts the type parameter at {@code index} from the given type, resolved
   * against the specified target class FQN.
   *
   * <p>This method is <b>inheritance-aware</b>: if the type doesn't directly have
   * the target FQN but extends/implements it (e.g., Kotlin's {@code kotlin.collections.Map}
   * implementing {@code java.util.Map}), the type parameters are resolved through the
   * supertype chain using {@link TypeConversionUtil#getSuperClassSubstitutor}.
   *
   * <p>If the resolved type parameter is a wildcard ({@code ? extends T}), the upper
   * bound {@code T} is returned.  This handles Kotlin's declaration-site variance
   * (e.g., {@code Map<K, out V>} compiling to {@code Map<K, ? extends V>}).
   *
   * <p>Examples:
   * <ul>
   *   <li>Java:   {@code Provider<BaseSessionService>}
   *               {@code getTypeParameter(type, "com.google.inject.Provider", 0)} → {@code BaseSessionService}</li>
   *   <li>Kotlin: {@code Map<String, Int>} (kotlin.collections.Map)
   *               {@code getTypeParameter(type, "java.util.Map", 1)} → {@code Int}</li>
   *   <li>Kotlin: {@code Set<Foo>} compiles to {@code Set<? extends Foo>}
   *               {@code getTypeParameter(type, "java.util.Set", 0)} → {@code Foo}</li>
   * </ul>
   */
  public static @Nullable PsiType getTypeParameter(@Nullable PsiType type, @NotNull String classFqn, int index) {
    if (!(type instanceof PsiClassType classType)) return null;

    PsiClass psiClass = classType.resolve();
    if (psiClass == null) return null;

    // Fast path: direct FQN match (common for Java sources).
    if (classFqn.equals(psiClass.getQualifiedName())) {
      PsiType[] parameters = classType.getParameters();
      if (index >= 0 && index < parameters.length) {
        return stripWildcard(parameters[index]);
      }
      return null;
    }

    // Slow path: walk the supertype chain (Kotlin collections, subtypes).
    PsiClass targetClass = JavaPsiFacade.getInstance(psiClass.getProject())
        .findClass(classFqn, psiClass.getResolveScope());
    if (targetClass == null || !InheritanceUtil.isInheritorOrSelf(psiClass, targetClass, true)) {
      return null;
    }

    PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(targetClass, classType);
    PsiTypeParameter[] typeParams = targetClass.getTypeParameters();
    if (index >= 0 && index < typeParams.length) {
      PsiType result = substitutor.substitute(typeParams[index]);
      return stripWildcard(result);
    }
    return null;
  }

  /**
   * Strips wildcard types to their upper bound.
   *
   * <p>Converts {@code ? extends T} → {@code T}.  This is needed for Kotlin's
   * declaration-site variance: {@code Set<out T>} compiles to {@code Set<? extends T>}
   * in bytecode, but Guice treats both as {@code Set<T>}.
   *
   * @param type the type, possibly a wildcard
   * @return the upper bound if a wildcard, or the type itself
   */
  public static @Nullable PsiType stripWildcard(@Nullable PsiType type) {
    if (type instanceof PsiWildcardType wildcard) {
      return wildcard.isExtends() ? wildcard.getExtendsBound() : wildcard.getBound();
    }
    return type;
  }
}