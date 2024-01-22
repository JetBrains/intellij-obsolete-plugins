package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderIcon;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tBusinessRuleTask interface.
 */
@RenderIcon(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Tasks.BusinessRuleTask")
public interface TBusinessRuleTask extends Bpmn20DomElement, TTask {

  @NotNull
  GenericAttributeValue<String> getImplementation();
}
