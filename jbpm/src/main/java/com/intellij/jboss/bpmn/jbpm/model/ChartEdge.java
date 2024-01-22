package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramRelationshipInfo;
import org.jetbrains.annotations.NotNull;

public abstract class ChartEdge<T> extends DiagramEdgeBase<T> {
  public ChartEdge(@NotNull ChartNode<T> source,
                   @NotNull ChartNode<T> target,
                   @NotNull DiagramRelationshipInfo relationship) {
    super(source, target, relationship);
  }

  @NotNull
  @Override
  public ChartNode<T> getSource() {
    return (ChartNode<T>)super.getSource();
  }

  @NotNull
  @Override
  public ChartNode<T> getTarget() {
    return (ChartNode<T>)super.getTarget();
  }

  public abstract void removeSelf();
}
