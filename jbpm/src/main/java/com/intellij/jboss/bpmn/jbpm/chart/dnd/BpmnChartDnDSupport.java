package com.intellij.jboss.bpmn.jbpm.chart.dnd;

import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartDataModel;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartNode;
import com.intellij.jboss.bpmn.jbpm.dnd.ChartDnDSupport;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.ui.ChartBuilder;
import org.jetbrains.annotations.NotNull;

public class BpmnChartDnDSupport extends ChartDnDSupport<TFlowElement, BpmnChartDataModel, BpmnChartNode, BpmnDnDNodeType> {
  public BpmnChartDnDSupport(@NotNull BpmnChartDataModel model,
                             @NotNull ChartBuilder<TFlowElement> builder) {
    super(model, builder, BpmnDnDNodeType.values(), true);
  }

  @Override
  public boolean canStartDragging(BpmnDnDNodeType type) {
    return true;
  }
}
