package com.intellij.lang.puppet.settings.configurable;

import com.intellij.lang.puppet.PuppetBundle;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.lang.puppet.util.PuppetConfigurationUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

public class PuppetConfigurable implements SearchableConfigurable {
  private final @NotNull PuppetProjectConfiguration myProjectConfiguration;
  private @Nullable PuppetConfigurableForm myConfigurableForm = null;
  private final @NotNull Project myProject;

  public PuppetConfigurable(@NotNull Project project) {
    myProject = project;
    myProjectConfiguration = PuppetProjectConfiguration.getInstance(project);
  }

  private @NotNull PuppetConfigurableForm getForm() {
    if (myConfigurableForm == null) {
      myConfigurableForm = new PuppetConfigurableForm();
    }
    return myConfigurableForm;
  }

  @Override
  public @NotNull String getId() {
    return "Settings.Puppet";
  }

  @Override
  public @Nls String getDisplayName() {
    return PuppetBundle.message("settings.puppet.configurable.display.name");
  }

  @Override
  public @Nullable String getHelpTopic() {
    return getId();
  }

  @Override
  public @Nullable JComponent createComponent() {
    return getForm().getPanel();
  }

  @Override
  public boolean isModified() {
    if (myConfigurableForm == null) {
      return false;
    }
    return myConfigurableForm.getLanguageVersionModel().getSelectedItem() != myProjectConfiguration.getLanguageVersion() ||
           !StringUtil.equals(myConfigurableForm.getPathToLibrarian().getText(), myProjectConfiguration.getLibrarianPath())
      ;
  }

  @Override
  public void apply() throws ConfigurationException {
    if (myConfigurableForm == null) {
      return;
    }
    if (myConfigurableForm.getLanguageVersionModel().getSelectedItem() != myProjectConfiguration.getLanguageVersion()) {
      myProjectConfiguration.setLanguageVersion(myConfigurableForm.getLanguageVersionModel().getSelectedItem());
      PuppetConfigurationUtil.reparsePuppetFiles(myProject);
    }
    myProjectConfiguration.setLibrarianPath(myConfigurableForm.getPathToLibrarian().getText());
  }

  @Override
  public void reset() {
    if (myConfigurableForm == null) {
      return;
    }
    myConfigurableForm.getLanguageVersionModel().setSelectedItem(myProjectConfiguration.getLanguageVersion());
    myConfigurableForm.getPathToLibrarian().setText(myProjectConfiguration.getLibrarianPath());
  }

  @Override
  public void disposeUIResources() {
    myConfigurableForm = null;
  }
}
