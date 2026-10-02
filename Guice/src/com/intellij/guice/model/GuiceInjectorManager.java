// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.InheritanceUtil;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GuiceInjectorManager {

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(final @NotNull PsiElement scope) {
    if (scope instanceof PsiClass scopeClass) {
      if (scopeClass instanceof PsiCompiledElement) {
        PsiElement navElement = scopeClass.getNavigationElement();
        if (!(navElement instanceof PsiClass sourceClass) || sourceClass == scopeClass) {
          return Set.of();
        }
        scopeClass = sourceClass;
      }
      if (!InheritanceUtil.isInheritor(scopeClass, "com.google.inject.Module")) {
        return Set.of();
      }
      return extractDescriptorsFromModuleClass(scopeClass, GuiceExtensionIndex.get());
    }
    Set<BindDescriptor> all = getBindingDescriptors(scope.getProject(), new LocalSearchScope(scope));
    Set<BindDescriptor> filtered = new HashSet<>();
    for (BindDescriptor bd : all) {
      PsiElement expr = bd.getBindExpression();
      if (expr != null && PsiTreeUtil.isContextAncestor(scope, expr, false)) {
        filtered.add(bd);
      }
    }
    return filtered;
  }

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    GuiceExtensionIndex index = GuiceExtensionIndex.get();
    Set<BindDescriptor> descriptors = new HashSet<>();
    final Set<PsiFile> files = getFilesToProcess(project, scope, index);
    for (PsiFile file : files) {
      descriptors.addAll(getBindingsInFile(file));
    }
    return descriptors;
  }

  private static @NotNull Set<PsiFile> getFilesToProcess(@NotNull Project project,
                                                         @NotNull SearchScope scope,
                                                         @NotNull GuiceExtensionIndex index) {
    final Set<PsiFile> files = new HashSet<>();
    if (scope instanceof GlobalSearchScope) {
      final PsiSearchHelper helper = PsiSearchHelper.getInstance(project);
      for (String word : index.getDescriptorCallNames()) {
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
    if (file instanceof PsiCompiledFile clsFile) {
      PsiElement sourceElement = clsFile.getNavigationElement();
      if (!(sourceElement instanceof PsiFile sourceFile) || sourceFile == clsFile) {
        return Set.of();
      }
      file = sourceFile;
    }
    List<PsiClass> guiceModules = collectGuiceModuleClasses(file);
    if (guiceModules.isEmpty()) {
      return Set.of();
    }
    GuiceExtensionIndex currentIndex = GuiceExtensionIndex.get();
    Set<BindDescriptor> descriptors = new HashSet<>();
    for (PsiClass moduleClass : guiceModules) {
      descriptors.addAll(extractDescriptorsFromModuleClass(moduleClass, currentIndex));
    }
    return descriptors;
  }

  private static @NotNull Set<BindDescriptor> extractDescriptorsFromModuleClass(@NotNull PsiClass moduleClass,
                                                                                @NotNull GuiceExtensionIndex currentIndex) {
    UClass uClass = UastContextKt.toUElement(moduleClass, UClass.class);
    if (uClass == null) {
      return Set.of();
    }
    final Set<BindDescriptor> descriptors = new HashSet<>();
    AbstractUastVisitor visitor = new AbstractUastVisitor() {
      @Override
      public boolean visitClass(@NotNull UClass node) {
        return true;
      }

      @Override
      public boolean visitField(@NotNull UField node) {
        return true;
      }

      @Override
      public boolean visitCallExpression(@NotNull UCallExpression call) {
        currentIndex.processDescriptorCall(call, descriptors);
        return false;
      }
    };
    for (UElement declaration : uClass.getUastDeclarations()) {
      declaration.accept(visitor);
    }
    return descriptors.isEmpty() ? Set.of() : descriptors;
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

  public static PsiClass @NotNull [] getGuiceModuleClasses(final @NotNull Module module, @NotNull GlobalSearchScope scope) {
    final PsiClass moduleInterface = JavaPsiFacade.getInstance(module.getProject()).findClass("com.google.inject.Module", scope);
    if (moduleInterface == null) {
      return PsiClass.EMPTY_ARRAY;
    }
    return ClassInheritorsSearch.search(moduleInterface, scope, true).findAll().toArray(PsiClass.EMPTY_ARRAY);
  }
}
