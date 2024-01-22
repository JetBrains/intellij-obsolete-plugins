package com.intellij.jboss.bpmn.jbpm.dnd;

import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.util.Function;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface ChartDnDNodeDefinition<T, Node extends ChartNode<T>, Model extends ChartDataModel> {
  @NotNull
  @Nls
  String getName();

  @NotNull
  Icon getIcon();

  @Nullable
  Function<Model, Node> getCreateFunction();

  boolean isLeafNode();
}
