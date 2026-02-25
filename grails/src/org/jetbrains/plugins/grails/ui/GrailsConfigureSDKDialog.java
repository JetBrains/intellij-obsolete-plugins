// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.ui.GrailsApplicationCombobox;
import org.jetbrains.plugins.grails.sdk.GrailsSDKManager;
import org.jetbrains.plugins.grails.structure.Grails3Application;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.util.version.Range;
import org.jetbrains.plugins.grails.util.version.Version;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class GrailsConfigureSDKDialog extends DialogWrapper {

  private final GrailsSDKHomeForm mySDKHomeForm;

  private JPanel myCenterPanel;
  private GrailsApplicationCombobox myApplicationCombobox;
  private JPanel mySDKFormPanel;
  private JLabel myApplicationLabel;

  public GrailsConfigureSDKDialog(@NotNull Project project) {
    super(project, false, IdeModalityType.IDE);

    mySDKHomeForm = new GrailsSDKHomeForm();
    mySDKHomeForm.setChangedCallback(this::checkOkAction);
    mySDKFormPanel.add(mySDKHomeForm.getComponent(), BorderLayout.CENTER);

    myApplicationLabel.setLabelFor(myApplicationCombobox);
    myApplicationCombobox.disallowEmptySelection();
    myApplicationCombobox.setApplications(GrailsApplicationManager.getInstance(project).getApplications());
    myApplicationCombobox.addItemListener(e -> {
      GrailsApplication selectedApplication = myApplicationCombobox.getSelectedApplication();
      mySDKHomeForm.setVersionRange(getVersionRange(selectedApplication));
      mySDKHomeForm.setPath(GrailsSDKManager.getGrailsSdkPath(selectedApplication));
    });
    myApplicationCombobox.addItemListener(e -> checkOkAction());

    setTitle(GrailsBundle.message("action.Grails.ChangeSDK.text"));
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return myCenterPanel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return mySDKHomeForm.getPathComponent();
  }

  private void checkOkAction() {
    setOKActionEnabled(myApplicationCombobox.getSelectedApplication() != null && mySDKHomeForm.validate());
  }

  public @NotNull GrailsConfigureSDKDialog setGrailsApplication(@Nullable GrailsApplication application) {
    myApplicationCombobox.setSelectedApplication(application);
    return this;
  }

  @Override
  protected void doOKAction() {
    final GrailsApplication application = myApplicationCombobox.getSelectedApplication();
    assert application != null;
    GrailsSDKManager.setGrailsSDK(application, mySDKHomeForm.getSelectedSdk().getPath());
    super.doOKAction();
  }

  private static @Nullable Range<Version> getVersionRange(@Nullable GrailsApplication application) {
    if (application == null) return null;
    if (application instanceof OldGrailsApplication) {
      return Version.LESS_THAN_3;
    }
    else if (application instanceof Grails3Application) {
      return Version.AT_LEAST_3;
    }
    else {
      return null;
    }
  }
}
