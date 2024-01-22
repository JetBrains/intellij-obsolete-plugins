package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainerModel;
import org.jetbrains.annotations.NotNull;

class ItemNode extends MasterDetailsComponent.MyNode {
  @NotNull private final ArquillianContainerModel model;

  ItemNode(@NotNull Project project, @NotNull ArquillianContainerModel model, Runnable treeUpdater) {
    super(new ItemNodeConfigurable(project, model, treeUpdater));
    this.model = model;
  }

  @NotNull
  public ArquillianContainerModel getModel() {
    return model;
  }
}
