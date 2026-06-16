// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToConstructorDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.beans.AssistedFactoryBindDescriptor;
import com.intellij.guice.model.beans.BindToInstanceDescriptor;
import com.intellij.guice.model.beans.BindToProviderDescriptor;
import com.intellij.guice.model.beans.MapMultibindDescriptor;
import com.intellij.guice.model.beans.MultimapBindDescriptor;
import com.intellij.guice.model.beans.OptionalBindDescriptor;
import com.intellij.guice.model.beans.SetMultibindDescriptor;
import com.intellij.guice.model.beans.UntargetedBindDescriptor;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.InheritanceUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GuiceInjectorManager {

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(final @NotNull PsiElement scope) {
    return CachedValuesManager.getCachedValue(scope, new ElementBindingsProvider(scope));
  }

  private static class ElementBindingsProvider implements CachedValueProvider<Set<BindDescriptor>> {
    private final PsiElement myScope;

    ElementBindingsProvider(PsiElement scope) {
      myScope = scope;
    }

    @Override
    public Result<Set<BindDescriptor>> compute() {
      return Result.create(getBindingDescriptors(myScope.getProject(), new LocalSearchScope(myScope)), myScope);
    }
  }

  private static final Set<String> ALL_BINDING_WORDS = ContainerUtil.newHashSet(
    "to", "toInstance", "toProvider", "toConstructor", "bind",
    "newOptionalBinder", "optionalBinder",
    "newSetBinder", "setBinder",
    "newMapBinder", "mapBinder",
    "newSetMultimapBinder", "multimapBinder",
    "build"
  );

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    Set<BindDescriptor> descriptors = new HashSet<>();
    final Set<PsiFile> files = getFilesToProcess(project, scope);
    for (PsiFile file : files) {
      descriptors.addAll(getBindingsInFile(file));
    }
    return descriptors;
  }

  private static @NotNull Set<PsiFile> getFilesToProcess(@NotNull Project project, @NotNull SearchScope scope) {
    final Set<PsiFile> files = new HashSet<>();
    if (scope instanceof GlobalSearchScope) {
      final PsiSearchHelper helper = PsiSearchHelper.getInstance(project);
      for (String word : ALL_BINDING_WORDS) {
        helper.processAllFilesWithWord(word, (GlobalSearchScope)scope, file -> {
          files.add(file);
          return true;
        }, true);
      }
    } else if (scope instanceof LocalSearchScope) {
      for (PsiElement element : ((LocalSearchScope)scope).getScope()) {
        final PsiFile file = element.getContainingFile();
        if (file != null) {
          files.add(file);
        }
      }
    }
    return files;
  }

  public static @NotNull Set<BindDescriptor> getBindingsInFile(@NotNull PsiFile file) {
    // Compiled class files (e.g., from hjars) cannot be walked with PsiRecursiveElementWalkingVisitor
    // (getNextSibling() is too slow) and don't contain method bodies.
    // However, if the library has attached sources (source jars), we can use the source file instead.
    if (file instanceof ClsFileImpl clsFile) {
      PsiElement sourceElement = clsFile.getNavigationElement();
      if (!(sourceElement instanceof PsiFile sourceFile) || sourceFile == clsFile) {
        return Set.of();
      }
      file = sourceFile;
    }
    final PsiFile fileToWalk = file;
    return CachedValuesManager.getCachedValue(fileToWalk, () -> {
      final Set<BindDescriptor> descriptors = new HashSet<>();
      // Collect all Guice module classes in this file, then traverse only those
      // using a UAST visitor.  This is language-agnostic (Java + Kotlin), avoids
      // scanning non-module classes, and skips inner classes during traversal
      // (since each inner module is visited separately from the collected list).
      List<PsiClass> guiceModules = collectGuiceModuleClasses(fileToWalk);
      if (!guiceModules.isEmpty()) {
        AbstractUastVisitor visitor = new AbstractUastVisitor() {
          @Override
          public boolean visitClass(@NotNull UClass node) {
            // The top-level accept() call already targets a UClass, so any
            // UClass encountered during child traversal is an inner class.
            // Skip it here — it will be visited separately from guiceModules
            // if it is itself a Guice module.
            return true;
          }

          @Override
          public boolean visitField(@NotNull UField node) {
            return true; // fields never contain binding calls
          }

          @Override
          public boolean visitCallExpression(@NotNull UCallExpression call) {
            final String callName = call.getMethodName();
            if (ALL_BINDING_WORDS.contains(callName)) {
              final PsiMethod resolved = call.resolve();
              if (resolved != null) {
                final PsiClass containingClass = resolved.getContainingClass();
                if (containingClass != null) {
                  final String qName = containingClass.getQualifiedName();
                  if (qName != null) {
                    createDescriptorIfMatches(call, callName, qName, containingClass, descriptors);
                  }
                }
              } else {
                // Fallback for unresolved calls: when the code is incomplete or
                // references a non-existent class (e.g., bind(X.class).to(DoNotExist.class)),
                // call.resolve() returns null.  Since we are already inside a verified Guice
                // module class, we can safely create a descriptor based on the method name.
                // The BindToDescriptor/etc. gracefully handle null binding classes.
                createDescriptorForUnresolvedCall(call, callName, descriptors);
              }
            }
            return false; // continue into children for chained calls
          }
        };
        for (PsiClass moduleClass : guiceModules) {
          UClass uClass = UastContextKt.toUElement(moduleClass, UClass.class);
          if (uClass != null) {
            // Visit methods directly — the visitClass override above prevents
            // descending into inner classes.
            for (UElement declaration : uClass.getUastDeclarations()) {
              declaration.accept(visitor);
            }
          }
        }
      }
      return CachedValueProvider.Result.create(descriptors, fileToWalk);
    });
  }

  /**
   * Collects <em>all</em> Guice module classes in the file, including nested ones.
   * Each module class is traversed separately with inner-class skipping in the visitor,
   * so there is no double-walking.
   *
   * <p>Works for both Java ({@link PsiClass}) and Kotlin ({@code KtLightClass}) since
   * {@link PsiClassOwner#getClasses()} returns light classes for Kotlin files, and
   * {@link InheritanceUtil#isInheritor(PsiClass, String)} handles both.
   */
  static @NotNull List<PsiClass> collectGuiceModuleClasses(@NotNull PsiFile file) {
    if (!(file instanceof PsiClassOwner classOwner)) return List.of();

    List<PsiClass> result = new ArrayList<>();
    for (PsiClass aClass : classOwner.getClasses()) {
      collectAllGuiceModules(aClass, result);
    }
    return result;
  }

  private static void collectAllGuiceModules(@NotNull PsiClass aClass,
                                             @NotNull List<PsiClass> result) {
    if (InheritanceUtil.isInheritor(aClass, "com.google.inject.Module")) {
      result.add(aClass);
    }
    // Always recurse into inner classes — each module is visited independently.
    for (PsiClass inner : aClass.getInnerClasses()) {
      collectAllGuiceModules(inner, result);
    }
  }

  /**
   * Checks whether a fully qualified class name belongs to a Guice package.
   * Used as a relaxed fallback when exact class matching doesn't cover all
   * Guice internal builder classes.
   */
  private static boolean isGuicePackage(@NotNull String qName) {
    return qName.startsWith("com.google.inject") || qName.startsWith("com.google.common.inject");
  }

  /**
   * Tries to create a binding-builder "tail" descriptor ({@code .to()}, {@code .toInstance()},
   * {@code .toProvider()}, {@code .toConstructor()}) from the given method name.
   *
   * @return {@code true} if a descriptor was created, {@code false} otherwise
   */
  private static boolean createBindingTailDescriptor(@NotNull String methodName,
                                                     @NotNull PsiElement outermostSource,
                                                     @NotNull Set<BindDescriptor> descriptors) {
    switch (methodName) {
      case "to" -> descriptors.add(new BindToDescriptor(outermostSource));
      case "toInstance" -> descriptors.add(new BindToInstanceDescriptor(outermostSource));
      case "toProvider" -> descriptors.add(new BindToProviderDescriptor(outermostSource));
      case "toConstructor" -> descriptors.add(new BindToConstructorDescriptor(outermostSource));
      default -> { return false; }
    }
    return true;
  }

  /**
   * Extracts a single type argument from a factory/binder call expression.
   * Tries explicit type arguments first, then falls back to the second value argument
   * (the first is typically the binder/module reference).
   *
   * @return the resolved {@link PsiClass}, or {@code null} if unavailable
   */
  private static @Nullable PsiClass extractSingleTypeArg(@NotNull UCallExpression call) {
    PsiType type = null;
    List<PsiType> typeArgs = call.getTypeArguments();
    if (!typeArgs.isEmpty()) {
      type = typeArgs.getFirst();
    } else {
      List<UExpression> args = call.getValueArguments();
      if (args.size() > 1) {
        type = GuiceUtils.getBindingTypeFromExpression(args.get(1));
      }
    }
    return type instanceof PsiClassType ct ? ct.resolve() : null;
  }

  /**
   * Extracts a key–value type argument pair from a MapBinder/MultimapBinder call expression.
   * Tries explicit type arguments first, then falls back to value arguments at indices 1 and 2.
   *
   * @return a two-element array {@code [keyClass, valClass]}; either element may be {@code null}
   */
  private static PsiClass @NotNull [] extractDualTypeArgs(@NotNull UCallExpression call) {
    PsiType keyType = null;
    PsiType valType = null;
    List<PsiType> typeArgs = call.getTypeArguments();
    if (typeArgs.size() > 1) {
      keyType = typeArgs.get(0);
      valType = typeArgs.get(1);
    } else {
      List<UExpression> args = call.getValueArguments();
      if (args.size() > 2) {
        keyType = GuiceUtils.getBindingTypeFromExpression(args.get(1));
        valType = GuiceUtils.getBindingTypeFromExpression(args.get(2));
      }
    }
    PsiClass keyClass = keyType instanceof PsiClassType kct ? kct.resolve() : null;
    PsiClass valClass = valType instanceof PsiClassType vct ? vct.resolve() : null;
    return new PsiClass[]{keyClass, valClass};
  }

  /**
   * Fallback descriptor creation for calls that cannot be resolved (the code is incomplete
   * or references a non-existent class).  Since we are already inside a verified Guice
   * module class, we can match on the method name text alone.
   *
   * <p>Only handles the binding-builder "tail" methods ({@code to}, {@code toInstance},
   * {@code toProvider}, {@code toConstructor}).  The root {@code bind()} call itself
   * typically resolves even when the argument class doesn't exist (because
   * {@code Binder.bind(Class)} is on the classpath), so it's handled by the normal path.
   *
   * @param call        the unresolved UAST call expression
   * @param methodName  the method name (already checked to be in {@link #ALL_BINDING_WORDS})
   * @param descriptors the output set to add descriptors to
   */
  private static void createDescriptorForUnresolvedCall(@NotNull UCallExpression call,
                                                         @NotNull String methodName,
                                                         @NotNull Set<BindDescriptor> descriptors) {
    PsiElement outermostSource = getOutermostSource(call);
    if (outermostSource != null) {
      createBindingTailDescriptor(methodName, outermostSource, descriptors);
    }
  }

  private static void createDescriptorIfMatches(@NotNull UCallExpression call,
                                                @NotNull String methodName,
                                                @NotNull String qName,
                                                @NotNull PsiClass containingClass,
                                                @NotNull Set<BindDescriptor> descriptors) {
    final PsiElement sourcePsi = call.getSourcePsi();
    if (sourcePsi == null) return;

    // Binding-builder tail methods: .to(), .toInstance(), .toProvider(), .toConstructor()
    if (GuiceClasses.LINKED_BINDING_BUILDER.equals(qName) ||
        InheritanceUtil.isInheritor(containingClass, GuiceClasses.LINKED_BINDING_BUILDER) ||
        isGuicePackage(qName)) {

      PsiElement outermostSource = getOutermostSource(call);
      if (outermostSource != null) {
        createBindingTailDescriptor(methodName, outermostSource, descriptors);
      }
    }

    // Untargeted bind()
    if ("bind".equals(methodName) &&
        ("com.google.inject.Binder".equals(qName) ||
         "com.google.inject.AbstractModule".equals(qName) ||
         "com.google.inject.PrivateModule".equals(qName) ||
         InheritanceUtil.isInheritor(containingClass, "com.google.inject.Binder") ||
         InheritanceUtil.isInheritor(containingClass, "com.google.inject.AbstractModule") ||
         isGuicePackage(qName))) {

      UCallExpression outermostCall = getOutermostCall(call);
      if (GuiceUtils.isUntargetedBinding(outermostCall)) {
        PsiElement outermostSource = getOutermostSource(call);
        if (outermostSource != null) {
          descriptors.add(new UntargetedBindDescriptor(outermostSource));
        }
      }
    }

    // OptionalBinder
    if (("newOptionalBinder".equals(methodName) || "optionalBinder".equals(methodName)) &&
        ("com.google.inject.multibindings.OptionalBinder".equals(qName) || isGuicePackage(qName))) {

      PsiElement outermostSource = getOutermostSource(call);
      if (outermostSource != null) {
        descriptors.add(new OptionalBindDescriptor(outermostSource, extractSingleTypeArg(call)));
      }
    }

    // SetBinder (Multibinder)
    if (("newSetBinder".equals(methodName) || "setBinder".equals(methodName)) &&
        ("com.google.inject.multibindings.Multibinder".equals(qName) || isGuicePackage(qName))) {

      PsiElement outermostSource = getOutermostSource(call);
      if (outermostSource != null) {
        descriptors.add(new SetMultibindDescriptor(outermostSource, extractSingleTypeArg(call)));
      }
    }

    // MapBinder
    if (("newMapBinder".equals(methodName) || "mapBinder".equals(methodName)) &&
        ("com.google.inject.multibindings.MapBinder".equals(qName) || isGuicePackage(qName))) {

      PsiElement outermostSource = getOutermostSource(call);
      if (outermostSource != null) {
        PsiClass[] kv = extractDualTypeArgs(call);
        descriptors.add(new MapMultibindDescriptor(outermostSource, kv[0], kv[1]));
      }
    }

    // MultimapBinder
    if (("newSetMultimapBinder".equals(methodName) || "multimapBinder".equals(methodName)) &&
        ("com.google.common.inject.MultimapBinder".equals(qName) || isGuicePackage(qName))) {

      PsiElement outermostSource = getOutermostSource(call);
      if (outermostSource != null) {
        PsiClass[] kv = extractDualTypeArgs(call);
        descriptors.add(new MultimapBindDescriptor(outermostSource, kv[0], kv[1]));
      }
    }

    // AssistedInject FactoryModuleBuilder.build()
    if ("build".equals(methodName) &&
        ("com.google.inject.assistedinject.FactoryModuleBuilder".equals(qName) || isGuicePackage(qName))) {

      List<UExpression> args = call.getValueArguments();
      if (!args.isEmpty()) {
        PsiType factoryType = GuiceUtils.getBindingTypeFromExpression(args.getFirst());
        PsiClass factoryClass = factoryType instanceof PsiClassType ct ? ct.resolve() : null;
        descriptors.add(new AssistedFactoryBindDescriptor(sourcePsi, factoryClass));
      }
    }
  }

  private static UCallExpression getOutermostCall(UCallExpression bindCall) {
    UElement outermost = GuiceUtils.getOutermostQualifiedParent(bindCall);
    UExpression selector = GuiceUtils.getSelectorIfQualified(outermost);
    return selector instanceof UCallExpression ? (UCallExpression)selector : bindCall;
  }

  private static @Nullable PsiElement getOutermostSource(UCallExpression call) {
    return GuiceUtils.getOutermostQualifiedParent(call).getSourcePsi();
  }


  public static PsiClass @NotNull [] getGuiceModuleClasses(final @NotNull Module module) {
    final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);

    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      PsiClass[] classes = getGuiceModuleClasses(module, scope);
      return CachedValueProvider.Result.createSingleDependency(classes, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public static PsiClass @NotNull [] getGuiceModuleClasses(final @NotNull Module module, @NotNull GlobalSearchScope scope) {
    Set<PsiClass> psiClasses = new HashSet<>();

    final PsiClass moduleInterface = JavaPsiFacade.getInstance(module.getProject()).findClass("com.google.inject.Module", scope);
    if (moduleInterface != null) {
      psiClasses.addAll(ClassInheritorsSearch.search(moduleInterface, scope, true).findAll());
    } else {
      final PsiClass abstractModuleClass = JavaPsiFacade.getInstance(module.getProject()).findClass(GuiceClasses.ABSTRACT_MODULE, scope);
      if (abstractModuleClass != null) {
        psiClasses.addAll(ClassInheritorsSearch.search(abstractModuleClass, scope, true).findAll());
      }
    }
    return psiClasses.toArray(PsiClass.EMPTY_ARRAY);
  }

  public static @NotNull Collection<PsiClass> getBindingAnnotations(@Nullable Module module) {
    if (module == null) return Collections.emptySet();
    Set<PsiClass> result = new HashSet<>();
    for (String qualifier : GuiceAnnotations.BINDING_ANNOTATIONS) {
      result.addAll(MetaAnnotationUtil.getAnnotationTypesWithChildren(module, qualifier, false));
    }
    return result;
  }


  public static @NotNull Set<PsiAnnotation> getBindingAnnotations(@NotNull PsiModifierListOwner owner) {
    Set<PsiAnnotation> annotations = new HashSet<>();
    final PsiModifierList modifierList = owner.getModifierList();
    if (modifierList == null) return annotations;

    final Collection<PsiClass> allBindingAnnos = getBindingAnnotations(ModuleUtilCore.findModuleForPsiElement(owner));
    if (allBindingAnnos.isEmpty()) return annotations;

    final Set<String> allBindingAnnoFqns = new HashSet<>();
    for (PsiClass psiClass : allBindingAnnos) {
      final String fqn = psiClass.getQualifiedName();
      if (fqn != null) {
        allBindingAnnoFqns.add(fqn);
      }
    }

    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
      final String fqn = annotation.getQualifiedName();
      if (fqn != null && allBindingAnnoFqns.contains(fqn)) {
        annotations.add(annotation);
      }
    }
    return annotations;
  }
}
