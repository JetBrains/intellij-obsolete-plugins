package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tTerminateEventDefinition interface.
 */
@DefinitionKind("Terminate")
@DefaultNamePrefix("TerminateEventDefinition")
public interface TTerminateEventDefinition extends Bpmn20DomElement, TEventDefinition {
}
