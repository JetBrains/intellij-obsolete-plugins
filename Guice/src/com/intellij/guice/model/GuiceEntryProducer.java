// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
   * Extracts all {@link GuiceEntry} instances from the classes of a file.
   * This includes nested, local and anonymous classes, for example {@code install(new AbstractModule() {...})}.
   */
  public static @NotNull Set<GuiceEntry> extractFromFile(@NotNull PsiFile file) {
    Set<GuiceEntry> entries = new HashSet<>();
    for (PsiClass cls : GuiceInjectorManager.collectAllClasses(file)) {
      entries.addAll(extractFromClass(cls));
    }
    return entries;
  }

  /**
   * Extracts all {@link GuiceEntry} instances from a class, without its nested classes.
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
  private static @NotNull Set<GuiceEntry> extractFromClass(@NotNull PsiClass cls) {
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
      // The constructor loop below handles constructors.
      if (method.isConstructor()) continue;
      boolean isInject = AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0);
      boolean isProvides = isModule && AnnotationUtil.isAnnotated(method,
          GuiceBindingMatchStrategy.getAllProvidesAnnotations(), 0);

      if (isInject || isProvides) {
        // Parameters → INJECTION_POINT
        for (PsiParameter param : method.getParameterList().getParameters()) {
          if (GuiceQualifiers.isAssisted(param)) continue;
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
          boolean isKotlinMethod = method.getNavigationElement() != method;
          if (AnnotationUtil.isAnnotated(method, GuiceBindingMatchStrategy.getProvidesIntoAnnotations(), 0)) {
            // @ProvidesInto*: find the matching strategy and contribute the collection type.
            for (GuiceBindingMatchStrategy strategy : GuiceBindingMatchStrategy.EP_NAME.getExtensionList()) {
              Collection<String> strategyAnnotations = strategy.getProvidesAnnotations();
              if (!strategyAnnotations.isEmpty()
                  && AnnotationUtil.isAnnotated(method, strategyAnnotations, 0)) {
                for (PsiType wrappedType : strategy.wrapProvidesTypes(method)) {
                  PsiType keyType = isKotlinMethod ? unwrapKotlinWildcards(wrappedType) : wrappedType;
                  entries.add(new GuiceEntry(
                      new GuiceBindingKey(keyType, GuiceQualifiers.fromDeclaration(method)), method, anchor,
                      EntryRole.BINDING_SITE, GuiceEntryProducer::providesMethodText));
                }
                break;
              }
            }
          } else {
            // Standard @Provides: BINDING_SITE for the exact return type.
            PsiType keyType = isKotlinMethod ? unwrapKotlinWildcards(returnType) : returnType;
            entries.add(new GuiceEntry(
                new GuiceBindingKey(keyType, GuiceQualifiers.fromDeclaration(method)),
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
        PsiElement anchor = resolveDeclarationAnchor(ctor);
        entries.add(new GuiceEntry(
            GuiceBindingKey.forClass(cls),
            ctor, anchor, EntryRole.BINDING_SITE,
            GuiceEntryProducer::providesMethodText));

        // Each parameter → INJECTION_POINT
        for (PsiParameter param : ctor.getParameterList().getParameters()) {
          if (GuiceQualifiers.isAssisted(param)) continue;
          GuiceEntry entry = createInjectionPointEntry(param, param.getType());
          entries.add(entry);
        }
      }
    }

    // @ImplementedBy and @ProvidedBy → BINDING_SITE for the class (a JIT binding)
    addJitAnnotationEntries(cls, GuiceAnnotations.IMPLEMENTED_BY, entries);
    addJitAnnotationEntries(cls, GuiceAnnotations.PROVIDED_BY, entries);

    // Binding calls in configure() for module classes
    if (isModule) {
      extractBindingCallEntries(cls, entries);
    }

    return entries;
  }


  /**
   * Adds the entries of an {@code @ImplementedBy(Impl.class)} or {@code @ProvidedBy(FooProvider.class)} annotation.
   * The annotation is a BINDING_SITE for the annotated class.
   * Like {@code .to(Impl.class)}, it is also an implementation reference to the class in the annotation value.
   */
  private static void addJitAnnotationEntries(@NotNull PsiClass cls, @NotNull String annotationFqn,
                                              @NotNull Set<GuiceEntry> entries) {
    UClass uClass = UastContextKt.toUElement(cls, UClass.class);
    UAnnotation annotation = uClass != null ? uClass.findAnnotation(annotationFqn) : null;
    PsiElement annotationPsi = annotation != null ? annotation.getSourcePsi() : null;
    if (annotationPsi == null) return;

    PsiElement classAnchor = resolveDeclarationAnchor(cls);
    entries.add(GuiceEntry.defaultBinding(GuiceBindingKey.forClass(cls), annotationPsi, classAnchor,
                                          PsiElement::getText));

    UExpression value = GuiceUtils.skipParenthesesAndCasts(annotation.findDeclaredAttributeValue("value"));
    if (value instanceof UClassLiteralExpression literal && literal.getType() instanceof PsiClassType classType) {
      PsiClass valueClass = classType.resolve();
      if (valueClass != null && !valueClass.equals(cls)) {
        entries.add(GuiceEntry.implementationReference(GuiceBindingKey.forClass(valueClass), annotationPsi,
                                                       classAnchor, PsiElement::getText));
      }
    }
  }

  // -----------------------------------------------------------------------
  // Injection point entry creation
  // -----------------------------------------------------------------------

  private static boolean isInjectAnnotated(@NotNull PsiField field) {
    if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
      return true;
    }
    // A Kotlin property without a use-site target puts @Inject on the property, not on the backing field.
    // The light field does not see such an annotation, so ask UAST for the source declaration.
    PsiElement navElem = field.getNavigationElement();
    if (navElem == null || navElem == field) {
      return false;
    }
    UElement uElement = UastContextKt.toUElement(navElem);
    if (!(uElement instanceof UAnnotated annotated)) {
      return false;
    }
    for (String injectAnno : GuiceAnnotations.INJECTS) {
      if (annotated.findAnnotation(injectAnno) != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates an INJECTION_POINT entry for a field or parameter.
   * Handles Provider<T> unwrapping at creation time.
   */
  private static @NotNull GuiceEntry createInjectionPointEntry(
      @NotNull PsiModifierListOwner element, @NotNull PsiType declaredType) {
    PsiType normalizedDeclaredType = element.getNavigationElement() != element
                                     ? unwrapKotlinWildcards(declaredType)
                                     : declaredType;
    // Unwrap Provider<T> → T at creation time
    PsiType resolvedType = GuiceUtils.getProviderType(normalizedDeclaredType);
    if (resolvedType == null) resolvedType = normalizedDeclaredType;

    PsiElement anchor = resolveDeclarationAnchor(element);

    GuiceQualifier qualifier = GuiceQualifiers.fromDeclaration(element);

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
   *   <li>Method parameter: {@code methodName(paramName)}</li>
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
        return method.getName() + "(" + param.getName() + ")";
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

      // Try each strategy — if one handles this descriptor, use its wrapTypes
      boolean handled = false;
      for (GuiceBindingMatchStrategy strategy : strategies) {
        Class<? extends BindDescriptor> descriptorClass = strategy.getDescriptorClass();
        if (descriptorClass != null && descriptorClass.isInstance(bd)) {
          UCallExpression outermostCall = bd.getOutermostCall();
          GuiceQualifier qualifier = outermostCall != null ? GuiceQualifiers.fromBinderCall(outermostCall) : null;
          for (PsiType wrappedType : strategy.wrapTypes(bd)) {
            entries.add(new GuiceEntry(
                new GuiceBindingKey(wrappedType, qualifier), bindExpr, anchor, EntryRole.BINDING_SITE,
                strategy.getTextProvider(bd)));
          }
          handled = true;
          break;
        }
      }
      if (handled) continue;

      // ---- Standard descriptors (bind().to(), untargeted, etc.) ----

      // BINDING_SITE for the bound type.
      // The .to() tail of a multibinder element, for example newSetBinder(...).addBinding().to(Impl.class),
      // binds no key of its own: the multibinder descriptor gives the keys.
      PsiClass boundClass = bd.getBoundClass();
      PsiType boundType = bd.getBoundType();
      if (boundType == null && boundClass != null) {
        boundType = JavaPsiFacade.getElementFactory(boundClass.getProject())
            .createType(boundClass);
      }
      boolean elementChain = isBinderElementChain(bd.getOutermostCall());
      if (boundType != null && !elementChain) {
        UCallExpression outermostCall = bd.getOutermostCall();
        GuiceQualifier qualifier = outermostCall != null ? GuiceQualifiers.fromBindingChain(outermostCall) : null;
        entries.add(new GuiceEntry(
            new GuiceBindingKey(boundType, qualifier),
            bindExpr, anchor, EntryRole.BINDING_SITE,
            GuiceEntryProducer::standardBindText));
      }

      // INJECTION_POINT for the implementation or provider class
      PsiClass implClass = bd instanceof BindToProviderDescriptor pbd ? pbd.getProviderClass() : bd.getBindingClass();
      if (implClass != null && (elementChain || !implClass.equals(boundClass))) {
        UCallExpression outermost = bd.getOutermostCall();
        PsiElement implAnchor = outermost != null ? getToCallAnchor(outermost) : bindExpr;
        entries.add(GuiceEntry.implementationReference(
            GuiceBindingKey.forClass(implClass),
            bindExpr, implAnchor,
            GuiceEntryProducer::standardBindText));
      }
    }

    extractGetProviderEntries(moduleClass, entries);
  }

  /** Classes that declare {@code getProvider(Class)} and {@code getProvider(Key)} for use inside a module. */
  private static final Set<String> GET_PROVIDER_OWNERS = Set.of(
      "com.google.inject.Binder",
      "com.google.inject.PrivateBinder",
      "com.google.inject.AbstractModule",
      "com.google.inject.PrivateModule");

  /**
   * Adds an INJECTION_POINT entry for each {@code getProvider(Foo.class)} or {@code getProvider(Key.get(...))}
   * call in the methods of the module class. The gutter anchor is the {@code getProvider} identifier.
   */
  private static void extractGetProviderEntries(@NotNull PsiClass moduleClass, @NotNull Set<GuiceEntry> entries) {
    for (PsiMethod method : moduleClass.getMethods()) {
      UMethod uMethod = UastContextKt.toUElement(method, UMethod.class);
      if (uMethod == null) continue;
      uMethod.accept(new AbstractUastVisitor() {
        @Override
        public boolean visitClass(@NotNull UClass node) {
          // The recursion over the classes of the module handles a nested class.
          return true;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
          GuiceEntry entry = createGetProviderEntry(node);
          if (entry != null) entries.add(entry);
          return super.visitCallExpression(node);
        }
      });
    }
  }

  private static @Nullable GuiceEntry createGetProviderEntry(@NotNull UCallExpression call) {
    if (!"getProvider".equals(call.getMethodName()) || call.getValueArgumentCount() != 1) return null;
    PsiMethod resolved = call.resolve();
    PsiClass owner = resolved != null ? resolved.getContainingClass() : null;
    if (owner == null || !GET_PROVIDER_OWNERS.contains(owner.getQualifiedName())) return null;

    UExpression argument = call.getValueArguments().getFirst();
    PsiType type = GuiceUtils.getBindingTypeFromExpression(argument);
    if (type == null) return null;
    UCallExpression keyGet = GuiceQualifiers.asKeyGet(argument);
    GuiceQualifier qualifier = keyGet != null && keyGet.getValueArgumentCount() > 1
                               ? GuiceQualifiers.fromExpression(keyGet.getValueArguments().get(1))
                               : null;

    UIdentifier identifier = call.getMethodIdentifier();
    PsiElement anchor = identifier != null ? identifier.getSourcePsi() : null;
    PsiElement target = call.getSourcePsi();
    if (anchor == null || target == null) return null;
    return new GuiceEntry(new GuiceBindingKey(type, qualifier), target, anchor, EntryRole.INJECTION_POINT,
                          PsiElement::getText);
  }

  private static final List<String> BINDER_ELEMENT_CALLS = List.of("addBinding", "setDefault", "setBinding");

  /**
   * Unwraps {@code ? extends T} wildcards introduced by Kotlin declaration-site variance
   * (for example {@code Set<MyService>} becoming {@code java.util.Set<? extends MyService>} in light PSI).
   */
  private static @NotNull PsiType unwrapKotlinWildcards(@NotNull PsiType type) {
    if (type instanceof PsiWildcardType wildcardType && wildcardType.isExtends()) {
      return unwrapKotlinWildcards(wildcardType.getExtendsBound());
    }
    if (type instanceof PsiClassType classType) {
      PsiType[] params = classType.getParameters();
      if (params.length == 0) return type;
      PsiClass resolved = classType.resolve();
      if (resolved == null) return type;
      PsiType[] unwrapped = new PsiType[params.length];
      boolean changed = false;
      for (int i = 0; i < params.length; i++) {
        unwrapped[i] = unwrapKotlinWildcards(params[i]);
        if (unwrapped[i] != params[i]) changed = true;
      }
      if (!changed) return type;
      return JavaPsiFacade.getElementFactory(resolved.getProject()).createType(resolved, unwrapped);
    }
    return type;
  }

  private static boolean isBinderElementChain(@Nullable UCallExpression outermost) {
    if (outermost == null) return false;
    for (String name : BINDER_ELEMENT_CALLS) {
      if (GuiceUtils.findCallInChain(outermost, name) != null) return true;
    }
    return false;
  }

  // -----------------------------------------------------------------------
  // Presentable text for standard binding call chains
  // -----------------------------------------------------------------------

  private static final int MAX_BIND_ARG_LENGTH = 50;

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
      if (appendCallInChain(call, sb, "bind", true) || appendCallInChain(call, sb, "bindConstant", true)) {
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
      String rawText = sourcePsi != null ? sourcePsi.getText() : arg.toString();
      String normalized = rawText.replaceAll("\\s+", " ").trim();
      if (normalized.length() > MAX_BIND_ARG_LENGTH) {
        normalized = normalized.substring(0, MAX_BIND_ARG_LENGTH - 3) + "...";
      }
      sb.append(normalized);
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
}
