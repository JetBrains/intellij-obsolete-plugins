package com.intellij.plugins.jboss.arquillian.testng.producer;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.testframework.AbstractInClassConfigurationProducer;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.jboss.arquillian.testng.runConfiguration.ArquillianTestNGRunConfigurationType;
import com.intellij.plugins.jboss.arquillian.testng.utils.ArquillianTestngUtils;
import com.intellij.psi.PsiElement;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGInClassConfigurationProducer;
import org.jetbrains.annotations.NotNull;

public final class ArquillianTestNGInClassConfigurationProducer extends AbstractInClassConfigurationProducer<TestNGConfiguration> {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return ArquillianTestNGRunConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
    return other.isProducedBy(TestNGInClassConfigurationProducer.class);
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull TestNGConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    if (!ArquillianTestngUtils.isTestngArquillianEnabled(context)) {
      return false;
    }
    return super.setupConfigurationFromContext(configuration, context, sourceElement);
  }
}
