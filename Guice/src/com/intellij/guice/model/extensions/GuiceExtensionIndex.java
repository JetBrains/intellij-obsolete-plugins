// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.diagnostic.PluginException;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.util.ExceptionUtil;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UExpression;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Compiled multi-stage index of all registered {@link GuiceBindingContributor} rules.
 *
 * <p>Cached on {@link GuiceBindingContributor#EP_NAME} and invalidated automatically
 * whenever extensions are added or removed.
 *
 * <p>A failure of contributor code is reported once per contributor class through
 * {@link #reportContributorFailure}, attributed to the plugin of that class.
 */
@ApiStatus.Internal
public final class GuiceExtensionIndex {
  private static final Logger LOG = Logger.getInstance(GuiceExtensionIndex.class);

  /**
   * Re-throws IDE control-flow and cancellation exceptions so read-action cancellation works as expected.
   */
  public static void rethrowIfControlFlow(@NotNull Throwable e) {
    if (Logger.shouldRethrow(e)) {
      ExceptionUtil.rethrow(e);
    }
  }

  /**
   * Reports failures of contributor code: the first failure of a class as a {@link PluginException}
   * attributed to the plugin that owns the class, later failures of the same class at debug level.
   * The index holds one reporter, so the set of reported classes resets when the index is rebuilt.
   */
  static final class FailureReporter {
    private final Set<Class<?>> myReportedClasses = ConcurrentHashMap.newKeySet();

    void report(@NotNull Class<?> failingClass, @NotNull String what, @NotNull Throwable e) {
      // A built-in class, for example a descriptor of this plugin, is a bug of this plugin, not of a contributor.
      boolean builtIn = failingClass.getClassLoader() == GuiceExtensionIndex.class.getClassLoader();
      String message = (builtIn ? "Guice plugin class " : "Guice contributor ") + failingClass.getName() + " failed: " + what;
      if (myReportedClasses.add(failingClass)) {
        PluginException.logPluginError(LOG, message + ". Further failures of this class are logged at debug level.",
                                       e, failingClass);
      }
      else if (LOG.isDebugEnabled()) {
        LOG.debug(message, e);
      }
    }
  }

  @FunctionalInterface
  private interface DescriptorCallHandler {
    boolean handle(@NotNull UCallExpression call,
                   @Nullable String resolvedOwnerQName,
                   @Nullable PsiClass containingClass,
                   @NotNull Set<BindDescriptor> descriptors);
  }

  private final @NotNull FailureReporter myFailureReporter;
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

  private GuiceExtensionIndex(@NotNull FailureReporter failureReporter,
                              @NotNull GuiceCallMatcher<DescriptorCallHandler> descriptorCallMatcher,
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
    myFailureReporter = failureReporter;
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

  /**
   * Reports a failure of contributor code that runs outside the registered callbacks, for example a method of a
   * custom {@link BindDescriptor} or a text provider of a {@link GuiceEntry}. See {@link FailureReporter}.
   *
   * @param contributorClass the class of the failing object; its plugin is blamed
   * @param what             a short description of the failing operation
   */
  public void reportContributorFailure(@NotNull Class<?> contributorClass, @NotNull String what, @NotNull Throwable e) {
    myFailureReporter.report(contributorClass, what, e);
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
  public void processDescriptorCall(@NotNull UCallExpression call, @NotNull Set<BindDescriptor> descriptors) {
    myDescriptorCallMatcher.process(call, descriptors, DescriptorCallHandler::handle);
  }

  /**
   * Runs the call rules on every call expression of a class in one UAST walk: the declarations of the class
   * (fields with their initializers, initializer blocks, methods) without its nested classes.
   *
   * @param descriptors the sink for {@link BindDescriptor}s of binder calls, or {@code null} to skip the descriptor
   *                    rules; only a module class has descriptors
   * @param context     the context for call-entry rules, or {@code null} to skip them
   */
  public void processClassCalls(@NotNull PsiClass psiClass,
                                boolean isModule,
                                @Nullable Set<BindDescriptor> descriptors,
                                @Nullable GuiceCallContext context) {
    boolean runDescriptors = descriptors != null && isModule;
    boolean runCallEntries = context != null
                             && !myCallEntryMatcher.isEmpty()
                             && (isModule || myCallEntryMatcher.hasGlobalRules());
    if (!runDescriptors && !runCallEntries) return;

    UClass uClass = UastContextKt.toUElement(psiClass, UClass.class);
    if (uClass == null) return;

    AbstractUastVisitor visitor = new AbstractUastVisitor() {
      @Override
      public boolean visitClass(@NotNull UClass node) {
        // Nested, local and anonymous classes are processed as classes of their own.
        return true;
      }

      @Override
      public boolean visitCallExpression(@NotNull UCallExpression node) {
        if (runDescriptors) {
          processDescriptorCall(node, descriptors);
        }
        if (runCallEntries) {
          // The producers are wrapped at registration time, so a failure is reported there with its contributor class.
          myCallEntryMatcher.process(node, isModule, context, (producer, callExpr, _qName, _cls, sink) -> {
            producer.accept(callExpr, sink);
            return true;
          });
        }
        return false;
      }
    };
    for (UElement declaration : uClass.getUastDeclarations()) {
      declaration.accept(visitor);
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
        // The providers are wrapped at registration time: a failing provider returns an empty list.
        for (Function<? super PsiMethod, ? extends List<PsiType>> provider : providers) {
          List<PsiType> types = provider.apply(method);
          if (types != null && !types.isEmpty()) {
            return types;
          }
        }
        return List.of();
      }
    }
    return List.of();
  }

  private static @NotNull GuiceExtensionIndex build() {
    FailureReporter reporter = new FailureReporter();
    RegistrarImpl registrar = new RegistrarImpl(reporter);
    for (GuiceBindingContributor contributor : GuiceBindingContributor.EP_NAME.getExtensionList()) {
      registrar.setCurrentContributor(contributor.getClass());
      try {
        contributor.register(registrar);
      }
      catch (Throwable t) {
        rethrowIfControlFlow(t);
        reporter.report(contributor.getClass(), "register()", t);
      }
    }
    return registrar.compile();
  }

  /**
   * A context that holds the entries of one producer until the producer returns normally. The dependency
   * bookkeeping of {@link GuiceCallContext#reportClassBindings} goes to the real context at once: a producer
   * that fails has still read the class, so the file must be re-indexed when that class changes.
   */
  private static final class BufferingCallContext implements GuiceCallContext {
    private final @NotNull GuiceCallContext myDelegate;
    private final @NotNull List<GuiceEntry> myBuffer = new ArrayList<>();

    BufferingCallContext(@NotNull GuiceCallContext delegate) {
      myDelegate = delegate;
    }

    @Override
    public void addEntry(@NotNull GuiceEntry entry) {
      myBuffer.add(entry);
    }

    @Override
    public void reportClassBindings(@NotNull PsiClass targetClass, @NotNull ClassEntryExtractor classExtractor) {
      myDelegate.reportClassBindings(targetClass, (cls, _sink) -> classExtractor.extract(cls, myBuffer::add));
    }

    @Override
    public @Nullable PsiClass reportClassArgument(@Nullable UExpression classLiteralArgument,
                                                  @NotNull ClassEntryExtractor classExtractor) {
      return myDelegate.reportClassArgument(classLiteralArgument, (cls, _sink) -> classExtractor.extract(cls, myBuffer::add));
    }

    void flush() {
      myBuffer.forEach(myDelegate::addEntry);
    }
  }

  private static final class RegistrarImpl implements GuiceExtensionRegistrar {
    private final @NotNull FailureReporter myReporter;
    private @NotNull Class<?> myCurrentContributor = GuiceBindingContributor.class;

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

    RegistrarImpl(@NotNull FailureReporter reporter) {
      myReporter = reporter;
    }

    /**
     * Sets the contributor whose {@code register()} runs next. The wrappers of the registered callbacks
     * capture it, so a later failure of a callback is attributed to the right plugin.
     */
    void setCurrentContributor(@NotNull Class<?> contributorClass) {
      myCurrentContributor = contributorClass;
    }

    /**
     * Wraps a callback of the current contributor: the callback cannot break the built-in Guice features
     * or other contributors, and its failures are reported through {@link FailureReporter}.
     */
    private @NotNull <T, R> Function<T, R> guard(@NotNull String what,
                                                 @NotNull Function<? super T, ? extends R> callback,
                                                 R fallback) {
      Class<?> contributorClass = myCurrentContributor;
      FailureReporter reporter = myReporter;
      return argument -> {
        try {
          return callback.apply(argument);
        }
        catch (Throwable t) {
          rethrowIfControlFlow(t);
          reporter.report(contributorClass, what + " on " + argument, t);
          return fallback;
        }
      };
    }

    @Override
    public void registerSingleTypeBinder(@NotNull GuiceCallPattern pattern,
                                         @NotNull Collection<String> binderClassFqns,
                                         @NotNull Function<? super UCallExpression, ? extends PsiType> typeExtractor,
                                         @NotNull BiFunction<? super PsiElement, ? super PsiType, ? extends BindDescriptor> factory) {
      mySingleTypeBinderMethods.addAll(pattern.getMethodNames());
      mySingleTypeBinderClasses.addAll(binderClassFqns);
      registerCallDescriptor(pattern, (call, sink) -> {
        PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
        if (outermostSource != null) {
          BindDescriptor descriptor = factory.apply(outermostSource, typeExtractor.apply(call));
          if (descriptor != null) {
            sink.accept(descriptor);
            return true;
          }
        }
        return false;
      });
    }

    @Override
    public void registerDualTypeBinder(@NotNull GuiceCallPattern pattern,
                                       @NotNull Collection<String> binderClassFqns,
                                       @NotNull Function<? super UCallExpression, PsiType @NotNull []> typesExtractor,
                                       @NotNull ContributorUtil.DualTypeDescriptorFactory factory) {
      myDualTypeBinderMethods.addAll(pattern.getMethodNames());
      myDualTypeBinderClasses.addAll(binderClassFqns);
      registerCallDescriptor(pattern, (call, sink) -> {
        PsiElement outermostSource = ContributorUtil.getOutermostSource(call);
        if (outermostSource != null) {
          PsiType[] kv = typesExtractor.apply(call);
          BindDescriptor descriptor = factory.create(outermostSource, kv.length > 0 ? kv[0] : null, kv.length > 1 ? kv[1] : null);
          sink.accept(descriptor);
          return true;
        }
        return false;
      });
    }

    @Override
    public void registerCallDescriptor(@NotNull GuiceCallPattern pattern, @NotNull CallDescriptorHandler handler) {
      Class<?> contributorClass = myCurrentContributor;
      FailureReporter reporter = myReporter;
      myDescriptorCalls.add(pattern, (call, _qName, _cls, descriptors) -> {
        // The handler fills a buffer; the index takes it only after a normal return.
        List<BindDescriptor> buffer = new ArrayList<>();
        try {
          boolean handled = handler.handle(call, buffer::add);
          descriptors.addAll(buffer);
          return handled;
        }
        catch (Throwable t) {
          rethrowIfControlFlow(t);
          reporter.report(contributorClass, "call-descriptor handler on " + call, t);
          return false;
        }
      });
    }

    @Override
    public void registerModuleCallEntry(@NotNull GuiceCallPattern pattern, @NotNull ModuleCallEntryProducer producer) {
      Class<?> contributorClass = myCurrentContributor;
      FailureReporter reporter = myReporter;
      myCallEntries.add(pattern, (call, context) -> {
        List<GuiceEntry> buffer = new ArrayList<>();
        try {
          producer.produce(call, buffer::add);
          buffer.forEach(context::addEntry);
        }
        catch (Throwable t) {
          rethrowIfControlFlow(t);
          reporter.report(contributorClass, "module call-entry producer on " + call, t);
        }
      }, true);
    }

    @Override
    public void registerCallWithContext(@NotNull GuiceCallPattern pattern,
                                        @NotNull BiConsumer<? super UCallExpression, ? super GuiceCallContext> producer) {
      myGlobalCallOwnerClasses.addAll(pattern.getExactOwnerClasses());
      Class<?> contributorClass = myCurrentContributor;
      FailureReporter reporter = myReporter;
      myCallEntries.add(pattern, (call, context) -> {
        BufferingCallContext buffering = new BufferingCallContext(context);
        try {
          producer.accept(call, buffering);
          buffering.flush();
        }
        catch (Throwable t) {
          rethrowIfControlFlow(t);
          reporter.report(contributorClass, "call-entry producer on " + call, t);
        }
      }, false);
    }

    @Override
    public void registerProvidesAnnotation(@NotNull Collection<String> annotationFqns,
                                           @NotNull Function<? super PsiMethod, ? extends List<PsiType>> wrappedTypesProvider) {
      Function<PsiMethod, List<PsiType>> guarded = guard("provides-annotation provider", wrappedTypesProvider, List.of());
      for (String fqn : annotationFqns) {
        myProvidesByAnno.computeIfAbsent(fqn, _k -> new ArrayList<>()).add(guarded);
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
          myReporter,
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
