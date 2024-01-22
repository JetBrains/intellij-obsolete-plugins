package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Start;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StartNode extends JpdlBasicNode<Start> {

  public StartNode(Start identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.START;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Start;
  }
}

