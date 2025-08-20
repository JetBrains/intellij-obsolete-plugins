package com.intellij.jboss.bpmn.jbpm.diagram.managers;

import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnDefinitionsWrapper;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnModuleWrapper;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class BpmnDiagramVfsResolver implements DiagramVfsResolver<BpmnElementWrapper<?>> {

  @Override
  @Nullable
  public String getQualifiedName(@Nullable BpmnElementWrapper element) {
    return element != null ? element.getFqn() : null;
  }

  @Override
  public BpmnElementWrapper resolveElementByFQN(@NotNull String fqn, @NotNull Project project) {
    final BpmnElementWrapper wrapper = BpmnDefinitionsWrapper.resolveElementByFQN(fqn, project);
    if (wrapper != null) {
      return wrapper;
    }
    return BpmnModuleWrapper.resolveElementByFQN(fqn, project);
  }
}
