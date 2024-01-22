package com.intellij.plugins.jboss.arquillian.junit.testFramework;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.junit.JUnit4Framework;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.jarRepository.RepositoryAddLibraryAction;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import com.intellij.plugins.jboss.arquillian.junit.runConfiguration.ArquillianJUnitRunConfigurationType;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;

import javax.swing.*;

public final class ArquillianJUnitFramework extends JUnit4Framework {
  @Override
  public @NotNull String getName() {
    return "Arquillian JUnit4";
  }

  @Override
  protected String getMarkerClassFQName() {
    return "org.jboss.arquillian.junit.Arquillian";
  }

  @Override
  public FileTemplateDescriptor getTestClassFileTemplateDescriptor() {
    return new FileTemplateDescriptor("Arquillian JUnit Test Class.java");
  }

  @Override
  public Promise<Void> setupLibrary(Module module) {
    RepositoryLibraryDescription junitLibrary = RepositoryLibraryDescription.findDescription(
      "org.jboss.arquillian.junit",
      "arquillian-junit-container");
    return RepositoryAddLibraryAction.addLibraryToModule(junitLibrary, module);
  }

  @Override
  public @NotNull Icon getIcon() {
    return ArquillianIcons.Arquillian;
  }

  @Override
  public boolean isTestClass(PsiClass clazz, boolean canBePotential) {
    return isFrameworkAvailable(clazz) && ArquillianUtils.isJunitArquillianEnabled(clazz);
  }

  @Override
  public boolean isTestMethod(PsiElement element, boolean checkAbstract) {
    return element instanceof PsiMethod && isFrameworkAvailable(element) && JUnitUtil.getTestMethod(element, true, false) != null;
  }

  @Override
  public boolean isTestMethod(PsiMethod method, PsiClass myClass) {
    return isFrameworkAvailable(method) && JUnitUtil.isTestMethod(MethodLocation.elementInClass(method, myClass), true, false);
  }

  @Override
  public boolean isMyConfigurationType(@NotNull ConfigurationType type) {
    return type instanceof ArquillianJUnitRunConfigurationType;
  }
}
