package com.intellij.restClient;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
@SuppressWarnings("HardCodedStringLiteral") // shouldn't be visible at UI
public class UsernamePasswordForm extends DialogWrapper {
  private JTextField myUsernameTextField;
  private JPasswordField myPasswordTextField;
  private JPanel myMainPanel;

  public UsernamePasswordForm(@NotNull Component parent) {
    super(parent, false);
    setTitle("Generate Authorization Header");
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myUsernameTextField;
  }

  public String getUsername() {
    return myUsernameTextField.getText();
  }

  public String getPassword() {
    return String.valueOf(myPasswordTextField.getPassword());

  }
}
