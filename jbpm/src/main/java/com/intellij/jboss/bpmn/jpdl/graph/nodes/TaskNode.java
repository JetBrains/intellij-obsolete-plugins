package com.intellij.jboss.bpmn.jpdl.graph.nodes;

import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jpdl.graph.JpdlNodeType;
import com.intellij.jboss.bpmn.jpdl.model.xml.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TaskNode extends JpdlBasicNode<Task> {

  public TaskNode(Task identifyingElement) {
    super(identifyingElement);
  }

  @Override
  @NotNull
  public JpdlNodeType getNodeType() {
    return JpdlNodeType.TASK;
  }

  @Override
  public Icon getIcon() {
    return JbossJbpmIcons.Jpdl.Task;
  }
}
