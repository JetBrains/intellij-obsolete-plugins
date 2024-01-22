package com.intellij.jboss.bpmn.jbpm.chart.model;

import com.intellij.jboss.bpmn.jbpm.model.ChartDomFileSource;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class BpmnChartSource extends ChartDomFileSource {
  public BpmnChartSource(Project project, @NotNull VirtualFile file) {
    super(project, file);
  }

  @Override
  public void dispose() {
  }
}
