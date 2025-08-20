package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.diagram.DiagramNodeBase;
import com.intellij.diagram.DiagramProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class ChartNode<T> extends DiagramNodeBase<T> {
  public ChartNode(DiagramProvider<T> provider) {
    super(provider);
  }

  public abstract String getId();

  @NotNull
  public abstract Collection<Class<?>> getClassesWithAnnotationsForRendering();

  public abstract void removeSelf();
}
