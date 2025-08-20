package com.intellij.jboss.bpmn.jbpm.settings;

import com.intellij.diagram.*;
import com.intellij.jboss.bpmn.jbpm.dnd.ChartDnDSupport;
import com.intellij.jboss.bpmn.jbpm.layout.ChartLayoutCoordinator;
import com.intellij.jboss.bpmn.jbpm.model.ChartDataModel;
import com.intellij.jboss.bpmn.jbpm.model.ChartSource;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uml.presentation.DiagramPresentationModelImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ChartProvider<T> extends DiagramProvider<T> {

  public abstract String getChartId();

  @NlsContexts.Label
  public abstract String getChartName();

  @Override
  public @NotNull DiagramVisibilityManager createVisibilityManager() {
    return EmptyDiagramVisibilityManager.INSTANCE;
  }

  @Override
  public DiagramPresentationModel createPresentationModel(@NotNull Project project, @NotNull Graph2D graph) {
    return new DiagramPresentationModelImpl(graph, project, this);
  }

  @Override
  public @NotNull DiagramRelationshipManager<T> getRelationshipManager() {
    //noinspection unchecked
    return (DiagramRelationshipManager<T>)DiagramRelationshipManager.NO_RELATIONSHIP_MANAGER;
  }

  @Override
  public @NotNull String getPresentableName() {
    return getChartName();
  }

  @Override
  public final @NotNull ChartDataModel<T> createDataModel(@NotNull Project project,
                                                          @Nullable T element,
                                                          @Nullable VirtualFile file,
                                                          @NotNull DiagramPresentationModel presentationModel) {
    assert false : "should not be called";
    return null;
  }

  public abstract ChartDataModel<T> createChartDataModel(@NotNull Project project,
                                                         @NotNull ChartSource source,
                                                         DiagramPresentationModel presentationModel);

  @Nullable
  public abstract ChartDnDSupport creteDnDSupport(ChartDataModel<T> dataModel);

  public abstract ChartLayoutCoordinator createLayoutCoordinator(Project project, ChartDataModel model);

  @NotNull
  @Override
  public ChartExtras<T> getExtras() {
    return new ChartExtras<>();
  }
}
