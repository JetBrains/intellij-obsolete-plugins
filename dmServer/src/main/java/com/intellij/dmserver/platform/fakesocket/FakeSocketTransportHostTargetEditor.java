package com.intellij.dmserver.platform.fakesocket;


import com.intellij.javaee.transport.TransportHost;
import com.intellij.javaee.transport.TransportHostTarget;
import com.intellij.javaee.transport.TransportHostTargetEditor;

import javax.swing.*;

public class FakeSocketTransportHostTargetEditor implements TransportHostTargetEditor {
  private JPanel myMainPanel;
  private JTextField myPortInTextField;
  private JTextField myPortOutTextField;

  @Override
  public JPanel getMainPanel() {
    return myMainPanel;
  }

  @Override
  public void setHost(TransportHost host) {

  }

  @Override
  public void resetStateFrom(TransportHostTarget state) {
    FakeSocketTransportHostTarget asFakeSocketState = (FakeSocketTransportHostTarget)state;
    myPortInTextField.setText(asFakeSocketState.getPortIn());
    myPortOutTextField.setText(asFakeSocketState.getPortOut());
  }

  @Override
  public void applyStateTo(TransportHostTarget state) {
    FakeSocketTransportHostTarget asFakeSocketState = (FakeSocketTransportHostTarget)state;
    asFakeSocketState.setPortIn(myPortInTextField.getText());
    asFakeSocketState.setPortOut(myPortOutTextField.getText());
  }
}
