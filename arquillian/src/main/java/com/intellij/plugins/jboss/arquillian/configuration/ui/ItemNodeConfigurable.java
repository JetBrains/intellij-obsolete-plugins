package com.intellij.plugins.jboss.arquillian.configuration.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.plugins.jboss.arquillian.configuration.model.ArquillianContainerModel;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class ItemNodeConfigurable extends NamedConfigurable<ArquillianContainerModel> {
  @NotNull final private Project project;
  @NotNull private final ArquillianContainerModel model;
  @NotNull private final ArquillianContainerState initialState;


  ItemNodeConfigurable(@NotNull Project project, @NotNull ArquillianContainerModel model, Runnable treeUpdater) {
    super(true, treeUpdater);
    this.project = project;
    this.model = model;
    this.initialState = model.getCurrentState();
  }

  @Override
  public ArquillianContainerModel getEditableObject() {
    return model;
  }

  @Override
  public String getBannerSlogan() {
    return null;
  }

  @Override
  public JComponent createOptionsPanel() {
    return model.getDescription().createSettingsPanel(project, model);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return model.getName();
  }

  @Override
  public void setDisplayName(String name) {
    model.setName(name);
  }

  @Override
  public boolean isModified() {
    return model.hasChanges(initialState);
  }

  @Override
  public void apply() throws ConfigurationException {

  }

  @Nullable
  @Override
  public Icon getIcon(boolean expanded) {
    return model.getIcon();
  }
}
