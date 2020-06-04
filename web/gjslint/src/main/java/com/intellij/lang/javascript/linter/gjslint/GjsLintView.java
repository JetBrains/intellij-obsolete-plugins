package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.javascript.linter.JSLinterBaseView;
import com.intellij.lang.javascript.linter.JSLinterUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.ComponentsKt;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Sergey Simonchik
 */
public class GjsLintView extends JSLinterBaseView<GjsLintState> {

  private final TextFieldWithBrowseButton myLinterExeFileTextField;
  private final TextFieldWithBrowseButton myConfigFileTextField;
  private final boolean mySingleDialog;
  private final Disposable myDisposable = Disposer.newDisposable();

  public GjsLintView(@NotNull Project project, boolean singleDialog) {
    myLinterExeFileTextField = JSLinterUtil
      .createTextFieldWithBrowseButton(project,
                                       "Select Closure Linter executable file - " + GjsLintConfiguration.DEFAULT_EXE_FILE_BASE_NAME, myDisposable);
    myConfigFileTextField = JSLinterUtil.createTextFieldWithBrowseButton(project, "Select configuration file", myDisposable);
    mySingleDialog = singleDialog;
  }

  @Override
  public void disposeResources() {
    Disposer.dispose(myDisposable);
  }

  @NotNull
  private static JPanel createConfigFileInfoPanel() {
    String[] lines = new String[]{
      "--strict",
      "--jsdoc",
      "--custom_jsdoc_tags=my_custom_tag"
    };
    StringBuilder html = new StringBuilder();
    for (String line : lines) {
      html.append("<div style='padding-top:1px;'>").append(line).append("</div");
    }

    JEditorPane pane = createEditorPane();
    pane.setText(String.format("<html><head>%s</head><body>%s</body></html>",
                               UIUtil.getCssFontDeclaration(StartupUiUtil.getLabelFont(), UIUtil.getLabelForeground(), null, null),
                               html));

    JPanel panel = new JPanel(new BorderLayout(0, 5));
    panel.add(new JLabel("Example of a configuration file:"), BorderLayout.NORTH);
    JPanel wrapper = new JPanel(new BorderLayout(0, 0));
    wrapper.add(pane, BorderLayout.CENTER);
    wrapper.setBorder(new RoundedLineBorder(JBColor.lightGray, 5, 1));
    panel.add(wrapper, BorderLayout.CENTER);
    panel.add(new JLabel("Type \"gjslint --help\" for more information."), BorderLayout.SOUTH);
    return panel;
  }

  @NotNull
  private static JEditorPane createEditorPane() {
    JEditorPane pane = ComponentsKt.htmlComponent();
    pane.setOpaque(true);
    pane.setBackground(UIUtil.getPanelBackground());
    pane.setForeground(UIUtil.getLabelForeground());
    pane.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
    return pane;
  }

  @Nullable
  @Override
  protected Component createTopRightComponent() {
    HyperlinkLabel usageLink = SwingHelper.createWebHyperlink(
      "How to Use Closure Linter",
      "https://developers.google.com/closure/utilities/docs/linter_howto"
    );
    JPanel usageLinkPanel = new JPanel(new BorderLayout(0, 0));
    usageLinkPanel.add(usageLink, BorderLayout.EAST);
    return usageLinkPanel;
  }

  @NotNull
  @Override
  protected Component createCenterComponent() {
    JPanel centerPanel = new JPanel(new GridBagLayout());
    centerPanel.add(new JLabel("Closure Linter executable file:"), new GridBagConstraints(
      0, 0,
      1, 1,
      0.0, 0.0,
      GridBagConstraints.EAST,
      GridBagConstraints.NONE,
      new Insets(0, 0, 4, 6),
      0, 0
    ));
    centerPanel.add(myLinterExeFileTextField, new GridBagConstraints(
      1, 0,
      1, 1,
      1.0, 0.0,
      GridBagConstraints.WEST,
      GridBagConstraints.HORIZONTAL,
      new Insets(0, 0, 4, 0),
      0, 0
    ));
    centerPanel.add(new JLabel("Configuration file:"), new GridBagConstraints(
      0, 1,
      1, 1,
      0.0, 0.0,
      GridBagConstraints.EAST,
      GridBagConstraints.NONE,
      new Insets(0, 0, 0, 6),
      0, 0
    ));
    centerPanel.add(myConfigFileTextField, new GridBagConstraints(
      1, 1,
      1, 1,
      1.0, 0.0,
      GridBagConstraints.WEST,
      GridBagConstraints.HORIZONTAL,
      new Insets(0, 0, 0, 0),
      0, 0
    ));
    JPanel configFileInfoPanel = createConfigFileInfoPanel();
    centerPanel.add(configFileInfoPanel, new GridBagConstraints(
      0, 2,
      2, 1,
      1.0, 0.0,
      GridBagConstraints.WEST,
      GridBagConstraints.HORIZONTAL,
      new Insets(15, 20, 0, 0),
      0, 0
    ));
    centerPanel.add(new JPanel(), new GridBagConstraints(
      0, 3,
      2, 1,
      1.0, 1.0,
      GridBagConstraints.WEST,
      GridBagConstraints.BOTH,
      new Insets(0, 0, 0, 0),
      0, 0
    ));
    centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    return centerPanel;
  }

  @NotNull
  @Override
  protected GjsLintState getState() {
    return new GjsLintState.Builder()
      .setLinterExePath(myLinterExeFileTextField.getText())
      .setConfigFilePath(myConfigFileTextField.getText())
      .build();
  }

  @Override
  protected void setState(@NotNull GjsLintState state) {
    myLinterExeFileTextField.setText(state.getLinterExePath());
    myConfigFileTextField.setText(state.getConfigFilePath());
    if (mySingleDialog) {
      ApplicationManager.getApplication().invokeLater(() -> {
        SwingHelper.setPreferredWidthToFitText(myLinterExeFileTextField.getTextField());
        SwingHelper.setPreferredWidthToFitText(myConfigFileTextField.getTextField());
        DialogWrapper dialogWrapper = DialogWrapper.findInstance(myLinterExeFileTextField);
        if (dialogWrapper != null) {
          SwingHelper.adjustDialogSizeToFitPreferredSize(dialogWrapper);
        }
      }, ModalityState.any());
    }
  }
}
