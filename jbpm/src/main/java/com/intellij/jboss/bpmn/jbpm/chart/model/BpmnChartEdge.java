package com.intellij.jboss.bpmn.jbpm.chart.model;

import com.intellij.jboss.bpmn.jbpm.model.ChartEdge;
import com.intellij.jboss.bpmn.jbpm.model.ChartNode;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TSequenceFlow;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmndi.BPMNEdge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BpmnChartEdge extends ChartEdge<TFlowElement> {
  @NotNull final private TSequenceFlow sequenceFlow;
  @Nullable private BPMNEdge layout;

  public BpmnChartEdge(@NotNull ChartNode<TFlowElement> source,
                       @NotNull ChartNode<TFlowElement> target,
                       @NotNull TSequenceFlow flow,
                       BpmnChartEdgeType edgeType) {
    super(source, target, edgeType.getDiagramRelationshipInfo());
    sequenceFlow = flow;
  }

  @NotNull
  public TSequenceFlow getSequenceFlow() {
    return sequenceFlow;
  }

  @Override
  public void removeSelf() {
    removeLayout();
    sequenceFlow.undefine();
  }

  @Nullable
  public BPMNEdge getLayout() {
    return layout;
  }

  public void setLayout(@Nullable BPMNEdge edge) {
    this.layout = edge;
  }

  public void removeLayout() {
    if (layout != null) {
      layout.undefine();
      layout = null;
    }
  }
}
