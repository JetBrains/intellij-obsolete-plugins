package com.intellij.struts.facet.ui;

import com.intellij.util.ui.ThreeStateCheckBox;

import javax.swing.*;

/**
 * @author nik
 */
public class StrutsFacetCommonSettingsPanel {
  private JPanel myMainPanel;
  private ThreeStateCheckBox myDisablePropertyKeysValidationCheckBox;

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  public ThreeStateCheckBox getDisablePropertyKeysValidationCheckBox() {
    return myDisablePropertyKeysValidationCheckBox;
  }
}
