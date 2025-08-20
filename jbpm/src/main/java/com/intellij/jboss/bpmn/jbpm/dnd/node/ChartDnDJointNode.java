package com.intellij.jboss.bpmn.jbpm.dnd.node;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class ChartDnDJointNode extends ChartDnDNode {
  private final @NotNull List<? extends ChartDnDNode> children;

  public ChartDnDJointNode(Project project, @NotNull @Nls String name, @NotNull Icon icon,
                           @NotNull List<? extends ChartDnDNode> children) {
    super(project, name, icon);
    this.children = children;
  }

  @Override
  public SimpleNode @NotNull [] getChildren() {
    return children.toArray(new ChartDnDNode[0]);
  }

  @Override
  public boolean canStartDragging() {
    return ContainerUtil.exists(children, child -> child.canStartDragging());
  }
}
