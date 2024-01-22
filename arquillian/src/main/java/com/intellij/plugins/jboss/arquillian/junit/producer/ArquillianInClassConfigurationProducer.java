package com.intellij.plugins.jboss.arquillian.junit.producer;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.TestInClassConfigurationProducer;
import com.intellij.execution.testframework.AbstractInClassConfigurationProducer;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.jboss.arquillian.junit.runConfiguration.ArquillianJUnitRunConfigurationType;
import com.intellij.plugins.jboss.arquillian.utils.ArquillianUtils;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public final class ArquillianInClassConfigurationProducer extends AbstractInClassConfigurationProducer<JUnitConfiguration> {
  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return ArquillianJUnitRunConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
    return other.isProducedBy(TestInClassConfigurationProducer.class);
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull JUnitConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    if (!ArquillianUtils.isJunitArquillianEnabled(context)) {
      return false;
    }
    return super.setupConfigurationFromContext(configuration, context, sourceElement);
  }
}
