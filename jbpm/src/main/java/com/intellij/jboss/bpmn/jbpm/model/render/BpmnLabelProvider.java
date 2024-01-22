package com.intellij.jboss.bpmn.jbpm.model.render;

import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartNode;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.providers.TextProvider;
import org.jetbrains.annotations.Nullable;

public class BpmnLabelProvider implements TextProvider<TFlowElement, BpmnChartNode> {
  @Override
  @Nullable
  public String getText(BpmnChartNode node) {
    return node.getTooltip();
  }
}
