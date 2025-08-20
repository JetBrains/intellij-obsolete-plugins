package org.intellij.j2ee.web.resin.ui;

import org.intellij.j2ee.web.resin.ResinBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShowResinConfDialog extends JDialog {
  private JPanel contentPane;
  private JButton buttonOK;
  private JTextArea textArea;

  public ShowResinConfDialog(String text) {
    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(buttonOK);

    buttonOK.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onOK();
      }
    });

    setTitle(ResinBundle.message("message.text.resin.conf.altered"));

    textArea.setText(text);
    textArea.setEditable(false);
    contentPane.setPreferredSize(new Dimension(640, 480));
  }

  private void onOK() {
    // add your code here
    dispose();
  }
}
