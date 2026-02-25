// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.tests.runner;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.GrailsRunConfigurationType;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.tests.GrailsTestUtils;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.ext.spock.SpockUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.Objects;

import static org.jetbrains.plugins.grails.tests.runner.ByClassKt.setupConfigurationByClass;
import static org.jetbrains.plugins.grails.tests.runner.ByDirectoryKt.setupConfigurationByDir;

final class GrailsTestConfigurationProducer extends LazyRunConfigurationProducer<GrailsRunConfiguration> {
  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return GrailsRunConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull GrailsRunConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElementRef) {
    final GrailsApplication application = GrailsApplicationManager.findApplication(context.getPsiLocation());
    if (application == null) return false;

    final PsiElement sourceElement = setupConfigurationFromContext0(configuration, application, context);
    if (sourceElement == null) return false;

    configuration.setGrailsApplication(application);
    sourceElementRef.set(sourceElement);

    if (application.getGrailsVersion().isAtLeast("1.3.0")) {
      final String parameters = configuration.getProgramParameters();
      configuration.setProgramParameters(parameters == null ? null : parameters + " -echoOut");
    }

    final String vmParameters = configuration.getVMParameters();
    if (StringUtil.isEmpty(vmParameters)) {
      configuration.setVMParameters("-Dgrails.full.stacktrace=true");
    }
    else {
      if (!vmParameters.contains("-Dgrails.full.stacktrace=true")) {
        configuration.setVMParameters(vmParameters + ' ' + "-Dgrails.full.stacktrace=true");
      }
    }

    return true;
  }

  private static @Nullable PsiElement setupConfigurationFromContext0(@NotNull GrailsRunConfiguration configuration,
                                                                     @NotNull GrailsApplication application,
                                                                     @NotNull ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    if (element == null) return null;

    if (element instanceof PsiDirectory) {
      if (setupConfigurationByDir(configuration, application, (PsiDirectory)element)) {
        return element;
      }
      return null;
    }

    PsiClass psiClass = null;

    final PsiFile file = element.getContainingFile();

    if (file instanceof GroovyFile groovyFile) {
      if (!groovyFile.isScript()) {
        for (GrTypeDefinition typeDefinition : groovyFile.getTypeDefinitions()) {
          if (typeDefinition instanceof GrClassDefinition && isGrailsTestClass(typeDefinition)) {
            psiClass = typeDefinition;
            break;
          }
        }
      }
    }
    else if (file instanceof PsiJavaFile) {
      for (PsiClass aClass : ((PsiJavaFile)file).getClasses()) {
        if (isGrailsTestClass(aClass)) {
          psiClass = aClass;
          break;
        }
      }
    }

    if (psiClass == null) return null;

    PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
    return setupConfigurationByClass(configuration, application, psiClass, method);
  }

  /**
   * Remove unimportant cmd parameters
   */
  private static String cleanCmd(@Nullable String cmd) {
    return cmd == null ? null : cmd.replace("-echoOut", "").replaceAll("\\s+", " ").trim();
  }

  @Override
  public boolean isConfigurationFromContext(@NotNull GrailsRunConfiguration configuration, @NotNull ConfigurationContext context) {
    final GrailsApplication application = GrailsApplicationManager.findApplication(context.getPsiLocation());
    if (application == null || !application.equals(configuration.getGrailsApplicationNullable())) return false;

    if (configuration.getVMParameters() == null) return false;

    final GrailsRunConfiguration tmp = new GrailsRunConfiguration(context.getProject(), getConfigurationFactory(), "tmp");
    if (setupConfigurationFromContext0(tmp, application, context) == null) return false;

    return Objects.equals(
      cleanCmd(tmp.getProgramParameters()),
      cleanCmd(configuration.getProgramParameters())
    );
  }

  public static boolean isGrailsTestClass(@NotNull PsiClass psiClass) {
    if (!GrailsUtils.isInGrailsTests(psiClass)) return false;

    if (AnnotationUtil.isAnnotated(psiClass, GrailsTestUtils.TEST_ANNOTATIONS, 0)) return true;

    String name = psiClass.getName();
    if (name == null) return false;

    if (name.matches(".+(Spec|Specification)")) {
      return InheritanceUtil.isInheritor(psiClass, SpockUtils.SPEC_CLASS_NAME);
    }
    else {
      if (!name.matches(".+Tests?")) return false;
      if (InheritanceUtil.isInheritor(psiClass, "junit.framework.TestCase")
          || InheritanceUtil.isInheritor(psiClass, SpockUtils.SPEC_CLASS_NAME)) {
        return true;
      }
    }

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      if (psiMethod.getModifierList().hasAnnotation("org.junit.Test")) {
        return true;
      }
    }

    return false;
  }

  public static boolean isGrailsTestMethod(@NotNull PsiMethod method) {
    PsiClass aClass = method.getContainingClass();
    if (aClass == null) return false;

    Project project = aClass.getProject();

    return SpockUtils.isTestMethod(method) ||
           JUnitUtil.isTestMethod(new MethodLocation(project, method, new PsiLocation<>(project, aClass)), true, false, false);
  }
}
