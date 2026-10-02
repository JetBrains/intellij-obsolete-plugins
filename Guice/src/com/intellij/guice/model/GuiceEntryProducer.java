// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.model.extensions.GuiceCallContext;
import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Produces {@link GuiceEntry} instances from PSI/UAST elements.
 *
 * <p>This is the <b>single source of truth</b> for what entries a given code element
 * contributes to the navigation index. All type resolution (Provider unwrapping,
 * multibinder unwrapping) happens here at creation time, not during matching.
 */
@ApiStatus.Experimental
public final class GuiceEntryProducer {
  private GuiceEntryProducer() {}

  /**
   * Result of extracting Guice navigation entries and cross-file target dependencies from a file.
   */
  @ApiStatus.Internal
  public record FileExtractionResult(
      @NotNull Set<GuiceEntry> entries,
      @NotNull Set<String> referencedTargetPaths,
      @NotNull Set<String> referencedTargetClassNames,
      @NotNull Set<VirtualFile> referencedTargetFiles
  ) {
    public static final FileExtractionResult EMPTY =
        new FileExtractionResult(Set.of(), Set.of(), Set.of(), Set.of());

    public FileExtractionResult(
        @NotNull Set<GuiceEntry> entries,
        @NotNull Set<String> referencedTargetPaths,
        @NotNull Set<String> referencedTargetClassNames
    ) {
      this(entries, referencedTargetPaths, referencedTargetClassNames, Set.of());
    }
  }

  /**
   * Extracts all {@link GuiceEntry} instances from the classes of a file.
   * This includes nested, local and anonymous classes, for example {@code install(new AbstractModule() {...})}.
   */
  @ApiStatus.Internal
  public static @NotNull Set<GuiceEntry> extractFromFile(@NotNull PsiFile file) {
    return extractFileData(file).entries();
  }

