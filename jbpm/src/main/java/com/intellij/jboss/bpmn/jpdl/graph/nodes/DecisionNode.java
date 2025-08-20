package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Decision;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DecisionNode extends JpdlBasicNode<Decision> {

  public DecisionNode(Decision identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.DECISIION;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Decision;
  }
}
