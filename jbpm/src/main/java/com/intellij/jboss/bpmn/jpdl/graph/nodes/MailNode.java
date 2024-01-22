package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Mail;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MailNode extends JpdlBasicNode<Mail> {

  public MailNode(Mail identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.MAIL;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Mail;
  }
}
