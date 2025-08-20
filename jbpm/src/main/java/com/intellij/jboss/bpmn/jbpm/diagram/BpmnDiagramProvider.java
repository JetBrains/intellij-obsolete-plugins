package com.intellij.jboss.bpmn.jbpm.diagram;

import com.intellij.diagram.*;
import com.intellij.diagram.actions.DiagramCreateNewElementAction;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.jboss.bpmn.JbossJbpmIcons;
import com.intellij.jboss.bpmn.jbpm.BpmnBundle;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.jboss.bpmn.jbpm.diagram.managers.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class BpmnDiagramProvider extends DiagramProvider<BpmnElementWrapper<?>> {

  @NonNls public static final String ID = "BPMN_2_0";

  private final DiagramNodeContentManager nodeContentManager = new BpmnNodeContentManager();
  private final DiagramElementManager<BpmnElementWrapper<?>> diagramElementManager = new BpmnDiagramElementManager();
  private final DiagramVfsResolver<BpmnElementWrapper<?>> diagramVfsResolver = new BpmnDiagramVfsResolver();
  private final DiagramColorManager diagramColorManager = new BpmnDiagramColorManager();
  private final DiagramExtras<BpmnElementWrapper<?>> diagramExtras = new BpmnDiagramExtras();

  @Pattern("[a-zA-Z0-9_-]*")
  @Override
  public @NotNull String getID() {
    return ID;
  }

  @Override
  public @NotNull DiagramVisibilityManager createVisibilityManager() {
    return EmptyDiagramVisibilityManager.INSTANCE;
  }

  @Override
  public DiagramEdgeCreationPolicy<BpmnElementWrapper<?>> getEdgeCreationPolicy() {
    return new DiagramEdgeCreationPolicy<>() {
      @Override
      public boolean acceptSource(@NotNull DiagramNode<BpmnElementWrapper<?>> source) {
        final BpmnElementWrapper element = source.getIdentifyingElement();
        return true; //todo
      }

      @Override
      public boolean acceptTarget(@NotNull DiagramNode<BpmnElementWrapper<?>> target) {
        final BpmnElementWrapper element = target.getIdentifyingElement();
        return true;// todo
      }
    };
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
  public @NotNull DiagramElementManager<BpmnElementWrapper<?>> getElementManager() {
    return diagramElementManager;
  }

  @Override
  public @NotNull DiagramVfsResolver<BpmnElementWrapper<?>> getVfsResolver() {
    return diagramVfsResolver;
  }

  @SuppressWarnings("unchecked")
  @Override
  public @NotNull DiagramRelationshipManager getRelationshipManager() {
    return DiagramRelationshipManager.NO_RELATIONSHIP_MANAGER;
  }

  @Override
  public @NotNull String getPresentableName() {
    return BpmnBundle.message("bpmn.diagram.title");
  }

  @Override
  public @NotNull DiagramDataModel<BpmnElementWrapper<?>> createDataModel(@NotNull Project project,
                                                                          @Nullable BpmnElementWrapper element,
                                                                          @Nullable VirtualFile file,
                                                                          @NotNull DiagramPresentationModel presentationModel) {
    return new BpmnDiagramDataModel(project, this, element, presentationModel);
  }

  @Override
  public @NotNull Icon getActionIcon(boolean isPopup) {
    return JbossJbpmIcons.Bpmn.Process;
  }

  @Override
  public @NotNull String getActionName(boolean isPopup) {
    return BpmnBundle.message(!isPopup ? "bpmn.2.0.designer" : "bpmn.2.0.overview");
  }

  @Override
  public @NotNull DiagramColorManager getColorManager() {
    return diagramColorManager;
  }

  @NotNull
  @Override
  public DiagramExtras<BpmnElementWrapper<?>> getExtras() {
    return diagramExtras;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public DiagramCreateNewElementAction<BpmnElementWrapper, ?> @NotNull [] getCreateNewActions() {
    return DiagramCreateNewElementAction.EMPTY; /* todo: */
  }
}
