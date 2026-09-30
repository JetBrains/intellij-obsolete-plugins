// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UFile;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class GuiceInjectorManager {

  private static final AtomicLong SNAPSHOT_MODIFICATION_COUNT = new AtomicLong();

  private static final ModificationTracker EP_MODIFICATION_TRACKER =
    () -> ContributorSnapshot.getInstance().modificationCount();

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
      if (myScope instanceof PsiClass scopeClass) {
        PsiFile file = scopeClass.getContainingFile();
        if (file == null) return Result.create(Set.of(), myScope, PsiModificationTracker.MODIFICATION_COUNT, EP_MODIFICATION_TRACKER);
        Map<PsiClass, Set<BindDescriptor>> byModule = getBindingsByModuleInFile(file, ContributorSnapshot.getInstance());
        Set<BindDescriptor> result = byModule.get(scopeClass);
        if (result == null) {
          for (Map.Entry<PsiClass, Set<BindDescriptor>> entry : byModule.entrySet()) {
            if (scopeClass.getManager().areElementsEquivalent(entry.getKey(), scopeClass)) {
              result = entry.getValue();
              break;
            }
          }
        }
        return Result.create(result != null ? result : Set.of(), file, PsiModificationTracker.MODIFICATION_COUNT, EP_MODIFICATION_TRACKER);
      }
      Set<BindDescriptor> all = getBindingDescriptors(myScope.getProject(), new LocalSearchScope(myScope));
      Set<BindDescriptor> filtered = new HashSet<>();
      for (BindDescriptor bd : all) {
        PsiElement expr = bd.getBindExpression();
        if (expr != null && PsiTreeUtil.isContextAncestor(myScope, expr, false)) {
          filtered.add(bd);
        }
      }
      return Result.create(filtered, myScope, PsiModificationTracker.MODIFICATION_COUNT, EP_MODIFICATION_TRACKER);
    }
  }

  /**
   * Immutable snapshot of registered contributors and their binding words.
   *
   * <p>The extension point caches this snapshot and invalidates it when a contributor
   * is registered or unregistered.
   */
  record ContributorSnapshot(@NotNull Set<String> bindingWords,
                             @NotNull List<GuiceBindingContributor> contributors,
                             @NotNull Map<String, List<GuiceBindingContributor>> contributorsByWord,
                             long modificationCount) {
    static @NotNull ContributorSnapshot getInstance() {
      return GuiceBindingContributor.EP_NAME.computeIfAbsent(ContributorSnapshot.class, ContributorSnapshot::create);
    }

    static @NotNull ContributorSnapshot create() {
      List<GuiceBindingContributor> contributors = GuiceBindingContributor.EP_NAME.getExtensionList();
      Set<String> words = new HashSet<>();
      Map<String, List<GuiceBindingContributor>> byWord = new HashMap<>();
      for (GuiceBindingContributor c : contributors) {
        for (String word : c.getBindingWords()) {
          words.add(word);
          byWord.computeIfAbsent(word, k -> new ArrayList<>()).add(c);
        }
      }
      Map<String, List<GuiceBindingContributor>> immutableByWord = new HashMap<>(byWord.size());
      for (Map.Entry<String, List<GuiceBindingContributor>> entry : byWord.entrySet()) {
        immutableByWord.put(entry.getKey(), List.copyOf(entry.getValue()));
      }
      return new ContributorSnapshot(
        Set.copyOf(words),
        contributors,
        Map.copyOf(immutableByWord),
        SNAPSHOT_MODIFICATION_COUNT.incrementAndGet());
    }

    @NotNull List<GuiceBindingContributor> contributorsForWord(@NotNull String word) {
      return contributorsByWord.getOrDefault(word, List.of());
    }
  }

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    ContributorSnapshot snapshot = ContributorSnapshot.getInstance();
    Set<BindDescriptor> descriptors = new HashSet<>();
    final Set<PsiFile> files = getFilesToProcess(project, scope, snapshot);
    for (PsiFile file : files) {
      descriptors.addAll(getBindingsInFile(file, snapshot));
    }
    return descriptors;
  }

  private static @NotNull Set<PsiFile> getFilesToProcess(@NotNull Project project,
                                                         @NotNull SearchScope scope,
                                                         @NotNull ContributorSnapshot snapshot) {
    final Set<PsiFile> files = new HashSet<>();
    if (scope instanceof GlobalSearchScope) {
      final PsiSearchHelper helper = PsiSearchHelper.getInstance(project);
      for (String word : snapshot.bindingWords()) {
        helper.processAllFilesWithWord(word, (GlobalSearchScope)scope, file -> {
          files.add(file);
          return true;
        }, true);
      }
    }
    else if (scope instanceof LocalSearchScope) {
      for (PsiElement element : ((LocalSearchScope)scope).getScope()) {
        final PsiFile file = element.getContainingFile();
        if (file != null) {
          files.add(file);
        }
      }
    }
    return files;
  }

  /**
   * Extracts binding descriptors from a single file.
   */
  public static @NotNull Set<BindDescriptor> getBindingsInFile(@NotNull PsiFile file) {
    return getBindingsInFile(file, ContributorSnapshot.getInstance());
  }

  /**
   * Extracts binding descriptors from a single file using a pre-computed
   * {@link ContributorSnapshot}.
   *
   * <p>The result is cached per-file via {@link CachedValuesManager} and invalidated
   * when PSI or the contributor extension point changes.
   *
   * @param file     the file to extract bindings from
   * @param snapshot the pre-computed contributor state (binding words + contributor list)
   * @return the set of binding descriptors found in the file
   */
  static @NotNull Set<BindDescriptor> getBindingsInFile(@NotNull PsiFile file,
                                                         @NotNull ContributorSnapshot snapshot) {
    Map<PsiClass, Set<BindDescriptor>> byModule = getBindingsByModuleInFile(file, snapshot);
    if (byModule.isEmpty()) return Set.of();
    Set<BindDescriptor> descriptors = new HashSet<>();
    for (Set<BindDescriptor> moduleDescriptors : byModule.values()) {
      descriptors.addAll(moduleDescriptors);
    }
    return descriptors;
  }

  private static @NotNull Map<PsiClass, Set<BindDescriptor>> getBindingsByModuleInFile(@NotNull PsiFile file,
                                                                                        @NotNull ContributorSnapshot snapshot) {
    // Compiled class files cannot be walked with PsiRecursiveElementWalkingVisitor
    // (getNextSibling() is too slow) and don't contain method bodies.
    // However, if the library has attached sources (source jars), we can use the source file instead.
    if (file instanceof PsiCompiledFile clsFile) {
      PsiElement sourceElement = clsFile.getNavigationElement();
      if (!(sourceElement instanceof PsiFile sourceFile) || sourceFile == clsFile) {
        return Map.of();
      }
      file = sourceFile;
    }
    final PsiFile fileToWalk = file;
    return CachedValuesManager.getCachedValue(fileToWalk, () -> {
      ContributorSnapshot currentSnapshot = ContributorSnapshot.getInstance();
      // Collect all Guice module classes in this file, then traverse only those
      // using a UAST visitor.  This is language-agnostic (Java + Kotlin), avoids
      // scanning non-module classes, and skips inner classes during traversal
      // (since each inner module is visited separately from the collected list).
      List<PsiClass> guiceModules = collectGuiceModuleClasses(fileToWalk);
      if (guiceModules.isEmpty()) {
        return CachedValueProvider.Result.create(
          Map.of(), fileToWalk, PsiModificationTracker.MODIFICATION_COUNT, EP_MODIFICATION_TRACKER);
      }
      final Map<PsiClass, Set<BindDescriptor>> byModule = new HashMap<>(guiceModules.size());
      for (PsiClass moduleClass : guiceModules) {
        UClass uClass = UastContextKt.toUElement(moduleClass, UClass.class);
        if (uClass == null) continue;
        final Set<BindDescriptor> descriptors = new HashSet<>();
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
            List<GuiceBindingContributor> matchingContributors =
              callName != null ? currentSnapshot.contributorsForWord(callName) : List.of();
            if (!matchingContributors.isEmpty()) {
              final PsiMethod resolved = call.resolve();
              if (resolved != null) {
                final PsiClass containingClass = resolved.getContainingClass();
                if (containingClass != null) {
                  final String qName = containingClass.getQualifiedName();
                  if (qName != null) {
                    dispatchToContributors(matchingContributors, call, callName, qName, containingClass, descriptors);
                  }
                }
              }
              else {
                // Fallback for unresolved calls: when the code is incomplete or
                // references a non-existent class (e.g., bind(X.class).to(DoNotExist.class)),
                // call.resolve() returns null.  Since we are already inside a verified Guice
                // module class, we can safely create a descriptor based on the method name.
                dispatchUnresolvedToContributors(matchingContributors, call, callName, descriptors);
              }
            }
            return false; // continue into children for chained calls
          }
        };
        // Visit methods directly — the visitClass override above prevents
        // descending into inner classes.
        for (UElement declaration : uClass.getUastDeclarations()) {
          declaration.accept(visitor);
        }
        if (!descriptors.isEmpty()) {
          byModule.put(moduleClass, descriptors);
        }
      }
      return CachedValueProvider.Result.create(
        byModule.isEmpty() ? Map.of() : byModule,
        fileToWalk,
        PsiModificationTracker.MODIFICATION_COUNT,
        EP_MODIFICATION_TRACKER);
    });
  }

  /**
   * Collects <em>all</em> Guice module classes in the file, including nested, local and anonymous ones.
   * Each module class is traversed separately with inner-class skipping in the visitor,
   * so there is no double-walking.
   *
   * <p>Works for both Java ({@link PsiClass}) and Kotlin ({@code KtLightClass}) since
   * {@link PsiClassOwner#getClasses()} returns light classes for Kotlin files, and
   * {@link InheritanceUtil#isInheritor(PsiClass, String)} handles both.
   */
  static @NotNull List<PsiClass> collectGuiceModuleClasses(@NotNull PsiFile file) {
    if (!(file instanceof PsiClassOwner classOwner)) return List.of();

    final PsiClass moduleClass =
        JavaPsiFacade.getInstance(file.getProject()).findClass("com.google.inject.Module", file.getResolveScope());
    if (moduleClass == null) {
      return List.of();
    }
    List<PsiClass> result = new ArrayList<>();
    for (PsiClass aClass : collectAllClasses(classOwner)) {
      if (aClass.isInheritor(moduleClass, true)) {
        result.add(aClass);
      }
    }
    return result;
  }

  /**
   * Returns all classes of the file: top-level, nested, local and anonymous classes,
   * for example {@code new AbstractModule() {...}} or a Kotlin {@code object : AbstractModule()}.
   * For a Kotlin file the result contains the light classes.
   */
  static @NotNull List<PsiClass> collectAllClasses(@NotNull PsiFile file) {
    List<PsiClass> result = new ArrayList<>();
    if (file instanceof PsiCompiledElement && file instanceof PsiClassOwner classOwner) {
      // A walk over compiled PSI is slow, and compiled code has no local or anonymous classes in the PSI.
      for (PsiClass aClass : classOwner.getClasses()) {
        collectNestedClasses(aClass, result);
      }
      return result;
    }
    if (file instanceof PsiJavaFile) {
      // PSI is faster than UAST here. A type parameter is also a PsiClass, so skip it.
      PsiTreeUtil.processElements(file, PsiClass.class, aClass -> {
        if (!(aClass instanceof PsiTypeParameter)) result.add(aClass);
        return true;
      });
      return result;
    }
    UFile uFile = UastContextKt.toUElement(file, UFile.class);
    if (uFile == null) {
      if (file instanceof PsiClassOwner classOwner) {
        for (PsiClass aClass : classOwner.getClasses()) {
          collectNestedClasses(aClass, result);
        }
      }
      return result;
    }
    uFile.accept(new AbstractUastVisitor() {
      @Override
      public boolean visitClass(@NotNull UClass node) {
        result.add(node.getJavaPsi());
        return false;
      }
    });
    return result;
  }

  private static void collectNestedClasses(@NotNull PsiClass aClass, @NotNull List<PsiClass> result) {
    result.add(aClass);
    for (PsiClass inner : aClass.getInnerClasses()) {
      collectNestedClasses(inner, result);
    }
  }

  /**
   * Dispatches a resolved call expression to the contributors that match {@code callName}.
   * Stops at the first contributor that handles the call.
   */
  private static void dispatchToContributors(@NotNull List<GuiceBindingContributor> matchingContributors,
                                             @NotNull UCallExpression call,
                                             @NotNull String callName,
                                             @NotNull String qName,
                                             @NotNull PsiClass containingClass,
                                             @NotNull Set<BindDescriptor> descriptors) {
    for (GuiceBindingContributor contributor : matchingContributors) {
      if (contributor.processCall(call, callName, qName, containingClass, descriptors)) {
        return;
      }
    }
  }

  /**
   * Dispatches an unresolved call expression to the contributors that match {@code callName}.
   * Stops at the first contributor that handles the call.
   */
  private static void dispatchUnresolvedToContributors(@NotNull List<GuiceBindingContributor> matchingContributors,
                                                       @NotNull UCallExpression call,
                                                       @NotNull String callName,
                                                       @NotNull Set<BindDescriptor> descriptors) {
    for (GuiceBindingContributor contributor : matchingContributors) {
      if (contributor.processUnresolvedCall(call, callName, descriptors)) {
        return;
      }
    }
  }

  public static PsiClass @NotNull [] getGuiceModuleClasses(final @NotNull Module module) {
    final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);

    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      PsiClass[] classes = getGuiceModuleClasses(module, scope);
      return CachedValueProvider.Result.createSingleDependency(classes, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  public static PsiClass @NotNull [] getGuiceModuleClasses(final @NotNull Module module, @NotNull GlobalSearchScope scope) {
    final PsiClass moduleInterface = JavaPsiFacade.getInstance(module.getProject()).findClass("com.google.inject.Module", scope);
    if (moduleInterface == null) {
      return PsiClass.EMPTY_ARRAY;
    }
    return ClassInheritorsSearch.search(moduleInterface, scope, true).findAll().toArray(PsiClass.EMPTY_ARRAY);
  }
}