  /**
   * Extracts all {@link GuiceEntry} instances and cross-file target dependencies from a file.
   */
  @ApiStatus.Internal
  public static @NotNull FileExtractionResult extractFileData(@NotNull PsiFile file) {
    Set<GuiceEntry> entries = new HashSet<>();
    CallContextImpl callContext = new CallContextImpl(entries);
    for (PsiClass cls : GuiceInjectorManager.collectAllClasses(file)) {
      extractFromClass(cls, callContext);
    }
    return new FileExtractionResult(
        entries,
        callContext.getReferencedTargetPaths(),
        callContext.getReferencedTargetClassNames(),
        callContext.getReferencedTargetFiles()
    );
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
  private static void extractFromClass(@NotNull PsiClass cls, @NotNull CallContextImpl callContext) {
    Set<GuiceEntry> entries = callContext.getEntries();
    GuiceExtensionIndex extensionIndex = GuiceExtensionIndex.get();

    // @Inject fields → INJECTION_POINT, plus any contributor-registered field annotations
    for (PsiField field : cls.getFields()) {
      if (isInjectAnnotated(field)) {
        GuiceEntry entry = createInjectionPointEntry(field, field.getType());
        entries.add(entry);
      }
      extensionIndex.processFieldAnnotations(field, entries);
    }

    boolean isModule = InheritanceUtil.isInheritor(cls, "com.google.inject.Module");

    // Methods: @Inject methods (any class), @Provides methods (module classes only)
    for (PsiMethod method : cls.getMethods()) {
      // The constructor loop below handles constructors.
      if (method.isConstructor()) continue;
      boolean isInject = AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0);
      boolean isProvides = isModule && AnnotationUtil.isAnnotated(method,
          extensionIndex.getAllProvidesAnnotations(), 0);
      boolean isCustomAnnotated = !extensionIndex.getMethodAnnotations().isEmpty()
          && AnnotationUtil.isAnnotated(method, extensionIndex.getMethodAnnotations(), 0);

      if (isInject || isProvides) {
        // Parameters → INJECTION_POINT
        for (PsiParameter param : method.getParameterList().getParameters()) {
          if (GuiceQualifiers.isAssisted(param)) continue;
          GuiceEntry entry = createInjectionPointEntry(param, param.getType());
          entries.add(entry);
        }
      }

      if (isCustomAnnotated) {
        extensionIndex.processMethodAnnotations(method, entries);
      }

      // @Provides → BINDING_SITE for return type (only in Module classes)
      if (isProvides) {
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
          PsiElement anchor = resolveDeclarationAnchor(method);
          boolean isKotlinMethod = method.getNavigationElement() != method;
          if (AnnotationUtil.isAnnotated(method, extensionIndex.getProvidesIntoAnnotations(), 0)) {
            for (PsiType wrappedType : extensionIndex.getWrappedProvidesTypes(method)) {
              PsiType keyType = isKotlinMethod ? unwrapKotlinWildcards(wrappedType) : wrappedType;
              entries.add(new GuiceEntry(
                  new GuiceBindingKey(keyType, GuiceQualifiers.fromDeclaration(method)), method, anchor,
                  EntryRole.BINDING_SITE, GuiceEntryProducer::providesMethodText));
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
    extensionIndex.processClassAnnotations(cls, entries);

    // Binding calls in configure() for module classes
    if (isModule) {
      extractBindingCallEntries(cls, entries);
    }
    extensionIndex.processCallEntries(cls, isModule, callContext);
  }

  private static final class CallContextImpl implements GuiceCallContext {
    private final @NotNull Set<GuiceEntry> myEntries;
    private final @NotNull Set<String> myReferencedTargetPaths = new HashSet<>();
    private final @NotNull Set<String> myReferencedTargetClassNames = new HashSet<>();
    private final @NotNull Set<VirtualFile> myReferencedTargetFiles = new HashSet<>();

    CallContextImpl(@NotNull Set<GuiceEntry> entries) {
      myEntries = entries;
    }

    @Override
    public @NotNull Set<GuiceEntry> getEntries() {
      return myEntries;
    }

    @Override
    public void reportClassBindings(
        @NotNull PsiClass targetClass,
        @NotNull BiConsumer<? super PsiClass, ? super Set<GuiceEntry>> classExtractor
    ) {
      PsiElement navElement = targetClass.getNavigationElement();
      PsiFile targetFile = navElement != null ? navElement.getContainingFile() : null;
      if (targetFile == null) {
        targetFile = targetClass.getContainingFile();
      }
      if (targetFile != null && targetFile.getVirtualFile() != null) {
        VirtualFile targetVf = targetFile.getVirtualFile();
        myReferencedTargetPaths.add(targetVf.getPath());
        myReferencedTargetFiles.add(targetVf);
      }
      String name = targetClass.getName();
      if (name != null && !name.isEmpty()) {
        myReferencedTargetClassNames.add(name);
      }
      PsiClass topLevel = PsiUtil.getTopLevelClass(targetClass);
      if (topLevel != null && topLevel.getName() != null && !topLevel.getName().isEmpty()) {
        myReferencedTargetClassNames.add(topLevel.getName());
      }
      classExtractor.accept(targetClass, myEntries);
    }

    @Override
    public @Nullable PsiClass reportClassArgument(
        @Nullable UExpression classLiteralArgument,
        @NotNull BiConsumer<? super PsiClass, ? super Set<GuiceEntry>> classExtractor
    ) {
      if (classLiteralArgument == null) return null;
      PsiType type = GuiceUtils.getBindingTypeFromExpression(classLiteralArgument);
      if (type instanceof PsiClassType classType) {
        PsiClass resolved = classType.resolve();
        if (resolved != null) {
          reportClassBindings(resolved, classExtractor);
          return resolved;
        }
        String className = classType.getClassName();
        if (className != null && !className.isEmpty()) {
          myReferencedTargetClassNames.add(className);
        }
      }
      recordCandidateClassNames(classLiteralArgument);
      return null;
    }

    private void recordCandidateClassNames(@NotNull UExpression expr) {
      PsiElement sourcePsi = expr.getSourcePsi();
      if (sourcePsi == null) return;
      String text = sourcePsi.getText();
      if (text == null || text.isEmpty()) return;
      int classSuffix = text.lastIndexOf("::class");
      if (classSuffix < 0) {
        classSuffix = text.lastIndexOf(".class");
      }
      if (classSuffix > 0) {
        text = text.substring(0, classSuffix);
      }
      for (String segment : text.split("\\.")) {
        String trimmed = segment.trim();
        if (!trimmed.isEmpty() && Character.isJavaIdentifierStart(trimmed.charAt(0))) {
          myReferencedTargetClassNames.add(trimmed);
        }
      }
    }

    @NotNull Set<String> getReferencedTargetPaths() {
      return myReferencedTargetPaths.isEmpty() ? Set.of() : Set.copyOf(myReferencedTargetPaths);
    }

    @NotNull Set<String> getReferencedTargetClassNames() {
      return myReferencedTargetClassNames.isEmpty() ? Set.of() : Set.copyOf(myReferencedTargetClassNames);
    }

    @NotNull Set<VirtualFile> getReferencedTargetFiles() {
      return myReferencedTargetFiles.isEmpty() ? Set.of() : Set.copyOf(myReferencedTargetFiles);
    }
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
  public static @NotNull GuiceEntry createInjectionPointEntry(
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
   * Creates a BINDING_SITE entry for a field declaration (such as an {@code @FlagSpec} field)
   * using the field's declared qualifier annotation.
   */
  public static @NotNull GuiceEntry createFieldBindingEntry(
      @NotNull PsiField field, @NotNull PsiType boundType) {
    return createFieldBindingEntry(field, boundType, GuiceQualifiers.fromDeclaration(field));
  }

  /**
   * Creates a BINDING_SITE entry for a field declaration with an explicit {@link GuiceQualifier}.
   */
  public static @NotNull GuiceEntry createFieldBindingEntry(
      @NotNull PsiField field, @NotNull PsiType boundType, @Nullable GuiceQualifier qualifier) {
    PsiType normalizedType = field.getNavigationElement() != field
                             ? unwrapKotlinWildcards(boundType)
                             : boundType;
    PsiElement anchor = resolveDeclarationAnchor(field);
    return new GuiceEntry(
        new GuiceBindingKey(normalizedType, qualifier),
        field, anchor, EntryRole.BINDING_SITE,
        GuiceEntryProducer::injectionPointText);
  }

  /**
   * Resolves the identifier leaf anchor for a method or constructor call expression.
   */
  public static @Nullable PsiElement getCallAnchor(@NotNull UCallExpression call) {
    UIdentifier identifier = call.getMethodIdentifier();
    if (identifier != null) {
      PsiElement psi = identifier.getSourcePsi();
      if (psi != null) return psi;
    }
    UReferenceExpression classRef = call.getClassReference();
    if (classRef != null) {
      PsiElement sourcePsi = classRef.getSourcePsi();
      if (sourcePsi instanceof PsiJavaCodeReferenceElement javaRef) {
        PsiElement nameElement = javaRef.getReferenceNameElement();
        if (nameElement != null) return nameElement;
      }
      return sourcePsi;
    }
    return null;
  }

  /**
   * Creates a call-site {@link GuiceEntry} anchored on the call's method or constructor identifier.
   */
  public static @Nullable GuiceEntry createCallEntry(@NotNull UCallExpression call,
                                                     @NotNull PsiType type,
                                                     @Nullable GuiceQualifier qualifier,
                                                     @NotNull EntryRole role) {
    PsiElement anchor = getCallAnchor(call);
    PsiElement target = call.getSourcePsi();
    if (anchor == null || target == null) return null;
    return new GuiceEntry(new GuiceBindingKey(type, qualifier), target, anchor, role, PsiElement::getText);
  }

  /**
   * Creates an implementation-reference {@link GuiceEntry} for a class referenced by a binder call
   * (so the class-level gutter on {@code referencedClass} navigates to {@code call}).
   */
  @ApiStatus.Experimental
  public static @Nullable GuiceEntry createCallImplementationReferenceEntry(@NotNull UCallExpression call,
                                                                            @NotNull PsiClass referencedClass) {
    PsiElement anchor = getCallAnchor(call);
    PsiElement target = call.getSourcePsi();
    if (anchor == null || target == null) return null;
    return GuiceEntry.implementationReference(
        GuiceBindingKey.forClass(referencedClass), target, anchor, PsiElement::getText);
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

    for (BindDescriptor bd : descriptors) {
      PsiElement bindExpr = bd.getBindExpression();
      if (bindExpr == null) continue;

      PsiElement anchor = getBindingAnchor(bindExpr);

      if (bd.isSpecialBinder()) {
        UCallExpression outermostCall = bd.getOutermostCall();
        GuiceQualifier qualifier = outermostCall != null ? GuiceQualifiers.fromBinderCall(outermostCall) : null;
        for (PsiType wrappedType : bd.getWrappedBoundTypes()) {
          entries.add(new GuiceEntry(
              new GuiceBindingKey(wrappedType, qualifier), bindExpr, anchor, EntryRole.BINDING_SITE,
              bd.getTextProvider()));
        }
        continue;
      }

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
  }

  /**
   * Adds an INJECTION_POINT entry for a {@code getProvider(Foo.class)} or {@code getProvider(Key.get(...))}
   * call inside a module class.
   */
  public static void addGetProviderEntry(@NotNull UCallExpression call, @NotNull Set<GuiceEntry> entries) {
    List<UExpression> valueArgs = call.getValueArguments();
    if (valueArgs.isEmpty()) return;
    UExpression argument = valueArgs.getFirst();
    PsiType type = GuiceUtils.getBindingTypeFromExpression(argument);
    if (type == null) return;
    UCallExpression keyGet = GuiceQualifiers.asKeyGet(argument);
    GuiceQualifier qualifier = keyGet != null && keyGet.getValueArgumentCount() > 1
                               ? GuiceQualifiers.fromExpression(keyGet.getValueArguments().get(1))
                               : null;
    GuiceEntry entry = createCallEntry(call, type, qualifier, EntryRole.INJECTION_POINT);
    if (entry != null) {
      entries.add(entry);
    }
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
   * <p>Only standard {@code bind()} chains are handled. For special binders
   * (MapBinder, SetBinder, OptionalBinder), see {@link BindDescriptor#getTextProvider()}.
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
