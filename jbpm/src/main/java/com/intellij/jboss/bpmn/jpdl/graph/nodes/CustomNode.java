package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Custom;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CustomNode extends JpdlBasicNode<Custom> {

  public CustomNode(Custom identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.CUSTOM;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Custom;
  }
}
