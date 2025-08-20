package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Fork;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ForkNode extends JpdlBasicNode<Fork> {

  public ForkNode(Fork identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.FORK;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Fork;
  }
}
