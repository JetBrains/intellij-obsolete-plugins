package com.intellij.gwt.clientBundle;

import com.intellij.gwt.clientBundle.jam.ClientBundleMethodJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceClassJamElement;
import com.intellij.gwt.clientBundle.jam.CssResourceMethodJamElement;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.codeInsight.GwtPatterns.inModuleWithGwtFacet;
import static com.intellij.gwt.codeInsight.GwtPatterns.inProjectWithGwtFacet;
import static com.intellij.patterns.PsiJavaPatterns.and;
import static com.intellij.patterns.PsiJavaPatterns.psiClass;
import static com.intellij.patterns.PsiJavaPatterns.psiMethod;

final class ClientBundleSemContributor extends SemContributor {
  @Override
  public void registerSemProviders(@NotNull SemRegistrar registrar, @NotNull Project project) {
    ClientBundleMethodJamElement.META.register(registrar, and(inModuleWithGwtFacet(PsiMethod.class),
                                                              psiMethod().inClass(psiClass().isInterface().inheritorOf(true, ClientBundleUtil.CLIENT_BUNDLE_INTERFACE))));
    final PsiClassPattern cssResourcesClass = psiClass().isInterface().inheritorOf(true, ClientBundleUtil.CSS_RESOURCE_INTERFACE);
    CssResourceClassJamElement.META.register(registrar,
                                             and(inProjectWithGwtFacet(PsiClass.class), cssResourcesClass));
    CssResourceMethodJamElement.META.register(registrar,
                                              and(inProjectWithGwtFacet(PsiMethod.class),
                                                  psiMethod().inClass(cssResourcesClass)));
  }
}
