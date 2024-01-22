package com.intellij.jboss.bpmn.jbpm.dnd.node;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class ChartDnDNode extends SimpleNode {
  private final @NotNull @Nls String name;
  private final @NotNull Icon icon;

  public ChartDnDNode(Project project, @Nls @NotNull String name, @NotNull Icon icon) {
    super(project);
    this.name = name;
    this.icon = icon;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(icon);
    presentation.addText(name, canStartDragging() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  public abstract boolean canStartDragging();
}
