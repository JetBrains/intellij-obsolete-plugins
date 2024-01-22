package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderImage;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tComplexGateway interface.
 */
@RenderImage(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Gateways.Parallel_48")
@DefaultNamePrefix("Complex Gateway")
public interface TComplexGateway extends Bpmn20DomElement, TGateway {
  @NotNull
  GenericAttributeValue<String> getDefault();

  @NotNull
  GenericDomValue<String> getActivationCondition();
}
