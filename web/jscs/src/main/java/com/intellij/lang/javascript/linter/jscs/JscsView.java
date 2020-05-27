package com.intellij.lang.javascript.linter.jscs;

import com.intellij.ide.BrowserUtil;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.lang.javascript.linter.JSLinterBaseView;
import com.intellij.lang.javascript.linter.JSLinterUtil;
import com.intellij.lang.javascript.linter.NodeModuleConfigurationView;
import com.intellij.lang.javascript.linter.jscs.config.JscsConfigFileType;
import com.intellij.lang.javascript.linter.ui.JSLinterConfigFileTexts;
import com.intellij.lang.javascript.linter.ui.JSLinterConfigFileView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author by Irina.Chernushina on 9/22/2014.
 */
public class JscsView extends JSLinterBaseView<JscsState> {
  private static final JSLinterConfigFileTexts CONFIG_TEXTS = getConfigTexts();
  @NonNls
  private static final String PACKAGE_NAME = "jscs";

  private final JSLinterConfigFileView myConfigFileView;
  private final NodeModuleConfigurationView myNodeModuleConfigurationView;
  private final ComponentWithBrowseButton<ComboBox> myPresetWithHelp;
  private final JPanel myPresetHint;

  public JscsView(@NotNull final Project project, boolean isFullModeDialog) {
    super(isFullModeDialog);
    myConfigFileView = new JSLinterConfigFileView(project, CONFIG_TEXTS, JscsConfigFileType.INSTANCE);
    myNodeModuleConfigurationView = new NodeModuleConfigurationView(project, PACKAGE_NAME, null);
    myPresetWithHelp = createPresetWithHelpButton();

    final JEditorPane presetHintComp = JSLinterUtil.createHtmlViewer(JscsBundle.message("jscs.configurable.preset.hint.text"), UIUtil.getTitledBorderFont());
    myPresetHint = SwingHelper.wrapWithHorizontalStretch(presetHintComp);
  }

  @NotNull
  @Override
  protected Component createCenterComponent() {
    JPanel panel = FormBuilder.createFormBuilder()
      .setHorizontalGap(UIUtil.DEFAULT_HGAP)
      .setVerticalGap(UIUtil.DEFAULT_VGAP)
      .setFormLeftIndent(UIUtil.DEFAULT_HGAP)
      .addLabeledComponent(NodeJsInterpreterField.getLabelTextForComponent(), myNodeModuleConfigurationView.getNodeInterpreterField())
      .addLabeledComponent(JscsBundle.message("jscs.configurable.label.package.path"), myNodeModuleConfigurationView.getPackageField())
      .addComponent(myConfigFileView.getComponent())
      .addLabeledComponent(JscsBundle.message("jscs.configurable.label.preset.label.text"), myPresetWithHelp)
      .addLabeledComponent("", myPresetHint)
      .getPanel();
    final JPanel centerPanel = SwingHelper.wrapWithHorizontalStretch(panel);
    centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    return centerPanel;
  }

  public static ComponentWithBrowseButton<ComboBox> createPresetWithHelpButton() {
    final ComboBox box = new ComboBox();
    final ComponentWithBrowseButton<ComboBox> comp = new ComponentWithBrowseButton<>(box, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final Object item = box.getSelectedItem();
        final String url;
        if (item instanceof JscsPreset) {
          url = ((JscsPreset)item).getDescribeUrl();
        }
        else {
          url = JscsPreset.COMMON_DESCRIPTION;
        }
        BrowserUtil.browse(url);
      }
    });
    FixedSizeButton uiHelpButton = comp.getButton();
    uiHelpButton.setToolTipText(JscsBundle.message("jscs.configurable.preset.show.in.browser.tooltip"));
    uiHelpButton.setIcon(UIUtil.getBalloonInformationIcon());
    uiHelpButton.setHorizontalAlignment(SwingConstants.CENTER);
    uiHelpButton.setVerticalAlignment(SwingConstants.CENTER);

    fillPresets(box);
    return comp;
  }

  @Override
  protected void handleEnableStatusChanged(boolean enabled) {
    myConfigFileView.onEnabledStateChanged(enabled);
    myPresetHint.setForeground(UIUtil.getLabelDisabledForeground());
  }

  @NotNull
  @Override
  protected JscsState getState() {
    final JscsState.Builder builder = new JscsState.Builder()
      .setNodePath(myNodeModuleConfigurationView.getNodeInterpreterField().getInterpreterRef())
      .setNodePackage(myNodeModuleConfigurationView.getPackageField().getSelected())
      .setCustomConfigFileUsed(myConfigFileView.isCustomConfigFileUsed())
      .setCustomConfigFilePath(myConfigFileView.getCustomConfigFilePath());
    final JscsPreset item = (JscsPreset) myPresetWithHelp.getChildComponent().getSelectedItem();
    if (item != null) {
      builder.setPreset(item);
    }
    return builder.build();
  }

  @Override
  protected void setState(@NotNull JscsState state) {
    myNodeModuleConfigurationView.getNodeInterpreterField().setInterpreterRef(state.getInterpreterRef());
    myNodeModuleConfigurationView.getPackageField().setSelected(new NodePackage(state.getPackagePath()));

    myConfigFileView.setCustomConfigFileUsed(state.isCustomConfigFileUsed());
    myConfigFileView.setCustomConfigFilePath(state.getCustomConfigFilePath());
    final JscsPreset preset = state.getPreset();
    if (preset != null) {
      myPresetWithHelp.getChildComponent().setSelectedItem(preset);
    } else {
      myPresetWithHelp.getChildComponent().setSelectedIndex(0);
    }

    resizeOnSeparateDialog();
  }

  private void resizeOnSeparateDialog() {
    if (isFullModeDialog()) {
      myNodeModuleConfigurationView.setPreferredWidthToComponents();
      myConfigFileView.setPreferredWidthToComponents();
    }
  }

  private static JSLinterConfigFileTexts getConfigTexts() {
    return new JSLinterConfigFileTexts(JscsBundle.message("jscs.configurable.look.for.config.files"),
                                       JscsBundle.message("jscs.configurable.label.default.config.description"),
                                       JscsBundle.message("jscs.configurable.select.config.file.text"));
  }

  private static void fillPresets(final ComboBox<JscsPreset> comboBox) {
    List<JscsPreset> items = new ArrayList<>(JscsPreset.values().length + 1);
    Collections.addAll(items, JscsPreset.values());
    Collections.sort(items);
    items.add(0, null);
    comboBox.setModel(new CollectionComboBoxModel<>(items));
    comboBox.setRenderer(SimpleListCellRenderer.create("", value -> value.getDisplayName()));
  }
}
