package com.intellij.lang.javascript.linter.jslint;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.intellij.lang.javascript.JavaScriptBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.*;
import com.intellij.webcore.ui.CustomFocusTraversalPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

public class JSLintOptionsView {

  private static final JSLintOption[][] TOLERATE_CHECKBOX_OPTIONS_LAYOUT = {
    {JSLintOption.BITWISE  },
    {JSLintOption.EVAL     },
    {JSLintOption.FOR      },
    {JSLintOption.MULTIVAR },
    {JSLintOption.SINGLE   },
    {JSLintOption.THIS     },
    {JSLintOption.WHITE    },
  };

  private static final JSLintOption[][] ASSUME_CHECKBOX_OPTIONS_LAYOUT = {
    {JSLintOption.DEVEL   },
    {JSLintOption.ES6     },
    {JSLintOption.BROWSER },
    {JSLintOption.COUCH   },
    {JSLintOption.NODE    },
  };

  private static final JSLintOption[] TEXT_FIELD_OPTIONS_LAYOUT = {
    JSLintOption.MAXLEN,
    JSLintOption.MAXERR,
    JSLintOption.GLOBALS
  };

  static {
    validate();
  }

  private static void validateLayout(JSLintOption[] @NotNull [] layout,
                                     @NotNull Set<JSLintOption.Type> expectedOptionTypes,
                                     @NotNull Set<JSLintOption> alreadyAddedOptions) {
    final int rowSize = layout[0].length;
    for (int i = 0; i < layout.length; i++) {
      JSLintOption[] optionsRow = layout[i];
      if (optionsRow.length != rowSize) {
        throw new RuntimeException("Row " + i + " is expected to have " + rowSize + " element!");
      }
      for (JSLintOption option : optionsRow) {
        if (option != null) {
          if (alreadyAddedOptions.contains(option)) {
            throw new RuntimeException("Duplicate option: " + option + "!");
          }
          if (!expectedOptionTypes.contains(option.getType())) {
            throw new RuntimeException("Expected types: " + expectedOptionTypes + ", found type: " + option.getType());
          }
          alreadyAddedOptions.add(option);
        }
      }
    }
  }

  private static void validate() {
    Set<JSLintOption> usedOptions = EnumSet.noneOf(JSLintOption.class);
    validateLayout(TOLERATE_CHECKBOX_OPTIONS_LAYOUT, EnumSet.of(JSLintOption.Type.BOOLEAN), usedOptions);
    validateLayout(ASSUME_CHECKBOX_OPTIONS_LAYOUT, EnumSet.of(JSLintOption.Type.BOOLEAN), usedOptions);
    validateLayout(new JSLintOption[][] {TEXT_FIELD_OPTIONS_LAYOUT},
                   EnumSet.of(JSLintOption.Type.STRING, JSLintOption.Type.INTEGER),
                   usedOptions);
    Set<JSLintOption> allOptions = EnumSet.allOf(JSLintOption.class);
    if (!usedOptions.equals(allOptions)) {
      allOptions.removeAll(usedOptions);
      throw new RuntimeException("used options != all options, unused options are " + allOptions);
    }
  }

  private final JPanel myOptionsComponent;
  private final ImmutableMap<JSLintOption, JComponent> myComponentByOptionMap;
  private final JCheckBox myJsonCheckbox = new JCheckBox("JSON");
  private final List<Runnable> myUpdatingJobs = new ArrayList<>();

  public JSLintOptionsView() {
    Map<JSLintOption, JComponent> componentByOptionMap = new HashMap<>();

    JComponent tolerateCheckboxesPanel = createCheckboxOptionsPanel(TOLERATE_CHECKBOX_OPTIONS_LAYOUT, "Tolerate", componentByOptionMap);
    JComponent assumeCheckboxesPanel = createCheckboxOptionsPanel(ASSUME_CHECKBOX_OPTIONS_LAYOUT, "Assume", componentByOptionMap);
    JPanel textFieldsPanel = createTextOptionsPanel(componentByOptionMap);

    JPanel northPanel = SwingHelper.newHorizontalPanel(
      Component.TOP_ALIGNMENT,
      tolerateCheckboxesPanel,
      Box.createHorizontalStrut(JBUIScale.scale(20)),
      assumeCheckboxesPanel
    );
    JPanel southPanel = createOtherFileTypesPanel(componentByOptionMap);
    myOptionsComponent = SwingHelper.wrapWithHorizontalStretch(
      SwingHelper.newLeftAlignedVerticalPanel(
      northPanel,
      Box.createVerticalStrut(JBUIScale.scale(5)),
      textFieldsPanel,
      Box.createVerticalStrut(JBUIScale.scale(15)),
      southPanel
    ));
    myOptionsComponent.setBorder(new JBEmptyBorder(0, 0, 5, 0));

    myComponentByOptionMap = ImmutableMap.copyOf(componentByOptionMap);
  }

