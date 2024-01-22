package com.intellij.plugins.jboss.arquillian.configuration.container;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.ArquillianBundle;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class ArquillianContainerManual extends ArquillianContainerImpl {

  public static final String MANUAL_CONTAINER_ID = "manual";

  public ArquillianContainerManual() {
    super(MANUAL_CONTAINER_ID, ArquillianBundle.message("manual.container.configuration"), null);
  }

  @NotNull
  @Override
  public Scope getScope() {
    return Scope.Manual;
  }

  @NotNull
  @Override
  public ArquillianContainerState createDefaultState(Project project, String name) {
    return new ArquillianContainerState(getId(), name, Collections.emptyList());
  }

  @Override
  public boolean canChangeDependencyList() {
    return true;
  }
}
