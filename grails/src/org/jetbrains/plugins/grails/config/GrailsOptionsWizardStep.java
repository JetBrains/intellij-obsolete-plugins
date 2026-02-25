// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.ui.GrailsSDKHomeForm;
import org.jetbrains.plugins.grails.util.version.Version;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class GrailsOptionsWizardStep extends ModuleWizardStep {

  private final @NotNull WizardContext myContext;
  private final @NotNull GrailsModuleBuilder myBuilder;
  private final @NotNull GradleParentProjectForm myParentProjectForm;
  private final GrailsSDKHomeForm mySelectSDKHomeForm;

  private JPanel myRootPanel;
  private JPanel myParentProjectPanel;
  private JPanel mySDKPanel;
  private JBLabel myOptionsLabel;
  private JBTextField myOptions;
  private JBRadioButton myCreateApp;
  private JBRadioButton myCreatePlugin;
  private JPanel myOptionsPanel;

  public GrailsOptionsWizardStep(@NotNull WizardContext context, @NotNull GrailsModuleBuilder builder) {
    myContext = context;
    myBuilder = builder;

    myParentProjectForm = new GradleParentProjectForm(context, data -> updateComponents());
    mySelectSDKHomeForm = new GrailsSDKHomeForm();
    mySelectSDKHomeForm.setChangedCallback(this::updateComponents);
    mySelectSDKHomeForm.setPath(PropertiesComponent.getInstance().getValue(GrailsConstants.GRAILS_LAST_SELECTED_SDK));

    myParentProjectPanel.add(myParentProjectForm.getComponent());
    mySDKPanel.add(mySelectSDKHomeForm.getComponent());

    myOptionsLabel.setLabelFor(myOptions);
  }

  @Override
  public JComponent getComponent() {
    return myRootPanel;
  }

  @Override
  public void updateDataModel() {
    myBuilder.setGrailsSDKHome(mySelectSDKHomeForm.getSelectedSdk());
    myBuilder.setParentProject(myParentProjectForm.getParentProject());
    myBuilder.setCreateChoice(getCreationChoice());
    myBuilder.setOptions(myOptions.getText());
    myBuilder.setCreatingNewProject(myContext.isCreatingNewProject());
  }

  @Override
  public boolean validate() throws ConfigurationException {
    return mySelectSDKHomeForm.validate();
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(myParentProjectForm);
  }

  private void updateComponents() {
    myParentProjectForm.updateComponents();
    myOptionsPanel.setVisible(
      mySelectSDKHomeForm.validate() && mySelectSDKHomeForm.getSelectedSdk().getVersion().isAtLeast(Version.GRAILS_3_0)
    );
  }

  private @NotNull String getCreationChoice() {
    if (myCreateApp.isSelected()) {
      return "create-app";
    }
    else if (myCreatePlugin.isSelected()) {
      return "create-plugin";
    }
    else {
      throw new AssertionError("No selection");
    }
  }
}
