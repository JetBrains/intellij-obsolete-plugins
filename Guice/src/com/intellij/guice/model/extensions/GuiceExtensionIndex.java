// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Compiled multi-stage index of all registered {@link GuiceBindingContributor} rules.
 *
 * <p>Cached on {@link GuiceBindingContributor#EP_NAME} and invalidated automatically
 * whenever extensions are added or removed.
 */
@ApiStatus.Internal
public final class GuiceExtensionIndex {
  private static final Logger LOG = Logger.getInstance(GuiceExtensionIndex.class);

  /**
   * Re-throws IDE control-flow and cancellation exceptions so read-action cancellation works as expected.
   */
  public static void rethrowIfControlFlow(@NotNull Exception e) {
    if (e instanceof ControlFlowException || e instanceof CancellationException) {
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  private interface DescriptorCallHandler {
    boolean handle(@NotNull UCallExpression call,
                   @Nullable String resolvedOwnerQName,
                   @Nullable PsiClass containingClass,
                   @NotNull Set<BindDescriptor> descriptors);
  }

  private final @NotNull GuiceCallMatcher<DescriptorCallHandler> myDescriptorCallMatcher;
  private final @NotNull GuiceCallMatcher<BiConsumer<? super UCallExpression, ? super GuiceCallContext>> myCallEntryMatcher;
  private final @NotNull Set<String> myAllCallNames;
  private final @NotNull Set<String> myGlobalCallOwnerClasses;
  private final @NotNull Set<String> mySingleTypeBinderMethodNames;
  private final @NotNull Set<String> myDualTypeBinderMethodNames;
  private final @NotNull Set<String> mySingleTypeBinderClassFqns;
  private final @NotNull Set<String> myDualTypeBinderClassFqns;
  private final @NotNull Set<String> myAllProvidesAnnotations;
  private final @NotNull Set<String> myProvidesIntoAnnotations;
  private final @NotNull Set<String> mySupportedFieldAnnotations;
  private final @NotNull Map<String, List<Function<? super PsiMethod, ? extends List<PsiType>>>> myProvidesByAnnotation;
  private final @NotNull Set<String> myProvidesShortNames;

  private GuiceExtensionIndex(@NotNull GuiceCallMatcher<DescriptorCallHandler> descriptorCallMatcher,
                              @NotNull GuiceCallMatcher<BiConsumer<? super UCallExpression, ? super GuiceCallContext>> callEntryMatcher,
                              @NotNull Set<String> allCallNames,
                              @NotNull Set<String> globalCallOwnerClasses,
                              @NotNull Set<String> singleTypeBinderMethodNames,
                              @NotNull Set<String> dualTypeBinderMethodNames,
                              @NotNull Set<String> singleTypeBinderClassFqns,
                              @NotNull Set<String> dualTypeBinderClassFqns,
                              @NotNull Set<String> allProvidesAnnotations,
                              @NotNull Set<String> providesIntoAnnotations,
                              @NotNull Set<String> supportedFieldAnnotations,
                              @NotNull Map<String, List<Function<? super PsiMethod, ? extends List<PsiType>>>> providesByAnnotation) {
    myDescriptorCallMatcher = descriptorCallMatcher;
    myCallEntryMatcher = callEntryMatcher;
    myAllCallNames = allCallNames;
    myGlobalCallOwnerClasses = globalCallOwnerClasses;
    mySingleTypeBinderMethodNames = singleTypeBinderMethodNames;
    myDualTypeBinderMethodNames = dualTypeBinderMethodNames;
    mySingleTypeBinderClassFqns = singleTypeBinderClassFqns;
    myDualTypeBinderClassFqns = dualTypeBinderClassFqns;
    myAllProvidesAnnotations = allProvidesAnnotations;
    myProvidesIntoAnnotations = providesIntoAnnotations;
    mySupportedFieldAnnotations = supportedFieldAnnotations;
    myProvidesByAnnotation = providesByAnnotation;
    myProvidesShortNames = extractShortNames(providesByAnnotation.keySet());
  }

  private static @NotNull Set<String> extractShortNames(@NotNull Collection<String> fqns) {
    if (fqns.isEmpty()) return Set.of();
    Set<String> shortNames = new LinkedHashSet<>(fqns.size());
    for (String fqn : fqns) {
      shortNames.add(shortNameOf(fqn));
    }
    return Set.copyOf(shortNames);
  }

  private static @NotNull String shortNameOf(@NotNull String fqn) {
    int dot = fqn.lastIndexOf('.');
    return dot >= 0 ? fqn.substring(dot + 1) : fqn;
  }

  private static boolean matchesAnnotationShortName(@NotNull PsiAnnotation annotation, @NotNull Set<String> shortNames) {
    PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
    if (ref == null) return true;
    String refName = ref.getReferenceName();
    return refName == null || shortNames.contains(refName);
  }

  /**
   * Returns the current compiled extension index, building and caching it on {@link GuiceBindingContributor#EP_NAME}.
   */
  public static @NotNull GuiceExtensionIndex get() {
    return GuiceBindingContributor.EP_NAME.computeIfAbsent(GuiceExtensionIndex.class, GuiceExtensionIndex::build);
  }

  public @NotNull Set<String> getDescriptorCallNames() {
    return myDescriptorCallMatcher.getMethodNames();
  }

  public @NotNull Set<String> getAllCallNames() {
    return myAllCallNames;
  }

  public @NotNull Set<String> getGlobalCallOwnerClasses() {
    return myGlobalCallOwnerClasses;
  }

  public @NotNull Set<String> getSingleTypeBinderMethodNames() {
    return mySingleTypeBinderMethodNames;
  }

  public @NotNull Set<String> getDualTypeBinderMethodNames() {
    return myDualTypeBinderMethodNames;
  }

  public @NotNull Set<String> getSingleTypeBinderClassFqns() {
    return mySingleTypeBinderClassFqns;
  }

  public @NotNull Set<String> getDualTypeBinderClassFqns() {
    return myDualTypeBinderClassFqns;
  }

  public @NotNull Set<String> getAllProvidesAnnotations() {
    return myAllProvidesAnnotations;
  }

  public @NotNull Set<String> getProvidesIntoAnnotations() {
    return myProvidesIntoAnnotations;
  }

  public @NotNull Set<String> getSupportedFieldAnnotations() {
    return mySupportedFieldAnnotations;
  }

  /**
   * Dispatches a call expression inside a Guice module class through the multi-stage descriptor matcher.
   */
  public boolean processDescriptorCall(@NotNull UCallExpression call, @NotNull Set<BindDescriptor> descriptors) {
    return myDescriptorCallMatcher.process(call, descriptors, DescriptorCallHandler::handle);
  }

  /**
   * Processes call-based {@link GuiceEntry} rules on the methods of a class.
   */
  public void processCallEntries(@NotNull PsiClass psiClass, boolean isModule, @NotNull GuiceCallContext context) {
    if (myCallEntryMatcher.isEmpty() || (!isModule && !myCallEntryMatcher.hasGlobalRules())) {
      return;
    }
    for (PsiMethod method : psiClass.getMethods()) {
      if (!(method instanceof LightElement) && method.getBody() == null) continue;
      UMethod uMethod = UastContextKt.toUElement(method, UMethod.class);
      if (uMethod == null) continue;
      uMethod.accept(new AbstractUastVisitor() {
        @Override
        public boolean visitClass(@NotNull UClass node) {
          return true;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
          myCallEntryMatcher.process(node, isModule, context, (producer, callExpr, _qName, _cls, sink) -> {
            try {
              producer.accept(callExpr, sink);
              return true;
            }
            catch (Exception e) {
              rethrowIfControlFlow(e);
              LOG.warn("Guice call-entry contributor failed on " + callExpr, e);
              return false;
            }
          });
          return super.visitCallExpression(node);
        }
      });
    }
  }

  /**
   * Computes wrapped bound key types for a {@code @ProvidesInto*} or {@code @CheckedProvides} method
   * in a single pass over the method annotations.
   */
  public @NotNull List<PsiType> getWrappedProvidesTypes(@NotNull PsiMethod method) {
    if (myProvidesByAnnotation.isEmpty()) {
      return List.of();
    }
    for (PsiAnnotation annotation : method.getAnnotations()) {
      if (!matchesAnnotationShortName(annotation, myProvidesShortNames)) continue;
      String fqn = annotation.getQualifiedName();
      if (fqn == null) continue;
      List<Function<? super PsiMethod, ? extends List<PsiType>>> providers = myProvidesByAnnotation.get(fqn);
      if (providers != null) {
        for (Function<? super PsiMethod, ? extends List<PsiType>> provider : providers) {
          try {
            List<PsiType> types = provider.apply(method);
            if (types != null && !types.isEmpty()) {
              return types;
            }
          }
          catch (Exception e) {
            rethrowIfControlFlow(e);
            LOG.warn("Guice provides-annotation contributor failed on " + method, e);
          }
        }
        return List.of();
      }
    }
    return List.of();
  }

  private static @NotNull GuiceExtensionIndex build() {
    RegistrarImpl registrar = new RegistrarImpl();
    for (GuiceBindingContributor contributor : GuiceBindingContributor.EP_NAME.getExtensionList()) {
      try {
        contributor.register(registrar);
      }
      catch (Exception e) {
        rethrowIfControlFlow(e);
        LOG.warn("Failed to register GuiceBindingContributor: " + contributor.getClass().getName(), e);
      }
    }
    return registrar.compile();
  }

  private static final class RegistrarImpl implements GuiceExtensionRegistrar {
    private final GuiceCallMatcher.Builder<DescriptorCallHandler> myDescriptorCalls =
        GuiceCallMatcher.builder(true);
    private final GuiceCallMatcher.Builder<BiConsumer<? super UCallExpression, ? super GuiceCallContext>> myCallEntries =
        GuiceCallMatcher.builder(false);

    private final Set<String> myGlobalCallOwnerClasses = new LinkedHashSet<>();
    private final Set<String> mySingleTypeBinderMethods = new LinkedHashSet<>();
    private final Set<String> myDualTypeBinderMethods = new LinkedHashSet<>();
    private final Set<String> mySingleTypeBinderClasses = new LinkedHashSet<>(List.of(
        GuiceClasses.LINKED_BINDING_BUILDER,
        "com.google.inject.binder.AnnotatedBindingBuilder"
    ));
    private final Set<String> myDualTypeBinderClasses = new LinkedHashSet<>();
    private final Set<String> myBindingFieldAnnotations = new LinkedHashSet<>();

    private final Map<String, List<Function<? super PsiMethod, ? extends List<PsiType>>>> myProvidesByAnno = new HashMap<>();

    @Override
    public void registerSingleTypeBinder(@NotNull GuiceCallPattern pattern,
                                         @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory) {
      mySingleTypeBinderMethods.addAll(pattern.getMethodNames());
      mySingleTypeBinderClasses.addAll(pattern.getExactOwnerClasses());
      registerCallDescriptor(pattern, (call, descriptors) -> {
        PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
        if (outermostSource != null) {
          BindDescriptor descriptor = factory.apply(outermostSource, ContributorUtil.extractSinglePsiType(call));
          if (descriptor != null) {
            descriptors.add(descriptor);
            return true;
          }
        }
        return false;
      });
    }

    @Override
    public void registerDualTypeBinder(@NotNull GuiceCallPattern pattern,
                                       @NotNull ContributorUtil.DualTypeDescriptorFactory factory) {
      myDualTypeBinderMethods.addAll(pattern.getMethodNames());
      myDualTypeBinderClasses.addAll(pattern.getExactOwnerClasses());
      registerCallDescriptor(pattern, (call, descriptors) -> {
        PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
        if (outermostSource != null) {
          PsiType[] kv = ContributorUtil.extractDualPsiTypes(call);
          BindDescriptor descriptor = factory.create(outermostSource, kv[0], kv[1]);
          if (descriptor != null) {
            descriptors.add(descriptor);
            return true;
          }
        }
        return false;
      });
    }

    @Override
    public void registerCallDescriptor(@NotNull GuiceCallPattern pattern,
                                       @NotNull BiFunction<? super UCallExpression, ? super Set<BindDescriptor>, Boolean> handler) {
      myDescriptorCalls.add(pattern, (call, _qName, _cls, descriptors) -> {
        try {
          return Boolean.TRUE.equals(handler.apply(call, descriptors));
        }
        catch (Exception e) {
          rethrowIfControlFlow(e);
          LOG.warn("Guice call-descriptor contributor failed on " + call, e);
          return false;
        }
      });
    }

    @Override
    public void registerModuleCallEntry(@NotNull GuiceCallPattern pattern,
                                        @NotNull BiConsumer<? super UCallExpression, ? super Set<GuiceEntry>> producer) {
      myCallEntries.add(pattern, (call, context) -> producer.accept(call, context.getEntries()), true);
    }

    @Override
    public void registerCallWithContext(@NotNull GuiceCallPattern pattern,
                                        @NotNull BiConsumer<? super UCallExpression, ? super GuiceCallContext> producer) {
      myGlobalCallOwnerClasses.addAll(pattern.getExactOwnerClasses());
      myCallEntries.add(pattern, producer, false);
    }

    @Override
    public void registerProvidesAnnotation(@NotNull Collection<String> annotationFqns,
                                           @NotNull Function<? super PsiMethod, ? extends List<PsiType>> wrappedTypesProvider) {
      for (String fqn : annotationFqns) {
        myProvidesByAnno.computeIfAbsent(fqn, _k -> new ArrayList<>()).add(wrappedTypesProvider);
      }
    }

    @Override
    public void registerBindingFieldAnnotations(@NotNull Collection<String> annotationFqns) {
      myBindingFieldAnnotations.addAll(annotationFqns);
    }

    @NotNull GuiceExtensionIndex compile() {
      GuiceCallMatcher<DescriptorCallHandler> descriptorMatcher = myDescriptorCalls.build();
      GuiceCallMatcher<BiConsumer<? super UCallExpression, ? super GuiceCallContext>> callEntryMatcher = myCallEntries.build();

      Set<String> allCallNames = new LinkedHashSet<>();
      allCallNames.addAll(descriptorMatcher.getMethodNames());
      allCallNames.addAll(callEntryMatcher.getMethodNames());

      Set<String> providesInto = Set.copyOf(myProvidesByAnno.keySet());
      Set<String> allProvides = new LinkedHashSet<>(GuiceAnnotations.PROVIDES_ANNOTATIONS);
      allProvides.addAll(providesInto);

      return new GuiceExtensionIndex(
          descriptorMatcher,
          callEntryMatcher,
          Set.copyOf(allCallNames),
          Set.copyOf(myGlobalCallOwnerClasses),
          Set.copyOf(mySingleTypeBinderMethods),
          Set.copyOf(myDualTypeBinderMethods),
          Set.copyOf(mySingleTypeBinderClasses),
          Set.copyOf(myDualTypeBinderClasses),
          Set.copyOf(allProvides),
          providesInto,
          Set.copyOf(myBindingFieldAnnotations),
          immutableListMap(myProvidesByAnno)
      );
    }

    private static <K, V> @NotNull Map<K, List<V>> immutableListMap(@NotNull Map<K, List<V>> map) {
      if (map.isEmpty()) return Map.of();
      Map<K, List<V>> copy = new HashMap<>(map.size());
      for (Map.Entry<K, List<V>> entry : map.entrySet()) {
        copy.put(entry.getKey(), List.copyOf(entry.getValue()));
      }
      return Map.copyOf(copy);
    }
  }
}
