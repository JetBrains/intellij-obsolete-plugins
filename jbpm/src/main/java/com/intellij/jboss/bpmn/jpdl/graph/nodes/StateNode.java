package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.State;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StateNode extends JpdlBasicNode<State> {

  public StateNode(State identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.STATE;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Page;
  }
}
