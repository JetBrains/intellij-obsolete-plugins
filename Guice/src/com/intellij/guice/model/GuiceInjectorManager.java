// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
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
import org.jetbrains.uast.UField;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
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

  /**
   * Immutable snapshot of registered contributors and their merged binding words.
   *
   * <p>Computed once per indexing pass and threaded through all file-processing
   * calls to avoid re-querying the extension point for every file.  For a 10K+ file
   * initial build this eliminates thousands of redundant EP lookups and
   * {@code HashSet} allocations.
   */
  record ContributorSnapshot(@NotNull Set<String> bindingWords,
                              @NotNull List<GuiceBindingContributor> contributors,
                              @NotNull Set<String> providesAnnotations) {
    static @NotNull ContributorSnapshot create() {
      List<GuiceBindingContributor> contributors = GuiceBindingContributor.EP_NAME.getExtensionList();
      Set<String> words = new HashSet<>();
      for (GuiceBindingContributor c : contributors) {
        words.addAll(c.getBindingWords());
      }
      return new ContributorSnapshot(Set.copyOf(words), contributors,
                                     GuiceBindingMatchStrategy.getAllProvidesAnnotations());
    }
  }

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    ContributorSnapshot snapshot = ContributorSnapshot.create();
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

  /**
   * Extracts binding descriptors from a single file.
   *
   * <p>This overload creates a fresh {@link ContributorSnapshot} on demand.
   * For batch processing, prefer {@link #getBindingsInFile(PsiFile, ContributorSnapshot)}
   * to share a single snapshot across all files.
   */
  public static @NotNull Set<BindDescriptor> getBindingsInFile(@NotNull PsiFile file) {
    return getBindingsInFile(file, ContributorSnapshot.create());
  }

  /**
   * Extracts binding descriptors from a single file using a pre-computed
   * {@link ContributorSnapshot}.
   *
   * <p>The result is cached per-file via {@link CachedValuesManager} and invalidated
   * when the file's PSI tree changes.  The snapshot is only consulted on cache miss.
   *
   * @param file     the file to extract bindings from
   * @param snapshot the pre-computed contributor state (binding words + contributor list)
   * @return the set of binding descriptors found in the file
   */
  static @NotNull Set<BindDescriptor> getBindingsInFile(@NotNull PsiFile file,
                                                         @NotNull ContributorSnapshot snapshot) {
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
            if (callName != null && snapshot.bindingWords().contains(callName)) {
              final PsiMethod resolved = call.resolve();
              if (resolved != null) {
                final PsiClass containingClass = resolved.getContainingClass();
                if (containingClass != null) {
                  final String qName = containingClass.getQualifiedName();
                  if (qName != null) {
                    dispatchToContributors(snapshot.contributors(), call, callName, qName, containingClass, descriptors);
                  }
                }
              } else {
                // Fallback for unresolved calls: when the code is incomplete or
                // references a non-existent class (e.g., bind(X.class).to(DoNotExist.class)),
                // call.resolve() returns null.  Since we are already inside a verified Guice
                // module class, we can safely create a descriptor based on the method name.
                dispatchUnresolvedToContributors(snapshot.contributors(), call, callName, descriptors);
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

    final PsiClass moduleClass =
        JavaPsiFacade.getInstance(file.getProject()).findClass("com.google.inject.Module", file.getResolveScope());
    if (moduleClass == null) {
      return List.of();
    }
    List<PsiClass> result = new ArrayList<>();
    for (PsiClass aClass : classOwner.getClasses()) {
      collectAllGuiceModules(aClass, moduleClass, result);
    }
    return result;
  }

  private static void collectAllGuiceModules(@NotNull PsiClass aClass,
      @NotNull PsiClass moduleClass,
                                             @NotNull List<PsiClass> result) {
    if (aClass.isInheritor(moduleClass, true)) {
      result.add(aClass);
    }
    // Always recurse into inner classes — each module is visited independently.
    for (PsiClass inner : aClass.getInnerClasses()) {
      collectAllGuiceModules(inner, moduleClass, result);
    }
  }

  /**
   * Dispatches a resolved call expression to registered contributors.
   * Stops at the first contributor that claims to handle the call.
   */
  private static void dispatchToContributors(@NotNull List<GuiceBindingContributor> contributors,
                                             @NotNull UCallExpression call,
                                             @NotNull String callName,
                                             @NotNull String qName,
                                             @NotNull PsiClass containingClass,
                                             @NotNull Set<BindDescriptor> descriptors) {
    for (GuiceBindingContributor contributor : contributors) {
      if (contributor.getBindingWords().contains(callName)) {
        if (contributor.processCall(call, callName, qName, containingClass, descriptors)) {
          return;
        }
      }
    }
  }

  /**
   * Dispatches an unresolved call expression to registered contributors.
   * Stops at the first contributor that claims to handle the call.
   */
  private static void dispatchUnresolvedToContributors(@NotNull List<GuiceBindingContributor> contributors,
                                                       @NotNull UCallExpression call,
                                                       @NotNull String callName,
                                                       @NotNull Set<BindDescriptor> descriptors) {
    for (GuiceBindingContributor contributor : contributors) {
      if (contributor.getBindingWords().contains(callName)) {
        if (contributor.processUnresolvedCall(call, callName, descriptors)) {
          return;
        }
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
