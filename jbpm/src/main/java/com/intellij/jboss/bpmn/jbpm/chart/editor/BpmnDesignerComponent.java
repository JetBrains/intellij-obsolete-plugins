package com.intellij.jboss.bpmn.jbpm.chart.editor;

import com.intellij.jboss.bpmn.jbpm.model.ChartSource;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.settings.ChartProvider;
import com.intellij.jboss.bpmn.jbpm.ui.ChartDesignerComponent;
import com.intellij.openapi.project.Project;

public class BpmnDesignerComponent extends ChartDesignerComponent<TFlowElement> {

  public BpmnDesignerComponent(Project project,
                               ChartSource source,
                               ChartProvider<TFlowElement> chartProvider) {
    super(project, source, chartProvider);
  }
}
