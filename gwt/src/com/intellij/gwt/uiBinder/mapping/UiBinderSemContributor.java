package com.intellij.gwt.uiBinder.mapping;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_TEMPLATE_ANNOTATION;

final class UiBinderSemContributor extends SemContributor {
  @Override
  public void registerSemProviders(@NotNull SemRegistrar registrar, @NotNull Project project) {
    UiTemplateInterfaceJamElement.META.register(registrar, PsiJavaPatterns.psiClass().withAnnotation(UI_TEMPLATE_ANNOTATION));
  }
}
