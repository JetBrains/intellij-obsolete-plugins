package com.intellij.jboss.bpmn.jbpm.dnd.node;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class ChartDnDLeafNode<NodeT> extends ChartDnDNode {
  private final NodeT myValue;

  public ChartDnDLeafNode(Project project, NodeT value, @Nls @NotNull String name, @NotNull Icon icon) {
    super(project, name, icon);
    myValue = value;
  }

  public NodeT getValue() {
    return myValue;
  }

  @Override
  public SimpleNode @NotNull [] getChildren() {
    return NO_CHILDREN;
  }

  @Override
  public boolean isAlwaysLeaf() {
    return true;
  }
}
