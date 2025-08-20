package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderImage;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tParallelGateway interface.
 */
@RenderImage(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Gateways.Parallel_48")
@DefaultNamePrefix("Parallel Gateway")
public interface TParallelGateway extends Bpmn20DomElement, TGateway {

}
