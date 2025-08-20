package com.intellij.jboss.bpmn.jbpm.providers;

import com.intellij.diagram.DiagramNode;
import org.jetbrains.annotations.Nullable;

public interface TextProvider<T, Node extends DiagramNode<T>> {
  @Nullable
  String getText(Node node);
}
