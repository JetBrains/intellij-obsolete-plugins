package com.intellij.plugins.jboss.arquillian.runConfiguration;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainerState;
import com.intellij.plugins.jboss.arquillian.configuration.persistent.ArquillianContainersManager;
import com.intellij.plugins.jboss.arquillian.configuration.ui.ArquillianSettingsConfigurable;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArquillianConfigurationPanel extends SettingsEditor<ArquillianRunConfiguration> {
  private final Project project;

  private ButtonGroup buttonGroup;
  private List<JBRadioButton> buttons;
  private JBRadioButton selectedButton;
  private String selectedName;

  private JPanel mainPanel;
  private JPanel frameworksPanel;
  private JButton configureButton;

  public ArquillianConfigurationPanel(final Project project) {
    this.project = project;
    configureButton.addActionListener(e -> {
      ArquillianSettingsConfigurable configurable = new ArquillianSettingsConfigurable(project);
      if (ShowSettingsUtil.getInstance().editConfigurable(project, configurable)) {
        refreshContainerList();
        setSelection(selectedName);
      }
    });
  }

  private void setSelection(String name) {
    for (JBRadioButton button : buttons) {
      button.setSelected(false);
    }
    selectedName = name;
    selectedButton = JBIterable.from(buttons).filter(button -> button.getText().equals(selectedName)).first();
    if (selectedButton != null) {
      selectedButton.setSelected(true);
    }
  }

  @Override
  public void resetEditorFrom(final @NotNull ArquillianRunConfiguration runConfiguration) {
    setSelection(runConfiguration.getContainerStateName());
  }

  @Override
  public void applyEditorTo(@NotNull ArquillianRunConfiguration runConfiguration) {
    runConfiguration.setContainerStateName(selectedName);
  }

  @Override
  public @NotNull JComponent createEditor() {
    return mainPanel;
  }

  private void refreshContainerList() {
    frameworksPanel.removeAll();
    buttonGroup = new ButtonGroup();
    List<ArquillianContainerState> states =
      new ArrayList<>(ArquillianContainersManager.getInstance(project).getState().containers);
    Collections.sort(states);

    buttons = ContainerUtil.map(states, containerState -> {
      JBRadioButton button = new JBRadioButton(containerState.getName());
      button.setHorizontalAlignment(SwingConstants.LEFT);
      frameworksPanel.add(button);
      buttonGroup.add(button);
      button.addChangeListener(e -> {
        if (button.isSelected()) {
          selectedButton = button;
          selectedName = button.getText();
        }
      });
      return button;
    });
  }

  private void createUIComponents() {
    frameworksPanel = new JPanel();
    frameworksPanel.setLayout(new BoxLayout(frameworksPanel, BoxLayout.Y_AXIS));
    refreshContainerList();
  }
}
