// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds {@link GuiceQualifier} values from declarations (fields, parameters, {@code @Provides} methods)
 * and from binder expressions ({@code annotatedWith(...)}, {@code Key.get(type, ...)}).
 */
@ApiStatus.Experimental
public final class GuiceQualifiers {
  private static final String NAMES_CLASS = "com.google.inject.name.Names";
  private static final String KEY_CLASS = "com.google.inject.Key";

  private GuiceQualifiers() {}

  // -----------------------------------------------------------------------
  // Declarations
  // -----------------------------------------------------------------------

  /**
   * Returns the qualifier of a declaration, or {@code null} if it has none.
   * An annotation that does not resolve gives {@link GuiceQualifier.Unknown},
   * unless its name is in a package that never holds qualifiers.
   */
  public static @Nullable GuiceQualifier fromDeclaration(@NotNull PsiModifierListOwner element) {
    return fromDeclaration(element, Set.of());
  }

  /**
   * Returns the qualifier of a declaration, ignoring any annotation whose qualified name is in
   * {@code ignoredAnnotations}.
   */
  public static @Nullable GuiceQualifier fromDeclaration(@NotNull PsiModifierListOwner element,
                                                         @NotNull Set<String> ignoredAnnotations) {
    PsiModifierList modifierList = element.getModifierList();
    GuiceQualifier unknown = null;

    if (modifierList != null) {
      for (PsiAnnotation annotation : modifierList.getAnnotations()) {
        String fqn = annotation.getQualifiedName();
        if (isKnownNonQualifier(fqn) || (fqn != null && ignoredAnnotations.contains(fqn))) continue;

        PsiClass annotationClass = annotation.resolveAnnotationType();
        if (annotationClass == null) {
          if (unknown == null) {
            PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
            unknown = new GuiceQualifier.Unknown(ref != null ? ref.getText() : String.valueOf(fqn));
          }
          continue;
        }
        String resolvedFqn = annotationClass.getQualifiedName();
        if (resolvedFqn == null || ignoredAnnotations.contains(resolvedFqn)) continue;
        if (GuiceAnnotations.NAMEDS.contains(resolvedFqn)) {
          return new GuiceQualifier.Named(AnnotationUtil.getStringAttributeValue(annotation, "value"));
        }
        if (AnnotationUtil.isAnnotated(annotationClass, GuiceAnnotations.BINDING_ANNOTATIONS, AnnotationUtil.CHECK_HIERARCHY)) {
          return fromAnnotationInstance(annotation, annotationClass, resolvedFqn);
        }
      }
    }

    // A Kotlin property without a use-site target puts qualifier annotations on the property,
    // not on the light backing field. Inspect UAST annotations on the source property.
    if (unknown == null && element instanceof PsiField && element.getNavigationElement() != element) {
      for (UAnnotation uAnno : getDeclarationUAnnotations(element.getNavigationElement())) {
        String fqn = uAnno.getQualifiedName();
        if (isKnownNonQualifier(fqn) || (fqn != null && ignoredAnnotations.contains(fqn))) continue;

        PsiClass annotationClass = uAnno.resolve();
        if (annotationClass == null) {
          if (unknown == null) {
            PsiElement sourcePsi = uAnno.getSourcePsi();
            unknown = new GuiceQualifier.Unknown(sourcePsi != null ? sourcePsi.getText() : String.valueOf(fqn));
          }
          continue;
        }
        String resolvedFqn = annotationClass.getQualifiedName();
        if (resolvedFqn == null || ignoredAnnotations.contains(resolvedFqn)) continue;
        if (GuiceAnnotations.NAMEDS.contains(resolvedFqn)) {
          UExpression valExpr = uAnno.findAttributeValue("value");
          return new GuiceQualifier.Named(valExpr != null ? UastUtils.evaluateString(valExpr) : null);
        }
        if (AnnotationUtil.isAnnotated(annotationClass, GuiceAnnotations.BINDING_ANNOTATIONS, AnnotationUtil.CHECK_HIERARCHY)) {
          PsiAnnotation javaPsi = uAnno.getJavaPsi();
          if (javaPsi != null) {
            return fromAnnotationInstance(javaPsi, annotationClass, resolvedFqn);
          }
          return new GuiceQualifier.Marker(resolvedFqn);
        }
      }
    }

    return unknown;
  }

