package com.intellij.seam.pageflow.graph.dnd;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Collections;
import java.util.List;

public class PageflowComponentNameDialog extends DialogWrapper {
  private JPanel myContentPane;
  private JTextField myName;
  private final List<String> myExcludedNames;

  public PageflowComponentNameDialog() {
    this(Collections.emptyList());
  }

  public PageflowComponentNameDialog(@NotNull List<String> excludedNames) {
    super(false);
    myExcludedNames = excludedNames;
    setModal(true);

    setTitle(SeamBundle.message("seam.pageflow.new.pageflow.component.dialog.name"));

    myName.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull final DocumentEvent e) {
        checkInput();
      }
    });

    getOKAction().setEnabled(false);
    init();
  }

  private void checkInput() {
    final String text = myName.getText().trim();
    getOKAction().setEnabled(text.length() > 0 && !myExcludedNames.contains(text));
  }

  public String getPageflowComponentName() {
    return myName.getText();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }
}
