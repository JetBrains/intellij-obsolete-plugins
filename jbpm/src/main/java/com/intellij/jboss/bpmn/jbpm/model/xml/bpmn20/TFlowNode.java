package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.JBColorProvider;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelColor;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tFlowNode interface.
 */
@RenderLabelColor(color = JBColorProvider.class)
public interface TFlowNode extends Bpmn20DomElement, TFlowElement {

  @NotNull
  List<GenericDomValue<String>> getIncomings();

  @NotNull
  List<GenericDomValue<String>> getOutgoings();
}
