package com.intellij.plugins.jboss.arquillian.junit.producer;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.AbstractAllInDirectoryConfigurationProducer;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.jboss.arquillian.constants.ArquillianConstants;
import com.intellij.plugins.jboss.arquillian.junit.runConfiguration.ArquillianJUnitRunConfigurationType;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public final class ArquillianAllInDirectoryConfigurationProducer extends AbstractAllInDirectoryConfigurationProducer {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return ArquillianJUnitRunConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull JUnitConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    return super.setupConfigurationFromContext(configuration, context, sourceElement)
           && ArquillianUtils.isClassAvailableInContext(context, ArquillianConstants.JUNIT_ARQUILLIAN_CLASS);
  }
}
