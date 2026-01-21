// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.BindToDescriptor;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public final class GuiceInjectorManager {

  public static @NotNull Set<BindDescriptor> getInjectBindingDescriptors(@NotNull InjectionPointDescriptor ip,
                                                                         @NotNull Set<? extends BindDescriptor> allDescriptors) {
    Set<BindDescriptor> descriptors = new HashSet<>();
    final PsiType type = ip.getType();
    if (type instanceof PsiClassType) {
      final PsiClass psiClass = ((PsiClassType)type).resolve();
      if (psiClass != null) {
        for (BindDescriptor descriptor : allDescriptors) {
          if (psiClass.equals(descriptor.getBoundClass()) && GuiceInjectionUtil.checkBindingAnnotations(ip, descriptor)) {
            descriptors.add(descriptor);
          }
        }
      }
    }

    return descriptors;
  }

  public static @NotNull Set<GuiceProvides<?>> getInjectProvidesDescriptors(@NotNull InjectionPointDescriptor ip,
                                                                            @NotNull List<? extends GuiceProvides> allDescriptors) {
    Set<GuiceProvides<?>> set = new HashSet<>();
    final PsiType type = ip.getType();
    if (type != null) {
      for (GuiceProvides<?> descriptor : allDescriptors) {
        final PsiType productType = descriptor.getProductType();
        if (productType != null) {
          final Set<PsiAnnotation> bindingAnnotations = descriptor.getBindingAnnotations();
          if (type.isAssignableFrom(productType) &&
              GuiceInjectionUtil.checkBindingAnnotations(ip.getBindingAnnotations(), bindingAnnotations)) {
            set.add(descriptor);
          }
        }
      }
    }

    return set;
  }

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(final @Nullable Module module) {
    if (module == null) return Collections.emptySet();

    Set<VirtualFile> files = new HashSet<>();
    for (PsiClass aClass : getGuiceModuleClasses(module)) {
      final PsiFile file = aClass.getContainingFile();
      if (file != null) {
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
          files.add(virtualFile);
        }
      }
    }

    if (files.isEmpty()) return Collections.emptySet();
    final GlobalSearchScope fileScope = GlobalSearchScope.filesScope(module.getProject(), files);

    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module,
                                                                              () -> CachedValueProvider.Result
                                                                                .create(
                                                                                  getBindingDescriptors(module.getProject(), fileScope),
                                                                                  getModificationsTrackers(module)));
  }

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(final @NotNull PsiElement scope) {
    return CachedValuesManager.getCachedValue(scope, () -> CachedValueProvider.Result
      .create(getBindingDescriptors(scope.getProject(), new LocalSearchScope(scope)), scope));
  }

  public static @NotNull Set<BindDescriptor> getBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    Set<BindDescriptor> descriptors = ConcurrentCollectionFactory.createConcurrentSet();

    descriptors.addAll(getToBindingDescriptors(project, scope));
    descriptors.addAll(getToInstanceBindingDescriptors(project, scope));
    descriptors.addAll(getToProviderBindingDescriptors(project, scope));
    descriptors.addAll(getToConstructorBindingDescriptors(project, scope));

    return descriptors;
  }

  public static @NotNull Set<BindDescriptor> getToBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    return getDescriptors(project, scope, "to");
  }

  private static @Unmodifiable Set<BindDescriptor> getDescriptors(@NotNull Project project, @NotNull SearchScope scope, @NotNull String name) {
    final Set<PsiMethodCallExpression> expressions = getLinkedBindingBuilderExpressions(project, scope, name);
    return ContainerUtil.map2Set(expressions, expression -> new BindToDescriptor(expression));
  }

  public static @NotNull Set<BindDescriptor> getToInstanceBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    return getDescriptors(project, scope, "toInstance");
  }

  public static @NotNull Set<BindDescriptor> getToProviderBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    return getDescriptors(project, scope, "toProvider");
  }

  public static @NotNull Set<BindDescriptor> getToConstructorBindingDescriptors(@NotNull Project project, @NotNull SearchScope scope) {
    return getDescriptors(project, scope, "toConstructor");
  }

  public static PsiClass @NotNull [] getGuiceModuleClasses(final @NotNull Module module) {
    final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesScope(module);

    return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
      Set<PsiClass> psiClasses = new HashSet<>() {

      };

      final PsiClass abstractModuleClass = JavaPsiFacade.getInstance(module.getProject()).findClass(GuiceClasses.ABSTRACT_MODULE, scope);
      if (abstractModuleClass != null) {
        psiClasses.addAll(ClassInheritorsSearch.search(abstractModuleClass, scope, true).findAll());
      }
      final PsiClass[] classes = psiClasses.toArray(PsiClass.EMPTY_ARRAY);
      return CachedValueProvider.Result.createSingleDependency(classes, PsiModificationTracker.MODIFICATION_COUNT);
    });
  }

  private static Collection<?> getModificationsTrackers(@NotNull Module module) {
    final Set<Object> deps = ContainerUtil.newHashSet(getGuiceModuleClasses(module));
    deps.add(PsiModificationTracker.MODIFICATION_COUNT);

    return deps;
  }

  private static @NotNull Set<PsiMethodCallExpression> getLinkedBindingBuilderExpressions(@NotNull Project project,
                                                                                          @NotNull SearchScope scope,
                                                                                          @NotNull String methodName) {
    Set<PsiMethodCallExpression> expressions = ConcurrentCollectionFactory.createConcurrentSet();
    final PsiClass
      moduleClass = JavaPsiFacade.getInstance(project).findClass(GuiceClasses.LINKED_BINDING_BUILDER, GlobalSearchScope.allScope(project));
    if (moduleClass != null) {
      final PsiMethod[] binds = moduleClass.findMethodsByName(methodName, false);
      for (PsiMethod bind : binds) {
        Set<PsiCall> calls = StringExpressionHelper.searchMethodCalls(bind, scope);
        for (PsiCall call : calls) {
          if (call instanceof PsiMethodCallExpression) expressions.add((PsiMethodCallExpression)call);
        }
      }
    }
    return expressions;
  }

  public static @NotNull Collection<PsiClass> getBindingAnnotations(@Nullable Module module) {
    return module == null
           ? Collections.emptySet()
           : MetaAnnotationUtil.getAnnotationTypesWithChildren(module, GuiceAnnotations.BINDING_ANNOTATION, false);
  }

  public static @NotNull Set<PsiAnnotation> getBindingAnnotations(@NotNull PsiModifierListOwner owner) {
    Set<PsiAnnotation> annotations = ConcurrentCollectionFactory.createConcurrentSet();

    for (PsiClass psiClass : getBindingAnnotations(ModuleUtilCore.findModuleForPsiElement(owner))) {
      final String fqn = psiClass.getQualifiedName();
      if (fqn != null) {
        final PsiAnnotation annotation = AnnotationUtil.findAnnotation(owner, fqn);
        if (annotation != null) {
          annotations.add(annotation);
        }
      }
    }
    return annotations;
  }
}
