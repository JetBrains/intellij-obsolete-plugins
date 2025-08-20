package com.intellij.jboss.bpmn.jbpm.chart;

import com.intellij.diagram.*;
import com.intellij.jboss.bpmn.jbpm.chart.dnd.BpmnChartDnDSupport;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartDataModel;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartSource;
import com.intellij.jboss.bpmn.jbpm.diagram.managers.BpmnDiagramColorManager;
import com.intellij.jboss.bpmn.jbpm.diagram.managers.BpmnNodeContentManager;
import com.intellij.jboss.bpmn.jbpm.dnd.ChartDnDSupport;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.ChartSource;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.openapi.graph.builder.EdgeCreationPolicy;
import com.intellij.openapi.graph.view.EditMode;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.uml.presentation.DiagramPresentationModelImpl;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class BpmnChartProvider extends ChartProvider<TFlowElement> {
  public static final @NonNls String ID = "BPMN_2_0";
  public static final @NlsSafe String BPMN_2_0 = "Bpmn 2.0";

  private final DiagramNodeContentManager nodeContentManager = new BpmnNodeContentManager();
  private final DiagramElementManager<TFlowElement> chartElementManager = new BpmnChartElementManager();
  private final DiagramVfsResolver<TFlowElement> chartVfsResolver = new BpmnChartVfsResolver();
  private final DiagramColorManager diagramColorManager = new BpmnDiagramColorManager();

  @Override
  public String getChartId() {
    return ID;
  }

  @Override
  public String getChartName() {
    return BPMN_2_0;
  }

  @Override
  public BpmnChartDataModel createChartDataModel(@NotNull Project project,
                                                 @NotNull ChartSource source,
                                                 DiagramPresentationModel presentationModel) {
    assert source instanceof BpmnChartSource;
    return new BpmnChartDataModel(project, this, (BpmnChartSource)source, presentationModel);
  }

  @Override
  public DiagramPresentationModel createPresentationModel(@NotNull Project project, @NotNull Graph2D graph) {
    return new DiagramPresentationModelImpl(graph, project, this) {
      @Override
      public void customizeSettings(@NotNull Graph2DView view, @NotNull EditMode editMode) {
        super.customizeSettings(view, editMode);
        editMode.allowEdgeCreation(true);
        editMode.allowBendCreation(true);
        editMode.allowResizeNodes(true);
      }

      @Override
      public @NotNull EdgeCreationPolicy<DiagramNode<?>> getEdgeCreationPolicy() {
        //noinspection unchecked
        return (EdgeCreationPolicy<DiagramNode<?>>)EdgeCreationPolicy.EVERYTHING_ACCEPTED_POLICY;
      }
    };
  }

  @Override
  public @NotNull ChartDnDSupport creteDnDSupport(ChartDataModel<TFlowElement> dataModel) {
    assert dataModel instanceof BpmnChartDataModel;
    return new BpmnChartDnDSupport((BpmnChartDataModel)dataModel, dataModel.getBuilder());
  }

  @Override
  public ChartLayoutCoordinator createLayoutCoordinator(Project project, ChartDataModel model) {
    assert model instanceof BpmnChartDataModel;
    return new BpmnChartLayoutCoordinator(project, (BpmnChartDataModel)model);
  }

  @Pattern("[a-zA-Z0-9_-]*")
  @Override
  public @NotNull String getID() {
    return ID;
  }

  @Override
  public @NotNull DiagramNodeContentManager getNodeContentManager() {
    return nodeContentManager;
  }

  @Override
  public @NotNull DiagramNodeContentManager createNodeContentManager() {
    return new BpmnNodeContentManager();
  }

  @Override
  public @NotNull DiagramElementManager<TFlowElement> getElementManager() {
    return chartElementManager;
  }

  @Override
  public @NotNull DiagramVfsResolver<TFlowElement> getVfsResolver() {
    return chartVfsResolver;
  }

  @Override
  public @NotNull DiagramColorManager getColorManager() {
    return diagramColorManager;
  }
}
