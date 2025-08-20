package com.intellij.plugins.jboss.arquillian.runConfiguration;

import com.intellij.openapi.project.Project;

public interface ArquillianTestFrameworkRunConfiguration {
  ArquillianRunConfiguration getRunConfiguration();

  Project getProject();
}
