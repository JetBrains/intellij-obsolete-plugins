/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.run;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.junit.JavaRuntimeConfigurationProducerBase;
import com.intellij.j2meplugin.module.MobileModuleUtil;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class J2MEConfigurationProducer extends JavaRuntimeConfigurationProducerBase {
  private SmartPsiElementPointer<PsiClass> myPointer;

  protected J2MEConfigurationProducer() {
    super(ConfigurationTypeUtil.findConfigurationType(J2MEConfigurationType.class));
  }

  @Override
  public PsiElement getSourceElement() {
    return myPointer == null ? null : myPointer.getElement();
  }

  @Override
  protected RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext context) {
    location = stepIntoSingleClass(location);
    if (location == null) return null;
    final Project project = location.getProject();
    final PsiElement element = location.getPsiElement();
    final PsiClass aClass = getMobileExeClass(element, PsiManager.getInstance(project));
    if (aClass == null) return null;
    myPointer = SmartPointerManager.createPointer(aClass);
    RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(project, context);
    final J2MERunConfiguration configuration = (J2MERunConfiguration)settings.getConfiguration();
    configuration.MAIN_CLASS_NAME = aClass.getQualifiedName();
    configuration.IS_CLASSES = true;
    configuration.userParameters = new ArrayList<>();
    configuration
      .setModule(ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(aClass.getContainingFile().getVirtualFile()));
    configuration.setGeneratedName();
    return settings;
  }

  @Override
  public int compareTo(Object o) {
    return PREFERED;
  }

  @Override
  protected RunnerAndConfigurationSettings findExistingByElement(Location location,
                                                                 @NotNull List<? extends RunnerAndConfigurationSettings> existingConfigurations,
                                                                 ConfigurationContext context) {
    final PsiClass aClass = getMobileExeClass(location.getPsiElement(), PsiManager.getInstance(location.getProject()));
    if (aClass != null) {
      final String qualifiedName = aClass.getQualifiedName();
      for (RunnerAndConfigurationSettings existingConfiguration : existingConfigurations) {
        if (Comparing.equal(qualifiedName, ((J2MERunConfiguration)existingConfiguration.getConfiguration()).MAIN_CLASS_NAME)) {
          return existingConfiguration;
        }
      }
    }
    return null;
  }

  private static PsiClass getMobileExeClass(PsiElement element, final PsiManager manager) {
    while (element != null) {
      if (element instanceof PsiClass) {
        final PsiClass aClass = (PsiClass)element;
        if (isMobileExeClass(aClass, manager)) {
          return aClass;
        }
      }
      element = element.getParent();
    }
    return null;
  }

  private static boolean isMobileExeClass(final PsiClass aClass, final PsiManager manager) {
    if (DumbService.isDumb(manager.getProject())) return false;
    if (!PsiClassUtil.isRunnableClass(aClass, true)) return false;

    final GlobalSearchScope scope = GlobalSearchScope.allScope(manager.getProject());
    for (MobileApplicationType mobileApplicationType : MobileModuleUtil.getExistingMobileApplicationTypes()) {
      PsiClass mobileClass = JavaPsiFacade.getInstance(manager.getProject()).findClass(mobileApplicationType.getBaseClassName(), scope);
      if (mobileClass != null) {
        if (aClass.isInheritor(mobileClass, true)) return true;
      }
    }
    return false;
  }

  public static Location stepIntoSingleClass(final Location location) {
    PsiElement element = location.getPsiElement();
    if (PsiTreeUtil.getParentOfType(element, PsiClass.class) != null) return location;
    element = PsiTreeUtil.getParentOfType(element, PsiJavaFile.class);
    if (element == null) return location;
    final PsiJavaFile psiFile = ((PsiJavaFile)element);
    final PsiClass[] classes = psiFile.getClasses();
    if (classes.length != 1) return location;
    return PsiLocation.fromPsiElement(classes[0]);
  }
}