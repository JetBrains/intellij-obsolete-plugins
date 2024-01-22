package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderIcon;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tSendTask interface.
 */
@RenderIcon(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Tasks.SendTask")
public interface TSendTask extends Bpmn20DomElement, TTask {

  @NotNull
  GenericAttributeValue<String> getImplementation();

  @NotNull
  GenericAttributeValue<String> getMessageRef();

  @NotNull
  GenericAttributeValue<String> getOperationRef();
}
