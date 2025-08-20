package com.intellij.jboss.bpmn.jbpm.chart.model;

import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.DiagramRelationshipInfoAdapter;
import com.intellij.diagram.presentation.DiagramLineType;

import java.awt.*;

public enum BpmnChartEdgeType {
  Flow(DiagramLineType.SOLID, DiagramRelationshipInfo.STANDARD, null),
  SubFlow(DiagramLineType.DOTTED, DiagramRelationshipInfo.STANDARD, null),
  Event(DiagramLineType.DASHED, DiagramRelationshipInfo.NONE, DiagramRelationshipInfo.DIAMOND);

  private final DiagramRelationshipInfo relationshipInfo;

  BpmnChartEdgeType(DiagramLineType lineType, Shape startArrow, Shape endArrow) {
    relationshipInfo = new DiagramRelationshipInfoAdapter.Builder()
      .setName("BPMN_CHART_EDGE")
      .setLineType(lineType)
      .setSourceArrow(startArrow)
      .setTargetArrow(endArrow)
      .create();
  }

  DiagramRelationshipInfo getDiagramRelationshipInfo() {
    return relationshipInfo;
  }
}