  static @NotNull List<UAnnotation> getDeclarationUAnnotations(@NotNull PsiElement navElem) {
    ArrayList<UAnnotation> result = new ArrayList<>();
    UElement uElem = UastContextKt.toUElement(navElem);
    if (uElem instanceof UAnnotated uAnnotated) {
      result.addAll(uAnnotated.getUAnnotations());
    }
    for (PsiElement child : navElem.getChildren()) {
      UAnnotation direct = UastContextKt.toUElement(child, UAnnotation.class);
      if (direct != null) {
        result.add(direct);
        continue;
      }
      for (PsiElement grandChild : child.getChildren()) {
        UAnnotation nested = UastContextKt.toUElement(grandChild, UAnnotation.class);
        if (nested != null) {
          result.add(nested);
        }
      }
    }
    return result;
  }

  /**
   * Tells if a parameter is an assisted parameter. Such a parameter comes from the factory call,
   * not from the injector, so it is not an injection point.
   */
  static boolean isAssisted(@NotNull PsiModifierListOwner element) {
    return AnnotationUtil.isAnnotated(element, GuiceAnnotations.ASSISTED, 0);
  }

  private static @NotNull GuiceQualifier fromAnnotationInstance(@NotNull PsiAnnotation annotation,
                                                                @NotNull PsiClass annotationClass,
                                                                @NotNull String fqn) {
    TreeMap<String, String> values = new TreeMap<>();
    for (PsiMethod method : annotationClass.getMethods()) {
      if (method instanceof PsiAnnotationMethod) {
        String name = method.getName();
        PsiAnnotationMemberValue value = annotation.findAttributeValue(name);
        values.put(name, value == null ? "" : valueText(value));
      }
    }
    if (values.isEmpty()) {
      PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
      if (attributes.length == 0) return new GuiceQualifier.Marker(fqn);
      for (PsiNameValuePair pair : attributes) {
        String name = pair.getAttributeName();
        PsiAnnotationMemberValue value = pair.getValue();
        values.put(name, value == null ? "" : valueText(value));
      }
    }
    StringBuilder sb = new StringBuilder();
    values.forEach((k, v) -> sb.append(k).append('=').append(v).append(';'));
    return new GuiceQualifier.Instance(fqn, sb.toString());
  }

  private static @NotNull String valueText(@NotNull PsiAnnotationMemberValue value) {
    if (value instanceof PsiReferenceExpression ref && ref.resolve() instanceof PsiEnumConstant constant) {
      PsiClass enumClass = constant.getContainingClass();
      return (enumClass != null ? enumClass.getQualifiedName() : "") + "#" + constant.getName();
    }
    if (value instanceof PsiClassObjectAccessExpression classAccess) {
      return classAccess.getOperand().getType().getCanonicalText();
    }
    if (value instanceof PsiExpression expression) {
      Object constant = JavaPsiFacade.getInstance(value.getProject()).getConstantEvaluationHelper()
        .computeConstantExpression(expression);
      if (constant != null) return String.valueOf(constant);
    }
    return value.getText();
  }

  /**
   * Checks whether an annotation FQN is known to never be a Guice qualifier.
   * This check prevents false "unknown qualifier" values for common annotations that do not resolve.
   * Resolved annotations are checked for the qualifier meta-annotation instead.
   */
  private static boolean isKnownNonQualifier(@Nullable String fqn) {
    return fqn != null && (
      fqn.startsWith("java.") ||
      fqn.startsWith("javax.annotation.") ||
      fqn.startsWith("jakarta.annotation.") ||
      fqn.startsWith("kotlin.") ||
      fqn.startsWith("org.jetbrains.annotations.") ||
      fqn.startsWith("com.google.errorprone.") ||
      GuiceAnnotations.INJECTS.contains(fqn) ||
      fqn.equals("Override") || fqn.equals("Deprecated") || fqn.equals("SuppressWarnings"));
  }

  // -----------------------------------------------------------------------
  // Binder expressions
  // -----------------------------------------------------------------------

