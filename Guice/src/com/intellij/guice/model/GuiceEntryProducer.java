// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.utils.GuiceUtils;

import com.intellij.psi.*;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/**
 * Produces {@link GuiceEntry} instances from PSI/UAST elements.
 *
 * <p>This is the <b>single source of truth</b> for what entries a given code element
 * contributes to the navigation index. All type resolution (Provider unwrapping,
 * multibinder unwrapping) happens here at creation time, not during matching.
 */
public final class GuiceEntryProducer {
  private GuiceEntryProducer() {}

  /**
   * Extracts all {@link GuiceEntry} instances from a class.
   * This covers:
   * <ul>
   *   <li>{@code @Inject} fields (INJECTION_POINT)</li>
   *   <li>{@code @Inject} constructor — produces BINDING_SITE for the class
   *       and INJECTION_POINT for each parameter</li>
   *   <li>{@code @Inject} method parameters (INJECTION_POINT)</li>
   *   <li>{@code @Provides} methods — produces BINDING_SITE for the return type
   *       and INJECTION_POINT for each parameter</li>
   *   <li>Binding calls in module {@code configure()} methods</li>
   * </ul>
   */
  public static @NotNull Set<GuiceEntry> extractFromClass(@NotNull PsiClass cls) {
    Set<GuiceEntry> entries = new HashSet<>();

    // @Inject fields → INJECTION_POINT
    for (PsiField field : cls.getFields()) {
      if (isInjectAnnotated(field)) {
        GuiceEntry entry = createInjectionPointEntry(field, field.getType());
        entries.add(entry);
      }
    }

    boolean isModule = InheritanceUtil.isInheritor(cls, "com.google.inject.Module");

    // Methods: @Inject methods (any class), @Provides methods (module classes only)
    for (PsiMethod method : cls.getMethods()) {
      boolean isInject = AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0);
      boolean isProvides = isModule && AnnotationUtil.isAnnotated(method,
          GuiceBindingMatchStrategy.getAllProvidesAnnotations(), 0);

      if (isInject || isProvides) {
        // Parameters → INJECTION_POINT
        for (PsiParameter param : method.getParameterList().getParameters()) {
          GuiceEntry entry = createInjectionPointEntry(param, param.getType());
          entries.add(entry);
        }
      }

      // @Provides → BINDING_SITE for return type (only in Module classes)
      if (isProvides) {
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
          PsiElement anchor = resolveDeclarationAnchor(method);

          // Fast path: single check against the cached set of all @ProvidesInto* annotations.
          // For standard @Provides methods (the common case), this avoids iterating strategies.
          if (AnnotationUtil.isAnnotated(method, GuiceBindingMatchStrategy.getProvidesIntoAnnotations(), 0)) {
            // @ProvidesInto*: find the matching strategy and contribute the collection type.
            for (GuiceBindingMatchStrategy strategy : GuiceBindingMatchStrategy.EP_NAME.getExtensionList()) {
              Collection<String> strategyAnnotations = strategy.getProvidesAnnotations();
              if (!strategyAnnotations.isEmpty()
                  && AnnotationUtil.isAnnotated(method, strategyAnnotations, 0)) {
                PsiType wrappedType = strategy.wrapProvidesType(method);
                if (wrappedType != null) {
                  entries.add(new GuiceEntry(
                      new GuiceBindingKey(wrappedType), method, anchor,
                      EntryRole.BINDING_SITE, GuiceEntryProducer::providesMethodText, strategy));
                }
                break;
              }
            }
          } else {
            // Standard @Provides: BINDING_SITE for the exact return type.
            entries.add(new GuiceEntry(
                new GuiceBindingKey(returnType, findQualifier(method)),
                method, anchor, EntryRole.BINDING_SITE,
                GuiceEntryProducer::providesMethodText));
          }
        }
      }
    }

    // @Inject constructors → BINDING_SITE for the class + INJECTION_POINT for params
    for (PsiMethod ctor : cls.getConstructors()) {
      if (AnnotationUtil.isAnnotated(ctor, GuiceAnnotations.INJECTS, 0)) {
        // The constructor IS a JIT binding for its containing class
        PsiClassType classType = JavaPsiFacade.getElementFactory(cls.getProject())
            .createType(cls);
        PsiElement anchor = resolveDeclarationAnchor(ctor);
        entries.add(new GuiceEntry(
            new GuiceBindingKey(classType),
            ctor, anchor, EntryRole.BINDING_SITE,
            GuiceEntryProducer::providesMethodText));

        // Each parameter → INJECTION_POINT
        for (PsiParameter param : ctor.getParameterList().getParameters()) {
          GuiceEntry entry = createInjectionPointEntry(param, param.getType());
          entries.add(entry);
        }
      }
    }

    // Binding calls in configure() for module classes
    if (isModule) {
      extractBindingCallEntries(cls, entries);
    }

    // Recurse into inner classes
    for (PsiClass inner : cls.getInnerClasses()) {
      entries.addAll(extractFromClass(inner));
    }

    return entries;
  }


  // -----------------------------------------------------------------------
  // Injection point entry creation
  // -----------------------------------------------------------------------

  private static boolean isInjectAnnotated(@NotNull PsiField field) {
    if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
      return true;
    }
    PsiElement navElem = field.getNavigationElement();
    if (navElem != null && navElem.getClass().getName().equals("org.jetbrains.kotlin.psi.KtProperty")) {
      try {
        java.lang.reflect.Method getAnnotationEntries = navElem.getClass().getMethod("getAnnotationEntries");
        List<?> entries = (List<?>) getAnnotationEntries.invoke(navElem);
        for (Object entryObj : entries) {
          if (entryObj instanceof PsiElement) {
            UAnnotation uAnno = UastContextKt.toUElement((PsiElement) entryObj, UAnnotation.class);
            if (uAnno != null) {
              String qName = uAnno.getQualifiedName();
              if (qName != null) {
                for (String injectAnno : GuiceAnnotations.INJECTS) {
                  if (qName.equals(injectAnno)) {
                    return true;
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
        // Ignore reflection errors to remain robust when Kotlin plugin is missing or different version
      }
    }
    return false;
  }

  /**
   * Creates an INJECTION_POINT entry for a field or parameter.
   * Handles Provider<T> unwrapping at creation time.
   */
  private static @NonNull GuiceEntry createInjectionPointEntry(
      @NotNull PsiModifierListOwner element, @NotNull PsiType declaredType) {
    // Unwrap Provider<T> → T at creation time
    PsiType resolvedType = GuiceUtils.getProviderType(declaredType);
    if (resolvedType == null) resolvedType = declaredType;

    PsiElement anchor = resolveDeclarationAnchor(element);

    PsiAnnotation qualifier = findQualifier(element);

    return new GuiceEntry(
        new GuiceBindingKey(resolvedType, qualifier),
        element, anchor, EntryRole.INJECTION_POINT,
        GuiceEntryProducer::injectionPointText);
  }

  /**
   * Produces a qualified display text for an injection point element.
   *
   * <ul>
   *   <li>Constructor parameter: {@code ClassName(paramName)}</li>
   *   <li>Method parameter: {@code ClassName.methodName(paramName)}</li>
   *   <li>Field: {@code ClassName.fieldName}</li>
   * </ul>
   */
  static @NotNull String injectionPointText(@NotNull PsiElement element) {
    if (element instanceof PsiParameter param) {
      PsiElement scope = param.getDeclarationScope();
      if (scope instanceof PsiMethod method) {
        if (method.isConstructor()) {
          PsiClass cls = method.getContainingClass();
          String className = cls != null ? cls.getName() : "";
          return className + "(" + param.getName() + ")";
        }
        return method.getNameIdentifier().getText() + "(" + param.getName() + ")";
      }
    }
    if (element instanceof PsiField field) {
      PsiClass cls = field.getContainingClass();
      if (cls != null) {
        return cls.getName() + "." + field.getName();
      }
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  /**
   * Produces display text for a {@code @Provides} method: {@code ClassName.methodName()}.
   * Omits parameter types to keep the navigation popup compact.
   */
  static @NotNull String providesMethodText(@NotNull PsiElement element) {
    if (element instanceof PsiMethod method) {
      var nameElement = method.getIdentifyingElement();
      if (nameElement != null) {
        return nameElement.getText() + "()";
      }
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  // -----------------------------------------------------------------------
  // Binding call entries (.to(), .toProvider(), mapBinder, etc.)
  // -----------------------------------------------------------------------

  /**
   * Extracts entries from binding calls inside module configure() methods.
   * Each {@code bind(X).to(Y)} chain produces:
   * <ul>
   *   <li>BINDING_SITE with key=X (the bound type)</li>
   *   <li>INJECTION_POINT with key=Y (the implementation reference)</li>
   * </ul>
   */
  private static void extractBindingCallEntries(@NotNull PsiClass moduleClass,
                                                @NotNull Set<GuiceEntry> entries) {
    Set<BindDescriptor> descriptors =
        GuiceInjectorManager.getBindingDescriptors(moduleClass);

    List<GuiceBindingMatchStrategy> strategies =
        GuiceBindingMatchStrategy.EP_NAME.getExtensionList();

    for (BindDescriptor bd : descriptors) {
      PsiElement bindExpr = bd.getBindExpression();
      if (bindExpr == null) continue;

      PsiElement anchor = getBindingAnchor(bindExpr);

      // Try each strategy — if one handles this descriptor, use its wrapType
      boolean handled = false;
      for (GuiceBindingMatchStrategy strategy : strategies) {
        if (strategy.getDescriptorClass().isInstance(bd)) {
          PsiType wrappedType = strategy.wrapType(bd);
          if (wrappedType != null) {
            entries.add(new GuiceEntry(
                new GuiceBindingKey(wrappedType), bindExpr, anchor, EntryRole.BINDING_SITE,
                strategy.getTextProvider(bd), strategy));
          }
          handled = true;
          break;
        }
      }
      if (handled) continue;

      // ---- Standard descriptors (bind().to(), untargeted, etc.) ----

      // BINDING_SITE for the bound type
      PsiClass boundClass = bd.getBoundClass();
      if (boundClass != null) {
        PsiType boundType = bd.getBoundType();
        if (boundType == null) {
          boundType = JavaPsiFacade.getElementFactory(boundClass.getProject())
              .createType(boundClass);
        }
        PsiAnnotation qualifier = getQualifierFromBinding(bd);
        entries.add(new GuiceEntry(
            new GuiceBindingKey(boundType, qualifier),
            bindExpr, anchor, EntryRole.BINDING_SITE,
            GuiceEntryProducer::standardBindText));
      }

      // INJECTION_POINT for the implementation class
      PsiClass bindingClass = bd.getBindingClass();
      if (bindingClass != null && !bindingClass.equals(boundClass)) {
        PsiType implType = JavaPsiFacade.getElementFactory(bindingClass.getProject())
            .createType(bindingClass);
        UCallExpression outermost = bd.getOutermostCall();
        PsiElement implAnchor = outermost != null ? getToCallAnchor(outermost) : bindExpr;
        entries.add(new GuiceEntry(
            new GuiceBindingKey(implType),
            bindExpr, implAnchor, EntryRole.INJECTION_POINT,
            GuiceEntryProducer::standardBindText));
      }
    }
  }

  // -----------------------------------------------------------------------
  // Presentable text for standard binding call chains
  // -----------------------------------------------------------------------

  /**
   * Produces a human-readable summary of a standard {@code bind().to()} chain.
   *
   * <p>Examples:
   * <ul>
   *   <li>{@code bind(Foo.class)} → "bind(Foo.class)"</li>
   *   <li>{@code bind(Foo.class).to(Bar.class)} → "bind(Foo.class).to(Bar.class)"</li>
   *   <li>{@code bind(Foo.class).annotatedWith(Named.class).toProvider(FooProvider.class)}
   *       → "bind(Foo.class).annotatedWith(Named.class).toProvider(FooProvider.class)"</li>
   * </ul>
   *
   * <p>Only standard {@code bind()} chains are handled. For strategy-handled
   * bindings (MapBinder, SetBinder, AssistedInject), see
   * {@link GuiceBindingMatchStrategy#getTextProvider}.
   */
  static @NotNull String standardBindText(@NotNull PsiElement element) {
    UElement uElement = UastContextKt.toUElement(element);
    uElement = GuiceUtils.getSelectorIfQualified(uElement);
    if (uElement instanceof UCallExpression call) {
      StringBuilder sb = new StringBuilder();
      if (appendCallInChain(call, sb, "bind", true)) {
        appendCallInChain(call, sb, "annotatedWith", false);
        if (appendCallInChain(call, sb, "to", false)) return sb.toString();
        if (appendCallInChain(call, sb, "toInstance", false)) return sb.toString();
        if (appendCallInChain(call, sb, "toProvider", false)) return sb.toString();
        if (appendCallInChain(call, sb, "toConstructor", false)) return sb.toString();
        // Untargeted binding: just bind(Foo.class)
        if (!sb.isEmpty()) return sb.toString();
      }
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  private static boolean appendCallInChain(@NotNull UCallExpression chain,
                                            @NotNull StringBuilder sb,
                                            @NotNull String methodName,
                                            boolean first) {
    UCallExpression found = GuiceUtils.findCallInChain(chain, methodName);
    if (found == null) return false;

    if (!first) sb.append(".");
    sb.append(methodName);

    List<PsiType> typeArgs = found.getTypeArguments();
    if (!typeArgs.isEmpty()) {
      sb.append("<").append(typeArgs.getFirst().getPresentableText()).append(">");
    }

    sb.append("(");
    List<UExpression> valueArgs = found.getValueArguments();
    if (!valueArgs.isEmpty()) {
      UExpression arg = valueArgs.getFirst();
      PsiElement sourcePsi = arg.getSourcePsi();
      sb.append(sourcePsi != null ? sourcePsi.getText() : arg.toString());
    }
    sb.append(")");
    return true;
  }


  // -----------------------------------------------------------------------
  // Anchor resolution helpers
  // -----------------------------------------------------------------------

  /**
   * Resolves the gutter anchor for any declaration (method, field, parameter, constructor)
   * to its <b>source PSI</b> declaration element.
   *
   * <p>Uses UAST to bridge from light PSI to source PSI. For Java this is a no-op
   * (the source PSI is the same element). For Kotlin, this bridges from
   * {@code SymbolLightMethod}/{@code SymbolLightField} to the source
   * {@code KtNamedFunction}/{@code KtProperty}/{@code KtPrimaryConstructor}.
   *
   * <p>The annotator uses the same element via {@code resolveAnnotatableOwner}
   * (which returns the leaf's parent = the same source declaration).
   * The visual gutter icon position is determined by the leaf token, not this anchor.
   */
  private static @NotNull PsiElement resolveDeclarationAnchor(@NotNull PsiElement element) {
    UElement uElement = UastContextKt.toUElement(element);
    if (uElement != null) {
      PsiElement sourcePsi = uElement.getSourcePsi();
      if (sourcePsi != null) return sourcePsi;
    }
    return element;
  }

  private static @NotNull PsiElement getBindingAnchor(@NotNull PsiElement bindExpression) {
    UElement uElement = UastContextKt.toUElement(bindExpression);

    // For chained calls like bind(Foo).to(Bar).in(Singleton), the bindExpression
    // is the outermost expression.  Walk DOWN to the innermost call (the bind()
    // call) to anchor the BINDING_SITE gutter icon there.
    if (uElement instanceof UQualifiedReferenceExpression) {
      UCallExpression innermost = GuiceUtils.findInnermostCall(uElement);
      if (innermost != null) {
        UIdentifier id = innermost.getMethodIdentifier();
        PsiElement psi = id != null ? id.getSourcePsi() : null;
        if (psi != null) return psi;
      }
    }
    if (uElement instanceof UCallExpression call) {
      UIdentifier id = call.getMethodIdentifier();
      PsiElement psi = id != null ? id.getSourcePsi() : null;
      return psi != null ? psi : bindExpression;
    }
    return bindExpression;
  }

  private static @NotNull PsiElement getToCallAnchor(@NotNull UCallExpression outermost) {
    // Walk the call chain to find the .to()/.toProvider() call
    UCallExpression current = outermost;
    while (current != null) {
      String name = current.getMethodName();
      if ("to".equals(name) || "toProvider".equals(name) || "toInstance".equals(name)
          || "toConstructor".equals(name)) {
        UIdentifier id = current.getMethodIdentifier();
        PsiElement psi = id != null ? id.getSourcePsi() : null;
        if (psi != null) return psi;
      }
      current = GuiceUtils.getReceiverCall(current);
    }
    PsiElement src = outermost.getSourcePsi();
    return src != null ? src : outermost.getJavaPsi();
  }

  // -----------------------------------------------------------------------
  // Qualifier detection
  // -----------------------------------------------------------------------

  private static @Nullable PsiAnnotation findQualifier(@NotNull PsiModifierListOwner element) {
    PsiModifierList modifierList = element.getModifierList();
    if (modifierList == null) return null;

    boolean hasUnresolved = false;
    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      if (isKnownNonQualifier(annotation.getQualifiedName())) {
        // Skip known non-qualifier annotation prefixes.
        continue;
      }
      PsiClass annotationClass = annotation.resolveAnnotationType();
      if (annotationClass != null) {
        if (AnnotationUtil.isAnnotated(annotationClass, GuiceAnnotations.BINDING_ANNOTATIONS,
            AnnotationUtil.CHECK_HIERARCHY)) {
          return annotation;
        }
        continue;
      }
      hasUnresolved = true;
    }

    // If any annotation couldn't be resolved and might be a qualifier,
    // return a sentinel to prevent false matches with unqualified injection points.
    if (hasUnresolved) {
      return createFakeAnnotation(element, "com.intellij.guice.UnresolvedQualifier");
    }
    return null;
  }

  /**
   * Checks whether an annotation FQN is known to never be a Guice qualifier.
   * Used to avoid false "unresolved qualifier" sentinels for standard annotations.
   */
  private static boolean isKnownNonQualifier(@Nullable String fqn) {
    return fqn != null && (
        fqn.startsWith("java.") ||
        fqn.startsWith("javax.annotation.") ||
        fqn.startsWith("jakarta.annotation.") ||
        fqn.startsWith("kotlin.") ||
        fqn.startsWith("org.jetbrains.annotations.") ||
        fqn.startsWith("com.google.inject.") ||
        fqn.startsWith("com.google.errorprone.") ||
        fqn.equals("Override") || fqn.equals("Deprecated") || fqn.equals("SuppressWarnings"));
  }

  private static @Nullable PsiAnnotation getQualifierFromBinding(@NotNull BindDescriptor bd) {
    UCallExpression expression = bd.getOutermostCall();
    if (expression == null) return null;

    UCallExpression annotatedWithCall = GuiceUtils.findCallInChain(expression, "annotatedWith");
    if (annotatedWithCall == null) return null;

    PsiClass annoClass = GuiceInjectionUtil.getCallExpressionType(expression, "annotatedWith");
    if (annoClass == null) {
      return createFakeAnnotation(bd.getBindExpression(), "com.intellij.guice.UnresolvedQualifier");
    }

    String fqn = annoClass.getQualifiedName();
    if (fqn == null) return null;

    if (GuiceAnnotations.NAMEDS.contains(fqn)) {
      UExpression arg = GuiceUtils.getArgumentOfCallInChain(expression, "annotatedWith");
      if (arg != null) {
        PsiElement sourcePsi = arg.getSourcePsi();
        if (sourcePsi instanceof PsiExpression) {
          PsiExpression namedExpr = GuiceInjectionUtil.findNamedExpression((PsiExpression) sourcePsi);
          if (namedExpr != null) {
            if (namedExpr instanceof PsiMethodCallExpression call) {
               PsiExpression[] args = call.getArgumentList().getExpressions();
               if (args.length == 1 && args[0] instanceof PsiLiteralExpression) {
                 Object val = ((PsiLiteralExpression) args[0]).getValue();
                 if (val instanceof String) {
                   return createFakeAnnotation(bd.getBindExpression(), fqn, (String) val);
                 }
               }
            }
            if (namedExpr instanceof PsiLiteralExpression) {
               Object val = ((PsiLiteralExpression) namedExpr).getValue();
               if (val instanceof String) {
                 return createFakeAnnotation(bd.getBindExpression(), fqn, (String) val);
               }
            }
          }
        }
      }
    }

    return createFakeAnnotation(bd.getBindExpression(), fqn);
  }

  private static @Nullable PsiAnnotation createFakeAnnotation(@Nullable PsiElement context, @NotNull String fqn) {
    if (context == null) return null;
    try {
      return JavaPsiFacade.getElementFactory(context.getProject())
          .createAnnotationFromText("@" + fqn, context);
    } catch (Exception e) {
      return null;
    }
  }

  private static @Nullable PsiAnnotation createFakeAnnotation(@Nullable PsiElement context, @NotNull String fqn, @NotNull String value) {
    if (context == null) return null;
    try {
      return JavaPsiFacade.getElementFactory(context.getProject())
          .createAnnotationFromText("@" + fqn + "(\"" + value + "\")", context);
    } catch (Exception e) {
      return null;
    }
  }
}
