package com.intellij.jboss.bpmn.jbpm.diagram.managers;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramColorManagerBase;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.jboss.bpmn.jbpm.diagram.BpmnDiagramPresentationConstants;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.BpmnDiagramEdge;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.BpmnEdgeType;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.Bpmn20DomElementWrapper;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.awt.*;


public class BpmnDiagramColorManager extends DiagramColorManagerBase {

  public static final Key<Color> CUSTOM_NODE_HEADER_COLOR = Key.create("CUSTOM_NODE_HEADER_COLOR");

  @NotNull
  @Override
  public Color getEdgeColor(@NotNull DiagramBuilder builder, @NotNull DiagramEdge edge) {
    if (edge instanceof BpmnDiagramEdge diagramEdge) {
      final BpmnEdgeType edgeType = diagramEdge.getEdgeType();
      if (edgeType == BpmnEdgeType.SUBFLOW) {
        return BpmnDiagramPresentationConstants.EDGE_SUBFLOW_COLOR;
      }
    }
    return super.getEdgeColor(builder, edge);
  }

  @NotNull
  @Override
  public Color getNodeHeaderBackground(@NotNull DiagramBuilder builder, @NotNull DiagramNode node, Object element) {
    if (element instanceof Bpmn20DomElementWrapper) {
      Color customColor = node.getUserData(CUSTOM_NODE_HEADER_COLOR);
      if (customColor != null) {
        return customColor;
      }
    }
    return super.getNodeHeaderBackground(builder, node, element);
  }
}