  /**
   * Returns the qualifier of a binding chain such as {@code bind(Foo.class).annotatedWith(...)},
   * {@code bind(Key.get(Foo.class, ...))}, or {@code build(Key.get(Factory.class, ...))},
   * or {@code null} if the binding is unqualified.
   */
  static @Nullable GuiceQualifier fromBindingChain(@NotNull UCallExpression outermostCall) {
    UCallExpression annotatedWith = GuiceUtils.findCallInChain(outermostCall, "annotatedWith");
    if (annotatedWith != null) {
      List<UExpression> args = annotatedWith.getValueArguments();
      if (!args.isEmpty()) return fromExpression(args.getFirst());
      List<PsiType> typeArgs = annotatedWith.getTypeArguments();
      if (!typeArgs.isEmpty()) return fromAnnotationType(typeArgs.getFirst(), annotatedWith);
      return unknown(annotatedWith);
    }
    UExpression bindArg = GuiceUtils.getArgumentOfCallInChain(outermostCall, "bind");
    if (bindArg == null) {
      bindArg = GuiceUtils.getArgumentOfCallInChain(outermostCall, "build");
    }
    UCallExpression keyGet = bindArg != null ? asKeyGet(bindArg) : null;
    if (keyGet != null && keyGet.getValueArgumentCount() > 1) {
      return fromExpression(keyGet.getValueArguments().get(1));
    }
    return null;
  }

  /**
   * Returns the qualifier of a multibinder or optional-binder call in the chain,
   * such as {@code Multibinder.newSetBinder(binder(), Foo.class, Names.named("a"))},
   * {@code MapBinder.newMapBinder(binder(), K.class, V.class, Names.named("a"))},
   * or {@code OptionalBinder.newOptionalBinder(binder(), Key.get(Foo.class, Names.named("a")))}.
   */
  static @Nullable GuiceQualifier fromBinderCall(@NotNull UCallExpression outermostCall) {
    GuiceExtensionIndex extensionIndex = GuiceExtensionIndex.get();
    for (String name : extensionIndex.getSingleTypeBinderMethodNames()) {
      UCallExpression call = GuiceUtils.findCallInChain(outermostCall, name);
      if (call != null) {
        List<UExpression> args = call.getValueArguments();
        if (args.size() >= 3) {
          return fromExpression(args.get(2));
        }
        if (args.size() == 2) {
          UCallExpression keyGet = asKeyGet(args.get(1));
          if (keyGet != null && keyGet.getValueArgumentCount() > 1) {
            return fromExpression(keyGet.getValueArguments().get(1));
          }
        }
        return null;
      }
    }
    for (String name : extensionIndex.getDualTypeBinderMethodNames()) {
      UCallExpression call = GuiceUtils.findCallInChain(outermostCall, name);
      if (call != null) {
        List<UExpression> args = call.getValueArguments();
        if (args.size() >= 4) {
          return fromExpression(args.get(3));
        }
        return null;
      }
    }
    return null;
  }

  /**
   * Returns the call {@code Key.get(...)} if the expression is such a call, possibly qualified.
   */
  static @Nullable UCallExpression asKeyGet(@NotNull UExpression expression) {
    UExpression e = GuiceUtils.skipParenthesesAndCasts(GuiceUtils.getSelectorIfQualified(expression));
    if (!(e instanceof UCallExpression call) || !"get".equals(call.getMethodName())) return null;
    PsiMethod method = call.resolve();
    PsiClass containingClass = method != null ? method.getContainingClass() : null;
    return containingClass != null && KEY_CLASS.equals(containingClass.getQualifiedName()) ? call : null;
  }

  /**
   * Returns the qualifier for an annotation argument: a class literal ({@code Db.class}, {@code Db::class.java}),
   * a call of {@code Names.named(...)}, or any other expression of an annotation type.
   */
  static @NotNull GuiceQualifier fromExpression(@NotNull UExpression expression) {
    // Db.class, Db::class.java, Db::class
    PsiType classLiteralType = GuiceUtils.getBindingTypeFromExpression(expression);
    if (classLiteralType != null) return fromAnnotationType(classLiteralType, expression);

    // Names.named("x"), named("x") with a static import
    UExpression e = GuiceUtils.skipParenthesesAndCasts(GuiceUtils.getSelectorIfQualified(expression));
    if (e instanceof UCallExpression call && "named".equals(call.getMethodName())) {
      PsiMethod method = call.resolve();
      PsiClass containingClass = method != null ? method.getContainingClass() : null;
      if (containingClass != null && NAMES_CLASS.equals(containingClass.getQualifiedName())) {
        List<UExpression> args = call.getValueArguments();
        return new GuiceQualifier.Named(args.isEmpty() ? null : UastUtils.evaluateString(args.getFirst()));
      }
    }

    // A helper method or a constant that returns Names.named("x").
    PsiElement sourcePsi = expression.getSourcePsi();
    if (sourcePsi instanceof PsiExpression psiExpression) {
      PsiExpression namedArg = GuiceInjectionUtil.findNamedExpression(psiExpression);
      if (namedArg != null) {
        Object value = JavaPsiFacade.getInstance(namedArg.getProject()).getConstantEvaluationHelper()
          .computeConstantExpression(namedArg);
        return new GuiceQualifier.Named(value instanceof String s ? s : null);
      }
    }

    // Any other annotation instance or @AutoAnnotation factory call.
    PsiType type = expression.getExpressionType();
    if (type instanceof PsiClassType classType) {
      PsiClass annotationClass = classType.resolve();
      if (annotationClass != null && annotationClass.isAnnotationType()) {
        if (e instanceof UCallExpression call) {
          GuiceQualifier instance = fromAnnotationFactoryCall(call, annotationClass);
          if (instance != null) return instance;
        }
        return fromAnnotationType(classType, expression);
      }
    }
    return unknown(expression);
  }

