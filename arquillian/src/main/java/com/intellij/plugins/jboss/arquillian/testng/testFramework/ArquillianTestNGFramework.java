package com.intellij.plugins.jboss.arquillian.testng.testFramework;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.jarRepository.RepositoryAddLibraryAction;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.jboss.arquillian.ArquillianIcons;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.plugins.jboss.arquillian.testng.runConfiguration.ArquillianTestNGRunConfigurationType;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiClass;
import com.theoryinpractice.testng.TestNGFramework;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;

import javax.swing.*;

public final class ArquillianTestNGFramework extends TestNGFramework {
  @Override
  public @NotNull String getName() {
    return "Arquillian TestNG";
  }

  @Override
  protected String getMarkerClassFQName() {
    return "org.jboss.arquillian.testng.Arquillian";
  }

  @Override
  public FileTemplateDescriptor getTestClassFileTemplateDescriptor() {
    return new FileTemplateDescriptor("Arquillian TestNG Test Class.java");
  }

  @Override
  public Promise<Void> setupLibrary(Module module) {
    RepositoryLibraryDescription testngLibrary = RepositoryLibraryDescription.findDescription(
      "org.jboss.arquillian.testng",
      "arquillian-testng-container");
    return RepositoryAddLibraryAction.addLibraryToModule(testngLibrary, module);
  }

  @Override
  public @NotNull String getDefaultSuperClass() {
    return ArquillianConstants.TESTNG_ARQUILLIAN_CLASS;
  }

  @Override
  public @NotNull Icon getIcon() {
    return ArquillianIcons.Arquillian;
  }

  @Override
  public boolean isTestClass(PsiClass clazz, boolean canBePotential) {
    return ArquillianUtils.isTestngArquillianEnabled(clazz);
  }

  @Override
  public boolean isMyConfigurationType(ConfigurationType type) {
    return type instanceof ArquillianTestNGRunConfigurationType;
  }
}
