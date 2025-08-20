package com.intellij.plugins.jboss.arquillian.testng.producer;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.plugins.jboss.arquillian.testng.runConfiguration.ArquillianTestNGRunConfigurationType;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiElement;
import com.theoryinpractice.testng.configuration.AbstractTestNGPackageConfigurationProducer;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import org.jetbrains.annotations.NotNull;

public final class ArquillianTestNGPackageConfigurationProducer extends AbstractTestNGPackageConfigurationProducer {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return ArquillianTestNGRunConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull TestNGConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    return super.setupConfigurationFromContext(configuration, context, sourceElement)
           && ArquillianUtils.isClassAvailableInContext(context, ArquillianConstants.TESTNG_ARQUILLIAN_CLASS);
  }
}
