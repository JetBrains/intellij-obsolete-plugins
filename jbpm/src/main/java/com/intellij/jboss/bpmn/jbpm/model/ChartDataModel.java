package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.diagram.*;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.jboss.bpmn.jbpm.ui.ChartBuilder;
import com.intellij.openapi.graph.builder.NodeGroupDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class ChartDataModel<T> extends DiagramDataModel<T> {
  public ChartDataModel(Project project, ChartProvider<T> provider) {
    super(project, provider);
  }

  @NotNull
  @Override
  public abstract Collection<? extends ChartNode<T>> getNodes();

  @NotNull
  @Override
  public abstract Collection<? extends ChartEdge<T>> getEdges();

  @NotNull
  @Override
  public ChartNode<T> getSourceNode(@NotNull DiagramEdge<T> edge) {
    assert edge instanceof ChartEdge;
    return ((ChartEdge<T>)edge).getSource();
  }

  @NotNull
  @Override
  public ChartNode<T> getTargetNode(@NotNull DiagramEdge<T> edge) {
    assert edge instanceof ChartEdge;
    return ((ChartEdge<T>)edge).getTarget();
  }

  @Nullable
  @Override
  public abstract ChartEdge<T> createEdge(@NotNull DiagramNode<T> from, @NotNull DiagramNode<T> to);

  @Override
  public void removeEdge(@NotNull DiagramEdge<T> edge) {
    assert edge instanceof ChartEdge;
    super.removeEdge(edge);
  }

  @Override
  public DiagramScopeManager<T> getScopeManager() {
    return super.getScopeManager();
  }

  @Override
  public @NotNull ChartProvider<T> getProvider() {
    DiagramProvider<T> provider = super.getProvider();
    assert provider instanceof ChartProvider;
    return (ChartProvider<T>)provider;
  }

  @Nullable
  @Override
  public abstract ChartNode<T> addElement(@Nullable T element);

  @Override
  public @NotNull ChartBuilder<T> getBuilder() {
    DiagramBuilder builder = super.getBuilder();
    assert builder instanceof ChartBuilder;
    //noinspection unchecked
    return (ChartBuilder<T>)builder;
  }

  @Override
  public @Nullable NodeGroupDescriptor getGroup(@NotNull DiagramNode<T> node) {
    assert node instanceof ChartNode;
    return super.getGroup(node);
  }
}
