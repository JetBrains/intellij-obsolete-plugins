package com.jetbrains.plugins.compass.ruby;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.plugins.compass.CompassSettings;
import com.jetbrains.plugins.compass.CompassSettingsPanel;
import com.jetbrains.plugins.compass.CompassSettingsPanelImpl;
import com.jetbrains.plugins.compass.CompassUtil;
import icons.RubyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.gem.GemInfo;
import org.jetbrains.plugins.ruby.gem.util.GemSearchUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import static com.intellij.xml.util.XmlStringUtil.wrapInHtml;

public class CompassSettingsPanelForRubyModule implements CompassSettingsPanel {
  @NotNull
  private final Module myModule;
  @NotNull
  private JCheckBox myCompassEnabledCheckBox;
  private TextFieldWithHistoryWithBrowseButton myCompassConfigPathTextField;
  private JPanel myPanel;
  private JLabel myCompassGemLabel;
  private JBLabel myConfigPathLabel;
  private JBLabel myCompassGemTitleLabel;

  public CompassSettingsPanelForRubyModule(@NotNull Module module, @NotNull List<String> configFilesVariants, boolean fullMode) {
    myCompassConfigPathTextField.setPreferredSize(new Dimension(fullMode ? 100 : 400, -1));
    myModule = module;
    myCompassEnabledCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateUiComponents();
      }
    });
    final Project project = module.getProject();
    CompassSettingsPanelImpl.initCompassConfigPathTextField(project, configFilesVariants, myCompassConfigPathTextField);
  }

  @Override
  public boolean isModified(@Nullable CompassSettings compassSettings) {
    return isEnableOptionChanged(compassSettings) || isConfigFilePathChanged(compassSettings);
  }

  @Override
  public boolean isConfigFilePathChanged(@Nullable CompassSettings compassSettings) {
    return compassSettings != null && !compassSettings.getCompassConfigPath().equals(getCompassConfigPath());
  }

  @Override
  public boolean isExecutablePathChanged(@Nullable CompassSettings compassSettings) {
    return false;
  }

  @Override
  public boolean isEnableOptionChanged(@Nullable CompassSettings compassSettings) {
    return compassSettings != null && compassSettings.isCompassSupportEnabled() != isCompassSupportEnabled();
  }

  @Override
  public void apply(@Nullable CompassSettings compassSettings) {
    if (compassSettings != null) {
      compassSettings.setCompassSupportEnabled(isCompassSupportEnabled());
      compassSettings.setCompassConfigPath(getCompassConfigPath());
    }
  }

  @Override
  public void reset(@Nullable CompassSettings compassSettings) {
    if (compassSettings != null) {
      final boolean compassSupportEnabled = compassSettings.isCompassSupportEnabled();
      myCompassEnabledCheckBox.setSelected(compassSupportEnabled);
      myCompassConfigPathTextField.getChildComponent().setText(compassSettings.getCompassConfigPath());
    }

    if (compassSettings == null) {
      UIUtil.setEnabled(myPanel, false, true);
    }
    updateUiComponents();
  }

  public boolean isCompassSupportEnabled() {
    return myCompassEnabledCheckBox.isSelected();
  }

  @NotNull
  public String getCompassConfigPath() {
    return StringUtil.notNullize(myCompassConfigPathTextField.getChildComponent().getText());
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public void dispose() {

  }

  private void updateUiComponents() {
    myCompassGemTitleLabel.setEnabled(myCompassEnabledCheckBox.isSelected());
    myConfigPathLabel.setEnabled(myCompassEnabledCheckBox.isSelected());
    UIUtil.setEnabled(myCompassConfigPathTextField, myCompassEnabledCheckBox.isSelected(), true);
    GemInfo compassGem = GemSearchUtil.findGemEx(myModule, CompassUtil.COMPASS_GEM_NAME);
    if (compassGem != null) {
      myCompassGemLabel.setIcon(RubyIcons.Ruby.Ruby);
      myCompassGemLabel.setText(compassGem.getLibraryName());
    }
    else {
      myCompassGemLabel.setIcon(AllIcons.Actions.Lightning);
      myCompassGemLabel.setText(wrapInHtml("<font color='#" + ColorUtil.toHex(JBColor.RED) + "'><left>Cannot find compass gem in module</left></b></font>"));
    }
    myCompassGemLabel.setBorder(JBUI.Borders.empty(2));
    myCompassGemLabel.revalidate();
  }
}