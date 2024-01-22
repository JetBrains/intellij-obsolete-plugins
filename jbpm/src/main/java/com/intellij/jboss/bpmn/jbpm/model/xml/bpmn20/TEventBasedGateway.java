package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderImage;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tEventBasedGateway interface.
 */
@RenderImage(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Gateways.EventBased_48")
@DefaultNamePrefix("Event Based Gateway")
public interface TEventBasedGateway extends Bpmn20DomElement, TGateway {
  @NotNull
  GenericAttributeValue<TEventBasedGatewayType> getEventGatewayType();

  @NotNull
  GenericAttributeValue<Boolean> getInstantiate();
}
