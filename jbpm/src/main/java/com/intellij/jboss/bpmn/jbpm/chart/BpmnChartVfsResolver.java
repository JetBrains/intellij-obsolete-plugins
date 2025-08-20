package com.intellij.jboss.bpmn.jbpm.chart;

import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BpmnChartVfsResolver implements DiagramVfsResolver<TFlowElement> {

  @Override
  @Nullable
  public String getQualifiedName(@Nullable TFlowElement element) {
    return element != null ? element.getId().getStringValue() : null;
  }

  @Override
  public TFlowElement resolveElementByFQN(@NotNull String fqn, @NotNull Project project) {
    return null;
  }
}
