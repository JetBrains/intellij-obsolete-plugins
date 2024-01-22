package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jboss.bpmn.jbpm.model.render.GatewayColorProvider;
import com.intellij.jboss.bpmn.jbpm.model.render.SquareImage48x48;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelColor;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

@Presentation(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons.Bpmn.Gateway")
@RenderLabelColor(color = GatewayColorProvider.class)
public interface TGateway extends Bpmn20DomElement, TFlowNode, SquareImage48x48 {

  @NotNull
  GenericAttributeValue<TGatewayDirection> getGatewayDirection();
}
