package com.intellij.jboss.bpmn.jbpm.ui;

import com.intellij.diagram.DiagramDataKeys;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.project.Project;
import com.intellij.uml.UmlGraphBuilder;
import org.jetbrains.annotations.NotNull;

public abstract class ChartBuilder<T> extends UmlGraphBuilder {
  private final ChartLayoutCoordinator chartLayoutCoordinator;
  @NotNull private final ChartProvider<T> chartProvider;

  protected ChartBuilder(Project project,
                         @NotNull ChartProvider<T> chartProvider,
                         Graph2D graph,
                         Graph2DView view,
                         ChartDataModel<T> dataModel,
                         DiagramPresentationModel presentationModel) {
    super(project, graph, view, dataModel, presentationModel);
    this.chartProvider = chartProvider;
    chartLayoutCoordinator = chartProvider.createLayoutCoordinator(project, dataModel);
    putUserData(DiagramDataKeys.UML_PROVIDER, chartProvider);
  }

  @Override
  @NotNull
  public ChartProvider<T> getProvider() {
    return chartProvider;
  }

  @Override
  public @NotNull ChartDataModel<T> getDataModel() {
    //noinspection unchecked
    return (ChartDataModel<T>)super.getDataModel();
  }

  public ChartLayoutCoordinator getChartLayoutCoordinator() {
    return chartLayoutCoordinator;
  }
}
