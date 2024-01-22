package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.diagram.DiagramNode;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface ColorProvider<T, Node extends DiagramNode<T>> {
  @NotNull
  Color getColor(Node node);
}
