package com.intellij.jboss.bpmn.jbpm.model.impl.xml;

import com.intellij.jboss.bpmn.jbpm.BpmnUtils;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowElement;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TFlowNode;
import com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20.TProcess;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class TProcessImpl implements TProcess {
  @NotNull
  @Override
  public List<TFlowElement> getFlowElements() {
    return BpmnUtils.getFlowElements(this);
  }

  @NotNull
  @Override
  public List<TFlowNode> getFlowNodes() {
    return BpmnUtils.getFlowNodes(this);
  }
}
