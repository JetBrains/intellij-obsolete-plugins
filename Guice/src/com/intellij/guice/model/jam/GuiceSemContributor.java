// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.jam;

import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.constants.GuiceClasses;
import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.project.Project;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.psiField;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

final class GuiceSemContributor extends SemContributor {
  @Override
  protected boolean isAvailable(@NotNull Project project) {
    return JavaLibraryUtil.hasLibraryClass(project, GuiceClasses.ABSTRACT_MODULE);
  }

  @Override
  public void registerSemProviders(@NotNull SemRegistrar registrar, @NotNull Project project) {
    GuiceInject.METHOD_META.register(registrar, psiMethod().withAnnotations(GuiceAnnotations.INJECT));
    GuiceInject.FIELD_META.register(registrar, psiField().withAnnotations(GuiceAnnotations.INJECT));

    GuiceInject.FIELD_JSR_330_META.register(registrar, psiField().withAnnotations(GuiceAnnotations.JAVAX_INJECT));
    GuiceInject.METHOD_JSR_330_META.register(registrar, psiMethod().withAnnotations(GuiceAnnotations.JAVAX_INJECT));

    GuiceInject.FIELD_JAKARTA_META.register(registrar, psiField().withAnnotations(GuiceAnnotations.JAKARTA_INJECT));
    GuiceInject.METHOD_JAKARTA_META.register(registrar, psiMethod().withAnnotations(GuiceAnnotations.JAKARTA_INJECT));

    GuiceProvides.METHOD_META.register(registrar, psiMethod().withAnnotation(GuiceAnnotations.PROVIDES));
  }
}

