package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Group;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class GroupNode extends JpdlBasicNode<Group> {

  public GroupNode(Group identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.GROUP;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Process;
  }
}