  @NotNull
  private JPanel createOtherFileTypesPanel(@NotNull Map<JSLintOption, JComponent> componentByOptionMap) {
    JPanel jsonPanel = createFileType(myJsonCheckbox, new JSLintOption[][]{}, componentByOptionMap);
    JPanel panel = SwingHelper.newHorizontalPanel(Component.TOP_ALIGNMENT, jsonPanel);
    panel.setBorder(IdeBorderFactory.createTitledBorder(JSLintBundle.message("border.title.validate.also")));
    return panel;
  }

  @NotNull
  private JPanel createFileType(@NotNull final JCheckBox titleCheckBox,
                                       JSLintOption[] @NotNull [] options,
                                       @NotNull Map<JSLintOption, JComponent> componentByOptionMap) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(titleCheckBox, BorderLayout.NORTH);
    if (options.length > 0) {
      final JPanel center = createCheckboxOptionsPanel(options, null, componentByOptionMap);
      center.setBorder(BorderFactory.createEmptyBorder(0, IdeBorderFactory.TITLED_BORDER_INDENT, 0, 0));
      panel.add(center, BorderLayout.CENTER);
      final Runnable r = () -> UIUtil.setEnabled(center, titleCheckBox.isSelected(), true);
      titleCheckBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          r.run();
        }
      });
      myUpdatingJobs.add(r);
    }
    return panel;
  }

  private static JPanel createCheckboxOptionsPanel(JSLintOption[] @NotNull [] layout,
                                                   @Nullable String title,
                                                   @NotNull Map<JSLintOption, JComponent> componentByOptionMap) {
    final int columnCount = layout[0].length;
    JPanel checkboxesPanel = new JPanel(new GridBagLayout());
    if (title != null) {
      checkboxesPanel.setBorder(IdeBorderFactory.createTitledBorder(title, false));
    }
    CustomFocusTraversalPolicy focusPolicy = CustomFocusTraversalPolicy.createAndInstallOn(checkboxesPanel);
    Integer checkboxHeight = null;
    for (int j = 0; j < columnCount; j++) {
      for (int i = 0; i < layout.length; i++) {
        JSLintOption option = layout[i][j];
        if (option != null) {
          if (option.getType() != JSLintOption.Type.BOOLEAN) {
            throw new RuntimeException("Boolean type is expected: " + option + "!");
          }
          String description = option.getDescription();
          if (title != null) {
            description = StringUtil.trimStart(description, title + " ").trim();
          }
          JCheckBox checkBox = new JCheckBox(description);
          checkBox.setToolTipText(option.getMeaning());
          componentByOptionMap.put(option, checkBox);
          focusPolicy.addNextComponentInTraversalOrder(checkBox);
          if (checkboxHeight == null) {
            checkboxHeight = checkBox.getPreferredSize().height;
          }
          int topInsets = 1;
          if (columnCount == 1 && i > 0 && layout[i - 1][j] == null) {
            topInsets += checkboxHeight + 1;
          }
          checkboxesPanel.add(checkBox, new GridBagConstraints(
            j, i,
            1, 1,
            1.0, 0.0,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.HORIZONTAL,
            JBUI.insets(topInsets, 0, 0, 1),
            0, 0
          ));
        }
      }
    }
    checkboxesPanel.add(new JPanel(), new GridBagConstraints(
      0, layout.length,
      columnCount, 1,
      1.0, 1.0,
      GridBagConstraints.NORTHWEST,
      GridBagConstraints.BOTH,
      JBUI.emptyInsets(),
      0, 0
    ));
    return checkboxesPanel;
  }

  @NotNull
  private static JPanel createTextOptionsPanel(@NotNull Map<JSLintOption, JComponent> componentByOptionMap) {
    FormBuilder builder = FormBuilder.createFormBuilder().setAlignLabelOnRight(true);
    for (JSLintOption option : TEXT_FIELD_OPTIONS_LAYOUT) {
      JLabel label = new JLabel(option.getDescription() + ":");
      label.setToolTipText(option.getMeaning());
      final JTextComponent textComponent;
      final boolean stringFieldType = option.getType() == JSLintOption.Type.STRING;
      if (stringFieldType) {
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setRows(3);
        textComponent = textArea;
      } else {
        JFormattedTextField formattedTextField = new JFormattedTextField();
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        formatter.setParseIntegerOnly(true);
        formattedTextField.setFormatterFactory(new DefaultFormatterFactory(new NumberFormatter(formatter)));
        formattedTextField.setColumns(6);
        textComponent = formattedTextField;
      }
      textComponent.setDisabledTextColor(UIUtil.getLabelDisabledForeground());
      JComponent comp = textComponent;
      if (textComponent instanceof JTextArea) {
        comp = new JBScrollPane(textComponent);
      }
      builder.addLabeledComponent(label, comp);
      componentByOptionMap.put(option, textComponent);
    }
    JPanel panel = builder.getPanel();
    panel.setBorder(new JBEmptyBorder(0, 10, 0, 10));
    return panel;
  }

  public void setState(@NotNull JSLintState state) {
    myJsonCheckbox.setSelected(state.isValidateJson());
    JSLintOptionsState optionsState = state.getOptionsState();
    for (JSLintOption option : JSLintOption.values()) {
      Object value = optionsState.getValue(option);
      JComponent component = myComponentByOptionMap.get(option);
      Preconditions.checkNotNull(component, "Component for %s is null!", option);
      setOptionState(component, value, option.getType());
    }
  }

  private static void setOptionState(@NotNull JComponent component,
                                     @Nullable Object value,
                                     @NotNull JSLintOption.Type optionType) {
    if (optionType == JSLintOption.Type.BOOLEAN) {
      JCheckBox checkBox = (JCheckBox) component;
      Boolean boolValue = (Boolean) value;
      boolean selected = boolValue != null && boolValue;
      checkBox.setSelected(selected);
    }
    else if (optionType == JSLintOption.Type.INTEGER) {
      JFormattedTextField formattedTextField = (JFormattedTextField) component;
      formattedTextField.setValue(value);
    }
    else if (optionType == JSLintOption.Type.STRING) {
      JTextArea textArea = (JTextArea) component;
      String strValue = (String) value;
      textArea.setText(StringUtil.notNullize(strValue));
    }
  }
  @NotNull
  public JSLintState.Builder getStateBuilder() {
    JSLintOptionsState optionsState = getOptionsState();
    JSLintState.Builder builder = new JSLintState.Builder();
    builder.setOptionsState(optionsState);
    builder.setValidateJson(myJsonCheckbox.isSelected());
    return builder;
  }

  @NotNull
  private JSLintOptionsState getOptionsState() {
    JSLintOptionsState.Builder builder = new JSLintOptionsState.Builder();
    for (JSLintOption option : JSLintOption.values()) {
      JComponent component = myComponentByOptionMap.get(option);
      Preconditions.checkNotNull(component, "Component for %s is null!", option);
      if (option.getType() == JSLintOption.Type.BOOLEAN) {
        JCheckBox checkBox = (JCheckBox) component;
        builder.put(option, checkBox.isSelected());
      }
      else if (option.getType() == JSLintOption.Type.INTEGER) {
        JFormattedTextField formattedTextField = (JFormattedTextField) component;
        Object value = formattedTextField.getValue();
        if (value != null && option.getType().isProperValue(value)) {
          builder.put(option, value);
        }
      }
      else if (option.getType() == JSLintOption.Type.STRING) {
        JTextArea textField = (JTextArea) component;
        String text = textField.getText();
        if (!StringUtil.isEmptyOrSpaces(text)) {
          builder.put(option, text);
        }
      }
    }
    return builder.build();
  }

  @NotNull
  public JComponent getComponent() {
    return myOptionsComponent;
  }

  public void handleEnableStatusChanged(boolean enabled) {
    if (enabled) {
      for (Runnable job : myUpdatingJobs) {
        job.run();
      }
    }
  }

}
