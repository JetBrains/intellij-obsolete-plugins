package com.intellij.jboss.bpmn.jpdl.actions;

import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;

public class CreateJpdlDialog extends DialogWrapper {
  private final InputValidator myValidator;
  private JPanel myContentPane;
  private JTextField myFileName;
  private JTextField myProcessName;
  private boolean myProcessNameWasEdited;

  public CreateJpdlDialog(@Nullable Project project, InputValidator validator) {
    super(project, true);
    setTitle(JpdlBundle.message("create.jpdl.process.dialog.title"));
    myValidator = validator;

    myFileName.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        recalculateTextAndValidity();
      }
    });
    myFileName.addInputMethodListener(new InputMethodListener() {
      @Override
      public void inputMethodTextChanged(InputMethodEvent event) {
        recalculateTextAndValidity();
      }

      @Override
      public void caretPositionChanged(InputMethodEvent event) {
        recalculateTextAndValidity();
      }
    });
    final FocusAdapter adapter = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        myProcessName.removeFocusListener(this);
        myProcessNameWasEdited = true;
      }
    };
    myProcessName.addFocusListener(adapter);
    init();
  }

  private void recalculateTextAndValidity() {
    final String text = myFileName.getText().trim();
    if (!myProcessNameWasEdited) {
      myProcessName.setText(text);
    }
    setOKActionEnabled(myValidator == null || myValidator.checkInput(text));
    if (myValidator instanceof InputValidatorEx) {
      setErrorText(((InputValidatorEx)myValidator).getErrorText(text), myFileName);
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myFileName;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  @Override
  protected void doOKAction() {
    String inputString = myFileName.getText().trim();
    if (myValidator == null ||
        myValidator.checkInput(inputString) &&
        myValidator.canClose(inputString)) {
      close(0);
    }
  }

  public String getFileName() {
    return myFileName.getText().trim();
  }

  public String getProcessName() {
    return myProcessName.getText().trim();
  }
}
