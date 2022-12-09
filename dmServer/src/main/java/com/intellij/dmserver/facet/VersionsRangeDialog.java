package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.VersionUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.VersionRange;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.List;

public class VersionsRangeDialog extends DialogWrapper {
  private JPanel myMainPanel;
  private JRadioButton myAnyRadioButton;
  private JRadioButton myModuleRadioButton;
  private JRadioButton myCustomRadioButton;
  private JTextField myCustomTextField;

  private List<NestedUnitIdentity> myUnitIdentities;

  public VersionsRangeDialog(Project project, List<NestedUnitIdentity> unitIdentities) {
    super(project);
    setTitle(DmServerBundle.message("VersionsRangeDialog.title"));

    ButtonGroup radioGroup = new ButtonGroup();
    radioGroup.add(myAnyRadioButton);
    radioGroup.add(myModuleRadioButton);
    radioGroup.add(myCustomRadioButton);
    ActionListener radioListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateUI();
      }
    };
    Enumeration<AbstractButton> radioEnumeration = radioGroup.getElements();
    while (radioEnumeration.hasMoreElements()) {
      radioEnumeration.nextElement().addActionListener(radioListener);
    }

    initContent(unitIdentities);

    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  private void initContent(List<NestedUnitIdentity> unitIdentities) {
    myUnitIdentities = unitIdentities;
    myAnyRadioButton.setSelected(true);
    myCustomTextField.setText("");
    updateUI();
  }

  private void updateUI() {
    myCustomTextField.setEnabled(myCustomRadioButton.isSelected());
  }

  @Override
  protected void doOKAction() {
    for (NestedUnitIdentity unitIdentity : myUnitIdentities) {
      if (myAnyRadioButton.isSelected()) {
        unitIdentity.setVersionRange(VersionUtils.emptyRange.toString());
      }
      else if (myModuleRadioButton.isSelected()) {
        Module module = unitIdentity.getModule();
        String moduleVersion = module == null ? null : findModuleVersion(module);
        String versionRangeText = VersionUtils.version2range(VersionUtils.loadVersion(moduleVersion)).toString();
        unitIdentity.setVersionRange(versionRangeText);
      }
      else if (myCustomRadioButton.isSelected()) {
        VersionRange versionRange = VersionUtils.parseVersionRange(myCustomTextField.getText());
        if (versionRange == null) {
          setErrorText(DmServerBundle.message("VersionsRangeDialog.error.version.range.format.not.matched"), myCustomTextField);
          return;
        }
        else {
          unitIdentity.setVersionRange(versionRange.toString());
        }
      }
    }
    super.doOKAction();
  }

  @Nullable
  private static String findModuleVersion(@NotNull Module module) {
    DMUnitDescriptor unitDescriptor = DMUnitDescriptorProvider.getInstance().processModule(module);
    return unitDescriptor == null ? null : unitDescriptor.getVersion();
  }
}
