package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tSignalEventDefinition interface.
 */
@DefinitionKind("Signal")
@DefaultNamePrefix("SignalEventDefinition")
public interface TSignalEventDefinition extends Bpmn20DomElement, TEventDefinition {

  @NotNull
  GenericAttributeValue<String> getSignalRef();
}
