package com.intellij.seam.model.jam;

import com.intellij.openapi.project.Project;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.jam.jsf.SeamJamConverter;
import com.intellij.seam.model.jam.jsf.SeamJamValidator;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PsiJavaPatterns.psiClass;

public class SeamSemContributor extends SemContributor {
  @Override
  public void registerSemProviders(@NotNull SemRegistrar registrar, @NotNull Project project) {
    SeamJamComponent.META.register(registrar, psiClass().withAnnotation(SeamAnnotationConstants.COMPONENT_ANNOTATION));
    SeamJamInstall.META.register(registrar, psiClass().withAnnotation(SeamAnnotationConstants.INSTALL_ANNOTATION));
    SeamJamRole.META.register(registrar, psiClass().withAnnotation(SeamAnnotationConstants.ROLE_ANNOTATION));
    SeamJamRoles.META.register(registrar, psiClass().withAnnotation(SeamAnnotationConstants.ROLES_ANNOTATION));
    SeamJamValidator.META.register(registrar, psiClass().withAnnotation(SeamAnnotationConstants.JSF_VALIDATOR_ANNOTATION));
    SeamJamConverter.META.register(registrar, psiClass().withAnnotation(SeamAnnotationConstants.JSF_CONVERTER_ANNOTATION));
  }
}