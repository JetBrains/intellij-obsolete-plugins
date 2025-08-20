package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Join;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JoinNode extends JpdlBasicNode<Join> {

  public JoinNode(Join identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.JOIN;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Join;
  }
}
