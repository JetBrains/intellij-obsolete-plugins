package com.intellij.dmserver.facet;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DMModuleConfigFacetSettingsPanel implements DMModuleFacetSettingsPanel<DMConfigFacetConfiguration> {

  private JTextField myNameTextField;
  private JPanel myMainPanel;

  private Module myConfiguredModule;

  @Override
  public void init(@Nullable Project project,
                   @Nullable Module configuredModule,
                   @NotNull ModulesProvider modulesProvider,
                   @NotNull Disposable parentDisposable) {
    myConfiguredModule = configuredModule;
  }

  @Override
  @NotNull
  public JPanel getMainPanel() {
    return myMainPanel;
  }

  @Override
  public void load(@NotNull DMConfigFacetConfiguration configuration) {
    myNameTextField.setText(configuration.getName(myConfiguredModule));
  }

  @Override
  public void apply(@NotNull DMConfigFacetConfiguration configuration) {
    save(configuration);
  }

  @Override
  public void save(@NotNull DMConfigFacetConfiguration configuration) {
    configuration.setName(myNameTextField.getText());
  }

  @Override
  public void updateEnablement() {

  }
}
