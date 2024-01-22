package com.intellij.jboss.bpmn.jbpm.ui;

import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.DiagramProvider;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.ChartSource;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uml.UmlGraphBuilder;
import com.intellij.uml.UmlGraphBuilderFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ChartBuilderFactory extends UmlGraphBuilderFactory {

  @Override
  protected <T> @NotNull DiagramDataModel<T> createDataModel(
    final @NotNull Project project,
    final @NotNull DiagramProvider<T> provider,
    final @Nullable T element,
    final @Nullable VirtualFile file,
    final @NotNull DiagramPresentationModel presentationModel
  ) {
    assert element != null;
    return ((ChartProvider<T>)provider).createChartDataModel(project, (ChartSource)element, presentationModel);
  }

  @Override
  protected <T> @NotNull UmlGraphBuilder createBuilder(
    final @NotNull Project project,
    final @NotNull DiagramProvider<T> provider,
    final @NotNull Graph2D graph,
    final @NotNull Graph2DView view,
    final @NotNull DiagramPresentationModel presentationModel,
    final @NotNull DiagramDataModel<T> dataModel
  ) {
    return new ChartBuilder<>(project, ((ChartProvider<T>)provider), graph, view, ((ChartDataModel<T>)dataModel), presentationModel) {
    };
  }

  public static <T> ChartBuilder<T> create(
    final @NotNull Project project,
    final @NotNull ChartProvider<T> provider,
    final @NotNull ChartSource source
  ) {
    //noinspection unchecked
    return (ChartBuilder<T>)new ChartBuilderFactory().createBuilder(project, provider, ((T)source), source.getFile());
  }
}
