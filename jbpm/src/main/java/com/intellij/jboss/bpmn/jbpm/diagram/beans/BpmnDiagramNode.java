package com.intellij.jboss.bpmn.jbpm.diagram.beans;

import com.intellij.diagram.DiagramNodeBase;
import com.intellij.diagram.DiagramProvider;
import com.intellij.jboss.bpmn.jbpm.diagram.beans.wrappers.BpmnElementWrapper;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BpmnDiagramNode extends DiagramNodeBase<BpmnElementWrapper<?>> {

  private final BpmnElementWrapper myElement;

  public BpmnDiagramNode(final BpmnElementWrapper element,
                         final @NotNull DiagramProvider<BpmnElementWrapper<?>> provider) {
    super(provider);
    myElement = element;
  }

  @Override
  public String getTooltip() {
    return StringUtil.notNullize(myElement.getName(), JpdlBundle.message("unknown.bpmn.element.tooltip"));
  }

  @Override
  public @Nullable Icon getIcon() {
    return myElement.getIcon();
  }

  @NotNull
  @Override
  public BpmnElementWrapper getIdentifyingElement() {
    return myElement;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BpmnDiagramNode that = (BpmnDiagramNode)o;
    if (myElement != null ? !myElement.equals(that.myElement) : that.myElement != null) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myElement != null ? myElement.hashCode() : 0);
    return result;
  }
}