  private static @Nullable GuiceQualifier fromAnnotationFactoryCall(@NotNull UCallExpression call,
                                                                    @NotNull PsiClass annotationClass) {
    String fqn = annotationClass.getQualifiedName();
    if (fqn == null || GuiceAnnotations.NAMEDS.contains(fqn)) return null;

    List<PsiAnnotationMethod> attrMethods = new ArrayList<>();
    for (PsiMethod method : annotationClass.getMethods()) {
      if (method instanceof PsiAnnotationMethod attrMethod) {
        attrMethods.add(attrMethod);
      }
    }
    if (attrMethods.isEmpty()) return null;

    List<UExpression> args = call.getValueArguments();
    if (args.isEmpty()) return null;

    PsiMethod resolvedFactory = call.resolve();
    PsiParameter[] params = resolvedFactory != null ? resolvedFactory.getParameterList().getParameters() : PsiParameter.EMPTY_ARRAY;

    TreeMap<String, String> values = new TreeMap<>();
    for (PsiAnnotationMethod attrMethod : attrMethods) {
      String attrName = attrMethod.getName();
      int argIndex = -1;
      for (int i = 0; i < params.length && i < args.size(); i++) {
        if (attrName.equals(params[i].getName())) {
          argIndex = i;
          break;
        }
      }
      if (argIndex < 0 && attrMethods.size() == 1 && args.size() == 1) {
        argIndex = 0;
      }
      if (argIndex >= 0) {
        String evaluated = evaluateArgumentValue(args.get(argIndex));
        if (evaluated == null) return null;
        values.put(attrName, evaluated);
      }
      else {
        PsiAnnotationMemberValue defaultValue = attrMethod.getDefaultValue();
        values.put(attrName, defaultValue != null ? valueText(defaultValue) : "");
      }
    }

    StringBuilder sb = new StringBuilder();
    values.forEach((k, v) -> sb.append(k).append('=').append(v).append(';'));
    return new GuiceQualifier.Instance(fqn, sb.toString());
  }

  private static @Nullable String evaluateArgumentValue(@NotNull UExpression arg) {
    String strVal = UastUtils.evaluateString(arg);
    if (strVal != null) return strVal;
    PsiElement sourcePsi = arg.getSourcePsi();
    if (sourcePsi instanceof PsiAnnotationMemberValue memberValue) {
      return valueText(memberValue);
    }
    Object evaluated = arg.evaluate();
    return evaluated != null ? String.valueOf(evaluated) : null;
  }

  private static @NotNull GuiceQualifier fromAnnotationType(@NotNull PsiType type, @NotNull UElement context) {
    if (type instanceof PsiClassType classType) {
      PsiClass annotationClass = classType.resolve();
      String fqn = annotationClass != null ? annotationClass.getQualifiedName() : null;
      if (fqn != null) {
        return GuiceAnnotations.NAMEDS.contains(fqn) ? new GuiceQualifier.Named(null) : new GuiceQualifier.Marker(fqn);
      }
    }
    return unknown(context);
  }

  private static @NotNull GuiceQualifier unknown(@NotNull UElement element) {
    PsiElement sourcePsi = element.getSourcePsi();
    return new GuiceQualifier.Unknown(sourcePsi != null ? sourcePsi.getText() : element.asRenderString());
  }
}
